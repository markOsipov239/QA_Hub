package qa_hub.service.integrations.cicd.gitlab

import qa_hub.service.integrations.cicd.gitlab.entity.GitlabBranch
import qa_hub.service.integrations.cicd.gitlab.entity.GitlabPipeline
import qa_hub.service.integrations.cicd.gitlab.entity.GitlabStartPipelineRequest
import retrofit2.Response

class GitlabClient(apiUrl: String, apiToken: String) {
    private val gitlabService = GitlabHttpInterface.getClient(apiUrl, apiToken)

    fun getBranches(projectId: String, perPage: Int = 100, page: Int = 1): List<GitlabBranch> {
        return gitlabService
            .getBranches(projectId, perPage, page)
            .execute()
            .body()!!
    }

    fun startPipeline(project: String, body: GitlabStartPipelineRequest): Response<GitlabPipeline?> {
        return gitlabService
            .startPipeline(project, body)
            .execute()
    }

    fun cancelPipeline(project: String, jobId: String): Int {
        return gitlabService
            .cancelPipeline(project, jobId)
            .execute()
            .code()
    }
}