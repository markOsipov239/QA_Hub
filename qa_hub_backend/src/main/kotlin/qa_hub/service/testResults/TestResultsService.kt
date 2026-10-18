package qa_hub.service.testResults

import com.mongodb.client.result.UpdateResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.aggregate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import qa_hub.core.mongo.QaHubMongoClient
import qa_hub.core.mongo.entity.Collections
import qa_hub.core.mongo.utils.setCurrentPropertyValues
import qa_hub.core.utils.DateTimeUtils
import qa_hub.entity.testRun.*
import qa_hub.service.utils.ProjectIntegrationsService
import java.time.Instant

@Service
class TestResultsService {
    @Autowired
    lateinit var mongoClient: QaHubMongoClient

    @Autowired
    lateinit var projectIntegrationsService: ProjectIntegrationsService


    private val testResultsCollection by lazy {
        mongoClient.db.getCollection<TestResult>(Collections.TEST_RESULTS.collectionName)
    }

    private val testResultsRetriesCollection by lazy {
        mongoClient.db.getCollection<TestResultRetry>(Collections.TEST_RESULTS_RETRIES.collectionName)
    }

    private val testRunCollection by lazy {
        mongoClient.db.getCollection<TestRun>(Collections.TEST_RUNS.collectionName)
    }

    fun findSingleResult(testRunId: String, identifier: String): TestResult? = runBlocking {
        testResultsCollection.findOne(
            and(
                TestResult::testRunId eq testRunId,
                or(
                    TestResult::fullName eq identifier,
                    TestResult::testcaseId eq identifier
                )
            )
        )
    }

    fun findTestResults(testRunId: String, request: TestResultsRequest?): List<TestResult> = runBlocking {
        val filter = mutableListOf(TestResult::testRunId eq testRunId)

        request?.filter?.let {
            filter.addAll(request.filter.getMongoFilter())
        }

        val query = mutableListOf(
            match(and(*filter.toTypedArray())),
            sort(ascending(TestResult::fullName)),
            skip(request?.pagination?.skip ?: 0 ))

        if ((request?.pagination?.limit ?: 0)> 0) {
            query.add(limit(request!!.pagination!!.limit) )
        }

        testResultsCollection.aggregate<TestResult>(query).toList()
    }


    data class CountResponse(val count: Long)
    fun countTestResults(testRunId: String, request: TestResultFilter?): CountResponse = runBlocking {
        val filter = mutableListOf(TestResult::testRunId eq testRunId)

        request?.let {
            filter.addAll(request.getMongoFilter())
        }

        return@runBlocking CountResponse(testResultsCollection.countDocuments(filter = and(*filter.toTypedArray())))
    }

    fun findTestRetries(testRunId: String, fullName: String): List<TestResultRetry> = runBlocking {
        testResultsRetriesCollection.aggregate<TestResultRetry>(
            match(
                and(
                    TestResultRetry::testRunId eq testRunId),
                    TestResultRetry::fullName eq fullName
                ),
            sort(ascending(TestResultRetry::retry))
        ).toList()
    }

    suspend fun updateActualTestsCount(testRunId: String) {
        val testsCount = getActualTestsCount(testRunId)

        testRunCollection.updateOne(
            TestRun::testRunId eq testRunId,
            set(
                TestRun::tests setTo testsCount
            )
        )
    }

    fun getActualTestsCount(testRunId: String) = runBlocking {
        val filter = TestResult::testRunId eq testRunId

        val totalCount = testResultsCollection.countDocuments(filter).toInt()
        val successCount = testResultsCollection.countDocuments(
            and(filter, TestResult::status eq TestStatus.SUCCESS.status)
        ).toInt()
        val failedCount = testResultsCollection.countDocuments(
            and(filter, TestResult::status eq TestStatus.FAILURE.status)
        ).toInt()

        return@runBlocking TestRunTests(
            testsCount = totalCount,
            failsCount = failedCount,
            successCount = successCount,
        )
    }

    private fun updateTmsTestResult(testResult: TestResult): String? {
        return try {
            if (testResult.tmsLaunchId != null && testResult.project != null) {
                val prjTmsInt = projectIntegrationsService
                    .getProjectTmsInt(testResult.project!!)
                val tmsService = prjTmsInt.tmsInfo?.tmsService()

                return tmsService?.updateTestcase(prjTmsInt.projectTmsInfo?.project!!, testResult)
            } else { null }
        } catch (e: Throwable) { null }
    }

    fun updateTestResult(testResult: TestResult): UpdateResult = runBlocking {
        val skipProperties = mutableListOf("_id", "testRunId", "fullName")
        if (testResult.status != TestStatus.PROCESSING.status && !testResult.localRetriesManagement) {
            skipProperties.add("retries")
        }

        testResult.endDate = DateTimeUtils.currentDateTimeUtc()
        if (testResult.startDate.isNullOrBlank()) {
            testResult.startDate = inferredStartDate(testResult.endDate, testResult.duration)
        }
        testResult.qaHubDuration = testResult.calcQaHubDuration()

        launch {
            updateRetriesInfo(testResult)
        }

        val result = testResultsCollection.updateOne(
            and(
                TestResult::testRunId eq testResult.testRunId,
                TestResult::fullName eq testResult.fullName
            ),
            set(
                *(testResult.setCurrentPropertyValues(skipProperties))
            )
        )

        launch {
            updateActualTestsCount(testResult.testRunId)
        }

        launch {
            updateTmsTestResult(testResult)
        }

        result
    }

    private suspend fun updateRetriesInfo(testResult: TestResult) {
        val filter = and(
            TestResultRetry::testRunId eq testResult.testRunId,
            TestResultRetry::fullName eq testResult.fullName
        )

        val historyItem = StatusHistoryItem(testResult)
        val existingRetries = testResultsRetriesCollection.find(filter).toList()

        if (
            existingRetries.isEmpty() ||
            historyItem.status == TestStatus.PROCESSING.status ||
            TestStatus.isFinal(existingRetries.last().statusHistory.last().status)
        ) {
            testResultsRetriesCollection.insertOne(
                TestResultRetry(
                    testRunId = testResult.testRunId,
                    fullName = testResult.fullName,
                    testcaseId = testResult.testcaseId,
                    retry = existingRetries.size + 1,
                    statusHistory = mutableListOf(historyItem)
                )
            )
        } else {
            testResultsRetriesCollection.updateOne(
                and(
                    TestResultRetry::testRunId eq testResult.testRunId,
                    TestResultRetry::fullName eq testResult.fullName,
                    TestResultRetry::retry eq existingRetries.size,
                ),
                push(
                    TestResultRetry::statusHistory, historyItem
                )
            )
        }
    }

    fun releaseDevice(testRunId: String, simulatorId: String) = runBlocking {
        testResultsCollection.updateMany(
            and(
                TestResult::testRunId eq testRunId,
                TestResult::deviceId eq simulatorId,
                TestResult::status eq TestStatus.PROCESSING.status
            ),
            set(TestResult::deviceId setTo "")
        )
    }

    fun forceStopForDevice(testRunId: String, simulatorId: String, runner: String) = runBlocking {
        val hangingTests = testResultsCollection.find(
            and(TestResult::testRunId eq testRunId,
                TestResult::deviceId eq simulatorId,
                TestResult::status eq TestStatus.PROCESSING.status
            )
        ).toList()

        hangingTests.forEach {
            forceStop(testResult = it)
        }
    }

    private fun forceStop(
        testResult: TestResult,
        message: String = "Unexpected test failure."
    ) = runBlocking {
        testResult.message = message
        testResult.status = TestStatus.FAILURE.status

        updateTestResult(testResult)
    }


    data class TimelineData(
        val startDate: String,
        val endDate: String,
        val runners: List<TimelineRunnerInfo>
    )

    data class TimelineRunnerInfo(
        val runner: String,
        val startDate: String,
        val endDate: String,
        val devices: List<TimelineDeviceInfo>
    )
    data class TimelineDeviceInfo(
        val runner: String,
        val deviceId: String,
        val startDate: String,
        val endDate: String,
        val results: List<TimelineElement>
    )

    data class TimelineElement(
        val fullName: String,
        var testcaseId: String? = null,
        val status: String,
        val startDate: String,
        val endDate: String,
        val duration: Double,
        val retry: Int
    )
    fun getTestrunTimelineData(testRunId: String): TimelineData? = runBlocking {
        val pipeline: MutableList<String> = mutableListOf(
            match(
              TestResultRetry::testRunId eq testRunId
            ).json,

            """
                {
                    ${'$'}addFields: {
                        deviceId: { ${'$'}last: "${'$'}statusHistory.deviceId" },
                        runner: { ${'$'}last: "${'$'}statusHistory.runner" }
                        status: { ${'$'}last: "${'$'}statusHistory.status" }
                        startDate: { ${'$'}first: "${'$'}statusHistory.date" }
                        endDate: { ${'$'}last: "${'$'}statusHistory.date" }
                        duration: { ${'$'}last: "${'$'}statusHistory.duration" }
                        qaHubDuration: { ${'$'}last: "${'$'}statusHistory.qaHubDuration" }
                    }
 	            }
            """.trimIndent(),

            """
                {
                    ${'$'}group: {
                        _id: "${'$'}deviceId",
                        deviceId:  { ${'$'}first: "${'$'}deviceId"}, 
                        runner: { ${'$'}first: "${'$'}runner"},
                        startDate: { ${'$'}min: "${'$'}startDate"},
                        endDate: { ${'$'}max: "${'$'}endDate"},
                        results: { ${'$'}push: "${"$$"}ROOT" }                        
                    }
                }
            """.trimIndent(),

            """
                {
                    ${'$'}sort: {                     
                        "deviceId": 1                       
                    }
                }
            """.trimIndent(),

            """
                {
                    ${'$'}group: {
                        _id: "${'$'}runner",                      
                        runner: { ${'$'}first: "${'$'}runner"},   
                        startDate: { ${'$'}min: "${'$'}startDate"},
                        endDate: { ${'$'}max: "${'$'}endDate"},
                        devices: { ${'$'}push: "${"$$"}ROOT" }                      
                    }
                }
            """.trimIndent(),

            """
                {
                    ${'$'}sort: {                     
                        "runner": 1                       
                    }
                }
            """.trimIndent(),

            """
                {
                    ${'$'}group: {
                        _id: "1",              
                        startDate: { ${'$'}min: "${'$'}startDate"},
                        endDate: { ${'$'}max: "${'$'}endDate"},
                        runners: { ${'$'}push: "${"$$"}ROOT" }                
                    }
                }
            """.trimIndent(),


        )


        val timeline = testResultsRetriesCollection.aggregate<TimelineData>(*pipeline.toTypedArray()).first()
        return@runBlocking timeline?.let { normalizeTimelineBounds(it) }
    }

    private fun normalizeTimelineBounds(timeline: TimelineData): TimelineData {
        val runners = timeline.runners.map { runner ->
            val devices = runner.devices.map { device ->
                val results = device.results.map { normalizeTimelineElement(it) }
                device.copy(
                    results = results,
                    startDate = minIsoDate(results.map { it.startDate } + device.startDate),
                    endDate = maxIsoDate(results.map { it.endDate } + device.endDate)
                )
            }
            runner.copy(
                devices = devices,
                startDate = minIsoDate(devices.map { it.startDate } + runner.startDate),
                endDate = maxIsoDate(devices.map { it.endDate } + runner.endDate)
            )
        }

        return timeline.copy(
            runners = runners,
            startDate = minIsoDate(runners.map { it.startDate } + timeline.startDate),
            endDate = maxIsoDate(runners.map { it.endDate } + timeline.endDate)
        )
    }

    private fun normalizeTimelineElement(element: TimelineElement): TimelineElement {
        val inferredStart = inferredStartDate(element.endDate, element.duration, element.startDate) ?: element.startDate
        return if (inferredStart != element.startDate) {
            element.copy(startDate = inferredStart)
        } else {
            element
        }
    }

    private fun inferredStartDate(endDate: String?, durationSec: Double?, fallbackStart: String? = null): String? {
        if (endDate.isNullOrBlank() || durationSec == null || durationSec <= 0) {
            return fallbackStart ?: endDate
        }

        return try {
            val end = Instant.parse(endDate)
            val fallback = fallbackStart?.let { Instant.parse(it) }
            val spanSec = fallback?.let { java.time.Duration.between(it, end).toMillis() / 1000.0 } ?: 0.0
            if (fallback != null && durationSec <= spanSec + 0.05) {
                fallbackStart
            } else {
                end.minusMillis((durationSec * 1000).toLong()).toString()
            }
        } catch (_: Exception) {
            fallbackStart ?: endDate
        }
    }

    private fun minIsoDate(dates: List<String?>): String {
        return dates.filterNotNull().filter { it.isNotBlank() }.minByOrNull { Instant.parse(it) } ?: ""
    }

    private fun maxIsoDate(dates: List<String?>): String {
        return dates.filterNotNull().filter { it.isNotBlank() }.maxByOrNull { Instant.parse(it) } ?: ""
    }
}