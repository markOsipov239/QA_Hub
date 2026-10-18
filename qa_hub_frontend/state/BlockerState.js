import {makeAutoObservable} from "mobx";
import {blockTest, editBlockedTest, getBlockedTests, getTaskStatuses, unblockTest} from "../requests/BlockerRequests";

class BlockerState {
  blockedTests = []
  taskTrackerInfo = {}
  tmsInfo = {}
  taskStatuses = {}
  loadingTaskStatuses = false
  pendingStatusRequests = {}

  setBlockedTests(blockedTests) {
    this.blockedTests = blockedTests
  }

  mergeTaskStatuses(statusList, requestedTasks = []) {
    const next = { ...this.taskStatuses }
    statusList.forEach((item) => {
      if (item?.task) {
        next[item.task] = item.statusInfo || { statusName: null, statusColor: null }
      }
    })
    requestedTasks.forEach((task) => {
      if (task && !(task in next)) {
        next[task] = { statusName: null, statusColor: null }
      }
    })
    this.taskStatuses = next
  }

  loadTaskStatuses(project) {
    const tasks = [...new Set(this.blockedTests.map((test) => test.tmsTask).filter(Boolean))]
    if (!project || tasks.length === 0) {
      return
    }

    this.loadingTaskStatuses = true
    getTaskStatuses(project, tasks)
      .then((response) => {
        this.mergeTaskStatuses(response.data || [], tasks)
      })
      .catch(() => {
        this.mergeTaskStatuses([], tasks)
      })
      .finally(() => {
        this.loadingTaskStatuses = false
      })
  }

  ensureTaskStatus(project, task) {
    if (!project || !task || this.taskStatuses[task] !== undefined || this.loadingTaskStatuses) {
      return
    }
    if (this.pendingStatusRequests[task]) {
      return
    }

    this.pendingStatusRequests[task] = getTaskStatuses(project, [task])
      .then((response) => {
        this.mergeTaskStatuses(response.data || [], [task])
      })
      .catch(() => {
        this.mergeTaskStatuses([], [task])
      })
      .finally(() => {
        delete this.pendingStatusRequests[task]
      })
  }

  updateBlockedTests(project, afterAction) {
    getBlockedTests(project).then(blockedTestsResponse => {
      this.setBlockedTests(blockedTestsResponse.data)

      if (afterAction) {
        afterAction()
      }
    })
  }

  blockTest(newBlockedTest, afterAction) {
    blockTest(newBlockedTest).then(() => {
      this.updateBlockedTests(newBlockedTest.project, afterAction)
    })
  }

  unblockTest(blockedTest) {
    unblockTest(blockedTest).then( response => {
      if (response.data.deletedCount > 0) {
        this.updateBlockedTests(blockedTest.project)
      }
    })
  }

  editBlockedTest(blockedTest) {
    editBlockedTest(blockedTest).then( response => {
      if (response.data.deletedCount > 0) {
        this.updateBlockedTests(blockedTest.project)
      }
    })
  }

  constructor() {
    makeAutoObservable(this, {
      pendingStatusRequests: false
    })
  }
}

export default new BlockerState()