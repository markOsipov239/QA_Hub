package qa_hub.service.integrations.tms.testrail

import com.fasterxml.jackson.databind.introspect.AnnotationCollector.TwoAnnotations
import qa_hub.entity.testRun.TestResult
import qa_hub.entity.testRun.TestStatus
import qa_hub.service.integrations.tms.CommonTestcase
import qa_hub.service.integrations.tms.testrail.entity.TestRailAddResultRequest
import qa_hub.service.integrations.tms.testrail.entity.TestRailAddRunRequest
import qa_hub.service.integrations.tms.testrail.entity.TestRailRunResponse
import qa_hub.service.integrations.tms.testrail.entity.TestrailStatus

class TestRailClient(
    baseUrl: String,
    user: String,
    password: String
) {
    private val client = TestRailHttpInterface.getClient(baseUrl, user, password)

    fun createTestRun(projectId: String, testRunName: String?, testIds: List<Int>): TestRailRunResponse {
        val body = TestRailAddRunRequest(
            name = testRunName ?: "Autotest launch",
            caseIds = testIds
        )
        val response = client.addRun(projectId, body).execute().body()!!

        return response
    }

    fun closeTestRun(testRunId: String): TestRailRunResponse {
        val response = client.closeRun(testRunId).execute().body()!!

        return response
    }

    fun getTestcases(projectId: String): List<CommonTestcase> {
        val limit = 100
        var page = 0
        var finished = false
        var errors = 0

        val result = mutableListOf<CommonTestcase>()
        while (!finished && errors <= 5) {
            try {
                val offset = page * limit
                val response = client.getCases(projectId, limit, offset).execute().body()!!
                result.addAll(response.cases.map {
                    CommonTestcase(
                        it.caseId.toString(),
                        it.customAutomationType != 0,
                        TestrailStatus.ACTUAL.statusName)
                })
                page += 1
                finished = response.cases.size < limit
            } catch (e: Exception) {
                e.printStackTrace()
                errors += 1
            }

        }
        return result
    }

    fun postTestResult(testResult: TestResult): String {
        val runId = testResult.tmsLaunchId!!
        val caseId = testResult.testcaseId

        val statusId = when (testResult.status) {
            TestStatus.SUCCESS.status -> TestRailResultStatus.PASSED.id
            TestStatus.SKIPPED.status -> TestRailResultStatus.BLOCKED.id
            TestStatus.FAILURE.status -> TestRailResultStatus.FAILED.id
            else -> TestRailResultStatus.UNTESTED.id
        }
        val elapsed = testResult.duration?.let { d ->
            if (d <= 0) null else "${d.toInt()}s"
        }
        val body = TestRailAddResultRequest(
            statusId = statusId,
            comment = testResult.message,
            elapsed = elapsed
        )
        val response = client.addResultForCase(runId, caseId, body).execute().body()
        return response?.id?.toString() ?: "${runId}_$caseId"
    }
}


private enum class TestRailResultStatus(val id: Int) {
    PASSED(1),
    BLOCKED(2),
    UNTESTED(3),
    RETEST(4),
    FAILED(5)
}
