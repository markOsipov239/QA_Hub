package qa_hub.service.integrations.taskTrackers.jira

import qa_hub.service.integrations.taskTrackers.jira.entity.JiraIssueResponse
import qa_hub.service.integrations.taskTrackers.jira.entity.JiraSearchRequest

class JiraClient(baseUrl: String, apiToken: String) {
    private val client = JiraHttpInterface.getClient(baseUrl, apiToken)

    fun getIssue(key: String): JiraIssueResponse {
        return JiraIssueResponse(
            client.getIssue(key)
                .execute()
                .body()!!
        )
    }

    fun searchIssues(keys: List<String>): List<JiraIssueResponse> {
        if (keys.isEmpty()) {
            return emptyList()
        }

        return keys.chunked(50).flatMap { chunk ->
            val jql = "key in (${chunk.joinToString(",")})"
            val response = client.searchIssues(
                JiraSearchRequest(
                    jql = jql,
                    maxResults = chunk.size
                )
            ).execute().body() ?: return@flatMap emptyList()

            response.issues.map { JiraIssueResponse(it) }
        }
    }
}
