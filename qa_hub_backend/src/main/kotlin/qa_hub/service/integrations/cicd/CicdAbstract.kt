package qa_hub.service.integrations.cicd

import qa_hub.entity.ProjectCicdInfo
import qa_hub.service.integrations.cicd.github.GitHubService
import qa_hub.service.integrations.cicd.gitlab.GitlabService
import qa_hub.service.integrations.cicd.teamcity.TeamcityService

data class CicdType(
    val cicdName: String,
    val authTypes: List<String> = CicdAuthType.values().map{ it.authType }
)

enum class CicdTypes(
    val cicdType: CicdType
) {
    GITHUB(CicdType("GitHub")),
    GITLAB(CicdType("GitLab")),
    TEAMCITY(CicdType("Teamcity"))
}

enum class CicdAuthType(val authType: String) {
    API_TOKEN("ApiToken"),
    PASSWORD("Password")
}

data class CicdInfo(
    var _id: String? = null,
    val cicdType: String,
    val baseUrl: String,
    val apiUrl: String,
    val apiToken: String? = null,
    val login: String? = null,
    val password: String? = null,
) {
    fun cicdService(): CicdIntegrationAbstract? {
        return when (cicdType) {
            CicdTypes.GITHUB.cicdType.cicdName -> GitHubService(this)
            CicdTypes.GITLAB.cicdType.cicdName -> GitlabService(this)
            CicdTypes.TEAMCITY.cicdType.cicdName -> TeamcityService(this)
            else -> null
        }
    }
}

data class StartJobRequest(
    val gitRef: String,
    val params: Map<String, String>
)

data class StartJobResponse(
    val code: Int,
    val message: String?
)

abstract class CicdIntegrationAbstract(val cicdInfo: CicdInfo) {
    abstract fun startJob(info: ProjectCicdInfo, jobId: String, startJobRequest: StartJobRequest): StartJobResponse

    abstract fun stopJob(info: ProjectCicdInfo, jobId: String)

    abstract fun getBranches(info: ProjectCicdInfo): List<String>
}

