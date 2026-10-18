import EditableTableCell from "../primitives/Table/EditableTableCell";
import {observer} from "mobx-react-lite";
import projectIntegrationsState from "../../state/integrations/ProjectIntegrationsState";
import blockerState from "../../state/BlockerState";
import TaskLink from "./TaskLink";
import TaskStatus from "./TaskStatus";


const TaskStatusCell = observer(({blockedTest, onChangeCallback, onBlurCallback, ...props}) => {
  const taskTrackerInfo = projectIntegrationsState.taskTrackerInt || {}
  const statusInfo = blockerState.taskStatuses[blockedTest.tmsTask] || {}

  const handleBlur = () => {
    blockerState.ensureTaskStatus(blockedTest.project, blockedTest.tmsTask)
    onBlurCallback?.()
  }


  return <EditableTableCell
    value={blockedTest.tmsTask}
    content={
      <div style={{
        display: 'grid',
        gridTemplateColumns: '1fr 10px 1fr',
        alignItems: 'center',
        width: '100%'
      }}>
        <div style={{display: 'flex', justifyContent: 'flex-end', minWidth: 0}}>
          <TaskLink blockedTest={blockedTest} taskUrl={taskTrackerInfo.taskUrl}/>
        </div>
        <div />
        <div style={{display: 'flex', justifyContent: 'flex-start', minWidth: 0}}>
          <TaskStatus status={statusInfo.statusName} color={statusInfo.statusColor} style={{marginLeft: 0}}/>
        </div>
      </div>
    }
    contentStyle={{
      width: '100%',
      maxWidth: '100%'
    }}
    onChangeCallback={ onChangeCallback }
    onBlurCallback={ handleBlur }
    {...props}
  />
})

export default TaskStatusCell


