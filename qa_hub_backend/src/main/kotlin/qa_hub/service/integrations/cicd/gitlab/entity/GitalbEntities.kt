package qa_hub.service.integrations.cicd.gitlab.entity


data class GitlabBranch(
    val name: String,
    val protected: Boolean,
    val commit: GitlabCommit
)

data class GitlabCommit(
    val id: String,
    val shord_id: String,
    val web_url: String
)

data class GitlabStartPipelineRequest(
    val ref: String,
    val variables: List<GitlabVariable>
)

data class GitlabVariable(
    val key: String,
    val value: String
)

data class GitlabPipeline(
    val id: Int,
    val iid: Int,
    val project_id: Int,
    val ref: String,
    val status: String,
    val source: String
)