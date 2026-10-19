package qa_hub.service.testResults.testStats


import kotlinx.coroutines.runBlocking
import org.litote.kmongo.coroutine.aggregate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import qa_hub.core.mongo.QaHubMongoClient
import qa_hub.core.mongo.entity.Collections
import org.litote.kmongo.*
import qa_hub.entity.testRun.*

data class GeneralStats(
    val totalTests: Int = 0,
    val totalTestResults: Int = 0,
    val successTestResults: Int = 0,
    val totalRetries: Int = 0,
    val avgTestRetries: Double = 0.0,
    val successTestResultRate: Double = 0.0,
    val successTestRetryRate: Double = 0.0,
    var avgRetryDuration: Double = 0.0,
)

data class RetryDurationStats(
    val avgRetryDuration: Double = 0.0,
)

@Service
class GeneralStatsHelper {
    @Autowired
    lateinit var mongoClient: QaHubMongoClient

    @Autowired
    lateinit var testRunFilterHelper: TestRunFilterHelper

    private val testResultsRetriesCollection by lazy {
        mongoClient.db.getCollection<TestResult>(Collections.TEST_RESULTS_RETRIES.collectionName)
    }

    private val testResultsCollection by lazy {
        mongoClient.db.getCollection<TestResult>(Collections.TEST_RESULTS.collectionName)
    }


    fun getTestResultFilter(request: TestStatsRequest, testRunIds: List<String>): String {
        val filters = mutableListOf(
            TestResult::testRunId `in` testRunIds,
            or(
                TestResult::status `in` TestStatus.finalStatuses,
                TestResultRetry::statusHistory / StatusHistoryItem::status `in` TestStatus.finalStatuses
            )
        )

        request.filter?.search?.let {
           filters.add(
               or(
                   TestResult::fullName regex Regex(request.filter.search, RegexOption.IGNORE_CASE),
                   TestResult::testcaseId regex Regex(request.filter.search, RegexOption.IGNORE_CASE)
               )
           )
        }

        return match(*filters.toTypedArray()).json
    }

    private fun getTestResultsGeneralStats(request: TestStatsRequest, testRunIds: List<String>): GeneralStats = runBlocking {
        if (testRunIds.isEmpty()) {
            return@runBlocking GeneralStats()
        }

        val pipeline: MutableList<String> = mutableListOf(
            getTestResultFilter(request, testRunIds),
            """
                {
                    ${'$'}group: {
                        _id: "${'$'}fullName",
                        fullName: { ${'$'}first: "${'$'}fullName"},
                        testcaseId: { ${'$'}last: "${'$'}testcaseId"},
                        totalTestResults: {${'$'}sum: 1},
                        avgRetries: { ${'$'}avg: "${'$'}retries"},
                        totalRetries: { ${'$'}sum: "${'$'}retries"},
                        successTestResults : {
                             ${'$'}sum: {
                                 ${'$'}cond: {
                                    if: { ${'$'}eq: [ "${'$'}status", "SUCCESS" ] },
                                    then: 1,
                                    else: 0
                                }
                            }
                        }
                    }
                }
            """.trimIndent(),
            """
                {
                    ${'$'}group: {
                        _id: "stats",
                        totalTests: {${'$'}sum: 1},
                        totalTestResults: {${'$'}sum: "${'$'}totalTestResults"},
                        successTestResults: {${'$'}sum: "${'$'}successTestResults"},
                        totalRetries: { ${'$'}sum: "${'$'}totalRetries"},
                        avgTestRetries: { ${'$'}avg: "${'$'}avgRetries"}
                    }
                }
            """.trimIndent(),
            """
                {
                    ${'$'}addFields: {
                        successTestResultRate: {
                            "${'$'}cond": [
                                { "${'$'}gt": ["${'$'}totalTestResults", 0] },
                                { "${'$'}divide": ["${'$'}successTestResults", "${'$'}totalTestResults"] },
                                0
                            ]
                        },
                        successTestRetryRate: {
                            "${'$'}cond": [
                                { "${'$'}gt": ["${'$'}totalRetries", 0] },
                                { "${'$'}divide": ["${'$'}successTestResults", "${'$'}totalRetries"] },
                                0
                            ]
                        }
                    }
 	            }
            """.trimIndent()
        )

        return@runBlocking testResultsCollection
            .aggregate<GeneralStats>(*pipeline.toTypedArray())
            .toList()
            .firstOrNull()
            ?: GeneralStats()
    }

    private fun getTestResultsRetriesGeneralStats(request: TestStatsRequest, testRunIds: List<String>): RetryDurationStats = runBlocking {
        if (testRunIds.isEmpty()) {
            return@runBlocking RetryDurationStats()
        }

        val pipeline: MutableList<String> = mutableListOf(
            getTestResultFilter(request, testRunIds),
            """
                {
                    ${'$'}addFields: {
                        finalStatus: { ${'$'}last: "${'$'}statusHistory"}
                    }
                }
            """.trimIndent(),
            """
                {
                    ${'$'}addFields: {
                        duration: "${'$'}finalStatus.duration"
                    }
                }
            """.trimIndent(),
            """
                {
                    ${'$'}match: {
                        duration: { ${'$'}ne: null}
                    }
                }
            """.trimIndent(),
            """
                {
                    ${'$'}group: {
                        _id: "stats",
                        avgRetryDuration: { ${'$'}avg: "${'$'}duration" }
                    }
                }
            """.trimIndent()
        )

        return@runBlocking testResultsRetriesCollection
            .aggregate<RetryDurationStats>(*pipeline.toTypedArray())
            .toList()
            .firstOrNull()
            ?: RetryDurationStats()
    }

    fun getGeneralStats(request: TestStatsRequest): GeneralStats = runBlocking {
        val filteredTestRuns = testRunFilterHelper.getFilteredTestRuns(request.project, request.filter)
        val testRunIds = filteredTestRuns.map { it.testRunId }

        val generalStats = getTestResultsGeneralStats(request, testRunIds)
        val retryGeneralStats = getTestResultsRetriesGeneralStats(request, testRunIds)

        generalStats.avgRetryDuration = retryGeneralStats.avgRetryDuration

        return@runBlocking generalStats
    }
}
