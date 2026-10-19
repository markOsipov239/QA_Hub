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

    @GetMapping("/logs")
    fun getLogs(@RequestParam(required = false, defaultValue = "200") linesCount: Int): ResponseEntity<List<LogEntity>> {
        val responseHeaders = HttpHeaders()
        responseHeaders.set("Skip-Logging", "true")

        val logFilePath = "/var/log/qa_hub/application_json.log"
        val logFile = File(logFilePath)
        if (!logFile.exists()) {
            throw Exception("Logs file is not found on path: $logFilePath")
        }

        val logs = logFile.useLines { lines ->
            lines
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()
                .takeLast(linesCount)
                .mapNotNull { parseLogLine(it) }
        }

        return ResponseEntity.ok().headers(responseHeaders).body(logs)
    }

    private fun parseLogLine(line: String): LogEntity? {
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
            // Skip a trailing incomplete line or corrupted JSON entry.
            null
        }
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
