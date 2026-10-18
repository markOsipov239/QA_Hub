import {effectiveResultStart, getDateTime, roundSeconds, secondsBetween} from "../../../../../utils/DateTimeUtils";
import StyledTooltip from "../../../../primitives/StyledTooltip";
import {customTheme} from "../../../../../styles/CustomTheme";

const TimelineElement = ({ startDate, endDate, duration, result, hoveredTest, setHoveredTest, ...props }) => {
  const calculateDuration = () => {
    if (result.duration) {
        return roundSeconds(result.duration)
    }
    if (result.qaHubDuration) {
        return roundSeconds(result.qaHubDuration)
    }
    return secondsBetween(result.startDate, result.endDate)
  }
  const resultDuration = calculateDuration()
  const resultStart = effectiveResultStart(result)
  const secondsFromStart = Math.max(0, secondsBetween(startDate, resultStart))
  const secondsBeforeEnd = secondsFromStart + resultDuration

  const relativeStart = duration > 0 ? secondsFromStart / duration * 100 : 0
  const relativeWidth = duration > 0 ? resultDuration / duration * 100 : 0

  const TimelineElementTooltip = () => {
    return <div style={{display: 'grid'}}>
      <label style={{fontWeight: 'bold'}}>{result.fullName}</label>

      <div style={{display: 'grid', marginTop: '10px'}}>
        <div style={{display: 'flex'}}>
          <label>Started: {secondsFromStart}s</label>
          <label style={{marginLeft: '15px', color: 'rgba(255, 255, 255, 0.5)'}}>{getDateTime(resultStart)}</label>

        </div>

        <div style={{display: 'flex'}}>
          <label style={{marginLeft: '5px'}}>Ended: {secondsBeforeEnd}s</label>
          <label style={{marginLeft: '15px', color: 'rgba(255, 255, 255, 0.5)'}}>{getDateTime(result.endDate)}</label>
        </div>

        <label style={{marginTop: '5px'}}>Duration: {resultDuration.toFixed(2)}s</label>
      </div>

      <label style={{marginTop: '10px'}}>Retry: {result.retry}</label>
    </div>
  }

  return <StyledTooltip
    maxWidth={'800px'}
    title={<TimelineElementTooltip />}
  >
    <div
      style={{
        position: 'absolute',
        left: `calc(${relativeStart}% + 1px)`,
        backgroundColor: result.status === 'SUCCESS' ? customTheme.palette.success.main : customTheme.palette.error.main,
        height: '100%',
        width: `max(2px, calc(${relativeWidth}% - 2px))`,
        ...props.style
      }}
      onMouseOver={() => setHoveredTest(result.fullName)}
      onMouseLeave={() => setHoveredTest(null)}
      onBlur={() => setHoveredTest(null)}
      onClick={() => {  window.open(window.location.href.split("/charts")[0] + `?test=${result.testcaseId || result.fullName}`, "_blank")}}
    >
      {
        hoveredTest === result.fullName &&
        <div style={{ width: '100%', height: '100%', backgroundColor: 'rgba(255, 255, 255, 0.5)' }}/>
      }
    </div>
  </StyledTooltip>
}

export default TimelineElement