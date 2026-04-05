package qa_hub.service.integrations.cicd.gitlab

import qa_hub.entity.ProjectCicdInfo
import qa_hub.service.integrations.cicd.CicdInfo
import qa_hub.service.integrations.cicd.CicdIntegrationAbstract
import qa_hub.service.integrations.cicd.StartJobRequest
import qa_hub.service.integrations.cicd.StartJobResponse
import qa_hub.service.integrations.cicd.gitlab.entity.GitlabStartPipelineRequest
import qa_hub.service.integrations.cicd.gitlab.entity.GitlabVariable

class GitlabService(cicdInfo: CicdInfo): CicdIntegrationAbstract(cicdInfo) {
    val client = GitlabClient(cicdInfo.apiUrl, cicdInfo.apiToken!!)

    override fun startJob(info: ProjectCicdInfo, jobId: String, startJobRequest: StartJobRequest): StartJobResponse {
        val body = GitlabStartPipelineRequest(
            ref = startJobRequest.gitRef,
            variables = startJobRequest.params.map{ GitlabVariable(it.key, it.value) }
        )
        val response = client.startPipeline(info.project, body)
        val code = response.code()
        var message: String? = null

        response.errorBody()?.let {
            message = it.string()
        }

        return StartJobResponse(
            code, message
        )
    }

    override fun stopJob(info: ProjectCicdInfo, jobId: String) {
        client.cancelPipeline(info.project, jobId)
    }

    override fun getBranches(info: ProjectCicdInfo): List<String> {
        val branches = mutableListOf<String>()
        var finished = false
        var page = 1
        val maxPages = 5
        while (!finished) {
            val response = client.getBranches(info.project, 100, page).map { it.name }
            branches.addAll(response)
            page += 1

            finished = response.isEmpty() || page == maxPages
        }
        return branches
    }
}