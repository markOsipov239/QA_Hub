package qa_hub.service.integrations.taskTrackers.jira

import qa_hub.service.integrations.taskTrackers.TaskStatusInfo
import qa_hub.service.integrations.taskTrackers.TaskStatusResponse
import qa_hub.service.integrations.taskTrackers.TaskTrackerInfo
import qa_hub.service.integrations.taskTrackers.TaskTrackerIntegrationAbstract


class JiraService(info: TaskTrackerInfo): TaskTrackerIntegrationAbstract(info) {
    val client = JiraClient(info.baseUrl, info.apiToken!!)

    override fun getTaskStatus(task: String): TaskStatusResponse {
        val response = client.getIssue(task)

        val statusInfo = TaskStatusInfo(
            statusColor = response.statusColor,
            statusName = response.statusName
        )

        return TaskStatusResponse(
            task = task,
            statusInfo = statusInfo
        )
    }

    override fun getTaskStatuses(tasks: List<String>): List<TaskStatusResponse> {
        val uniqueTasks = tasks.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (uniqueTasks.isEmpty()) {
            return emptyList()
        }

        val jiraKeys = uniqueTasks.filter { JIRA_KEY_REGEX.matches(it) }
        val otherKeys = uniqueTasks - jiraKeys.toSet()

        val fromSearch = try {
            if (jiraKeys.isEmpty()) {
                emptyList()
            } else {
                val issuesByKey = client.searchIssues(jiraKeys)
                    .mapNotNull { issue -> issue.key?.uppercase()?.let { it to issue } }
                    .toMap()

                jiraKeys.mapNotNull { task ->
                    val issue = issuesByKey[task.uppercase()] ?: return@mapNotNull null
                    TaskStatusResponse(
                        task = task,
                        statusInfo = TaskStatusInfo(
                            statusColor = issue.statusColor,
                            statusName = issue.statusName
                        )
                    )
                }
            }
        } catch (_: Exception) {
            jiraKeys.mapNotNull { task ->
                try {
                    getTaskStatus(task)
                } catch (_: Exception) {
                    null
                }
            }
        }

        val fromSingle = otherKeys.mapNotNull { task ->
            try {
                getTaskStatus(task)
            } catch (_: Exception) {
                null
            }
        }

        return fromSearch + fromSingle
    }

    companion object {
        private val JIRA_KEY_REGEX = Regex("^[A-Za-z][A-Za-z0-9_]+-\\d+$")
    }
}