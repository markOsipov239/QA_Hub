package qa_hub.controller.testRuns

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import qa_hub.entity.testRun.SingleTestStats
import qa_hub.entity.testRun.TestHistoryRequest
import qa_hub.entity.testRun.TestStats
import qa_hub.entity.testRun.TestStatsRequest
import qa_hub.service.testResults.testStats.GeneralStats
import qa_hub.service.testResults.testStats.TestStatsService

@RestController
@RequestMapping("/api/stats")
class TestStatsController {

    @Autowired
    lateinit var testStatsService: TestStatsService

    @PostMapping("")
    fun getStatsForProject(@RequestBody request: TestStatsRequest): List<TestStats> {
        return testStatsService.getStatsForProject(request)
    }

    @PostMapping("/general")
    fun getGeneralStats(@RequestBody request: TestStatsRequest): GeneralStats {
        return testStatsService.getGeneralStats(request)
    }

    @PostMapping("/alt")
    fun getStatsForProjectAlt(@RequestBody request: TestStatsRequest): List<TestStats> {
        return testStatsService.getStatsForProjectAlt(request)
    }

    @PostMapping("/history")
    fun getTestHistory(@RequestBody request: TestHistoryRequest): SingleTestStats? {
        return testStatsService.getTestHistory(request)
    }
}