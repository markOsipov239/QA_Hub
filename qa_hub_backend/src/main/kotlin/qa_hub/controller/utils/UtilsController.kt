package qa_hub.controller.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File

@RestController
@RequestMapping("/api/utils")
class UtilsController {
    data class LogEntity(
        val timestamp: String? = null,
        val level: String? = null,
        val thread: String? = null,
        val logger: String? = null,
        val message: String? = null
    )

    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, Any?>>() {}.type
    private val ansiRegex = Regex("\\u001B\\[[0-9;]*m")
    private val plainLogRegex = Regex("""^\[(.*?)]\s+(\w+)\s+-\s+(.*)$""")

    private val logDirPath = "/var/log/qa_hub"

    @GetMapping("/logs")
    fun getLogs(@RequestParam(required = false, defaultValue = "200") linesCount: Int): ResponseEntity<List<LogEntity>> {
        val responseHeaders = HttpHeaders()
        responseHeaders.set("Skip-Logging", "true")

        val logDir = File(logDirPath)
        if (!logDir.exists()) {
            throw Exception("Logs directory is not found on path: $logDirPath")
        }

        val jsonFiles = listJsonLogFiles(logDir)
        val plainFiles = listPlainLogFiles(logDir)

        responseHeaders.set(
            "X-Log-Files",
            (jsonFiles + plainFiles).joinToString(",") { "${it.name}:${it.length()}" }
        )

        val jsonLogs = readJsonLogs(jsonFiles, linesCount)
        val logs = jsonLogs.ifEmpty { readPlainLogs(plainFiles, linesCount) }

        if (logs.isEmpty()) {
            val diagnostic = LogEntity(
                timestamp = System.currentTimeMillis().toString(),
                level = "WARN",
                thread = "qa-hub",
                logger = "UtilsController",
                message = "No log lines found in $logDirPath. jsonFiles=${jsonFiles.map { "${it.name}:${it.length()}" }}, plainFiles=${plainFiles.map { "${it.name}:${it.length()}" }}"
            )
            return ResponseEntity.ok().headers(responseHeaders).body(listOf(diagnostic))
        }

        return ResponseEntity.ok().headers(responseHeaders).body(logs)
    }

    private fun listJsonLogFiles(logDir: File): List<File> {
        return logDir.listFiles()
            ?.filter { it.isFile && (it.name == "application_json.log" || it.name.startsWith("application_json_")) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    private fun listPlainLogFiles(logDir: File): List<File> {
        return logDir.listFiles()
            ?.filter {
                it.isFile && (
                    it.name == "application.log" ||
                        (it.name.startsWith("application_") && !it.name.startsWith("application_json") && it.name.endsWith(".log"))
                    )
            }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    private fun readJsonLogs(files: List<File>, linesCount: Int): List<LogEntity> {
        if (files.isEmpty()) {
            return emptyList()
        }

        return files
            .asSequence()
            .flatMap { file -> file.readLines().asSequence() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
            .takeLast(linesCount)
            .mapNotNull { parseJsonLogLine(it) }
    }

    private fun readPlainLogs(files: List<File>, linesCount: Int): List<LogEntity> {
        if (files.isEmpty()) {
            return emptyList()
        }

        return files
            .asSequence()
            .flatMap { file -> file.readLines().asSequence() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
            .takeLast(linesCount)
            .mapNotNull { parsePlainLogLine(it) }
    }

    private fun parseJsonLogLine(line: String): LogEntity? {
        return try {
            val raw = gson.fromJson<Map<String, Any?>>(line, mapType) ?: return null
            LogEntity(
                timestamp = raw["timestamp"]?.toString(),
                level = raw["level"]?.toString(),
                thread = raw["thread"]?.toString(),
                logger = raw["logger"]?.toString(),
                message = raw["message"]?.toString()
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePlainLogLine(line: String): LogEntity? {
        val stripped = line.replace(ansiRegex, "")
        val match = plainLogRegex.find(stripped) ?: return LogEntity(
            timestamp = null,
            level = "INFO",
            thread = null,
            logger = null,
            message = stripped
        )

        return LogEntity(
            timestamp = match.groupValues[1],
            level = match.groupValues[2],
            thread = null,
            logger = null,
            message = match.groupValues[3]
        )
    }

    data class BuildInfo(
        val branch: String,
        val commit: String,
        val dirty: Boolean,
        val buildTime: String
    )

    @GetMapping("/version")
    fun getVersion(): BuildInfo {
        val stream = javaClass.classLoader.getResourceAsStream("build-info.json")
            ?: return BuildInfo("unknown", "unknown", false, "unknown")

        return stream.bufferedReader().use { Gson().fromJson(it, BuildInfo::class.java) }
    }
}
