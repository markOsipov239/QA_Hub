package qa_hub.service.integrations.tms.testrail.entity

import com.google.gson.annotations.SerializedName

data class TestRailAddRunRequest(
    val name: String,
    @SerializedName("case_ids") val caseIds: List<Int>,
)

data class TestRailRunResponse(
    val id: Int,
    @SerializedName("project_id") val projectId: Int,
    val name: String,
    val is_completed: Boolean
)

data class TestRailCasesPage(
    val offset: Int? = null,
    val limit: Int? = null,
    val size: Int? = null,
    val cases: List<TestRailCase> = listOf()
)

data class TestRailSeparatedStep(
    val content: String,
    val expected: String
)

data class TestRailCase(
    @SerializedName("case_id") val caseId: Int,
    val title: String,
    @SerializedName("custom_steps") val customSteps: String?,
    @SerializedName("custom_steps_separated") val customStepsSeparated: List<TestRailSeparatedStep>?,
    @SerializedName("is_deleted") val isDeleted: Boolean? = null,
    @SerializedName("custom_automation_type") val customAutomationType: Int,
    @SerializedName("custom_preconds") val customPreconds: String
)

data class TestRailAddResultRequest(
    @SerializedName("status_id") val statusId: Int,
    val comment: String? = null,
    val elapsed: String? = null
)

data class TestRailAddResultResponse(
    val id: Int? = null
)

enum class TestrailStatus(val code: Int, val statusName: String) {
    ACTUAL(0, "Actual"),
    DRAFT(1, "Draft"),
    DEPRECATED(2, "Deprecated"),
    REVIEW(3, "Review");

    companion object {
        fun getNameByCode(code: Int): String {
            return entries.firstOrNull{
                it.code == code
            }?.statusName ?: "Unknown"
        }
    }
}