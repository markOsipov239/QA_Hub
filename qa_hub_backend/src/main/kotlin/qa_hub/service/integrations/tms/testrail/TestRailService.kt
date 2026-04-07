package qa_hub.service.integrations.tms.testrail

import qa_hub.entity.testRun.TestResult
import qa_hub.service.integrations.tms.CommonTestcase
import qa_hub.service.integrations.tms.TmsInfo
import qa_hub.service.integrations.tms.TmsIntegrationAbstract
import qa_hub.service.integrations.tms.TmsProjectAbstract

class TestRailService(info: TmsInfo) : TmsIntegrationAbstract(info) {

    private val client = TestRailClient(
        baseUrl = info.apiUrl,
        user = info.login!!,
        password = info.password!!
    )

    override fun getProjects(): List<TmsProjectAbstract> {
        TODO("Not yet implemented")
    }

    override fun getProject(projectId: String): TmsProjectAbstract {
        TODO("Not yet implemented")
    }

    override fun getTestcases(projectId: String): List<CommonTestcase> {
        return client.getTestcases(projectId)
    }

    override fun getTestcase(projectId: String, testcaseId: String): CommonTestcase {
        TODO("Not yet implemented")
    }

    override fun updateTestcase(tmsProject: String, testResult: TestResult): String {
        return client.postTestResult(testResult)
    }

    override fun startTestrun(projectId: String, testRunName: String?, testIds: List<String>): String {
        return client.createTestRun(projectId, testRunName, testIds.map{ it.toInt() }).id.toString()
    }

    override fun completeTestrun(projectId: String, testRunId: String): String {
        return client.closeTestRun(testRunId).id.toString()
    }
}
