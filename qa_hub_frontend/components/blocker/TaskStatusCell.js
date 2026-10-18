import EditableTableCell from "../primitives/Table/EditableTableCell";
import {useEffect, useState} from "react";
import {getTaskStatus} from "../../requests/BlockerRequests";
import {observer} from "mobx-react-lite";
import projectIntegrationsState from "../../state/integrations/ProjectIntegrationsState";
import TaskLink from "./TaskLink";
import TaskStatus from "./TaskStatus";


const TaskStatusCell = observer(({blockedTest, onChangeCallback, onBlurCallback, ...props}) => {
  const [status, setStatus] = useState(null)
  const [color, setColor] = useState(null)
  const taskTrackerInfo = projectIntegrationsState.taskTrackerInt || {}

  useEffect(() => {
    getTaskStatus(blockedTest.project, blockedTest.tmsTask).then((data) => {
      setStatus(data?.data?.statusInfo?.statusName)
      setColor(data?.data?.statusInfo?.statusColor || null)
    })
  }, [blockedTest.tmsTask])


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
          <TaskStatus status={status} color={color} style={{marginLeft: 0}}/>
        </div>
      </div>
    }
    contentStyle={{
      width: '100%',
      maxWidth: '100%'
    }}
    onChangeCallback={ onChangeCallback }
    onBlurCallback={ onBlurCallback }
    {...props}
  />
})

export default TaskStatusCell


