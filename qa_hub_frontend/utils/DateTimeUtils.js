export function getDate(dateStr) {
    return new Date(dateStr).toLocaleString('ru').split(", ")[0]
}

export function getTimeSeconds(dateStr) {
    return new Date(dateStr).toLocaleString('ru').split(", ")[1]
}

export function getDateTime(dateStr) {
    const dateComponents = new Date(dateStr).toLocaleString('ru').split(", ")
    const date = dateComponents[0]
    const time = dateComponents[1]
    const timeMinutes = time.substring(0, time.lastIndexOf(":"))

    return `${timeMinutes} ${date}`
}

export function getTimeMinutes(timestamp) {
    let time = new Date(timestamp)
      .toLocaleString('ru')
      .split(", ")[1]

    return time.substring(0, time.lastIndexOf(":"))
}

export function daysBetween(date1, date2) {
    const parsedDate1 = new Date(date1)
    const parsedDate2 = date2 && new Date(date2) || new Date()

    const coeff = (1000 * 3600 * 24)
    const timediff = (parsedDate2.getTime() - parsedDate1.getTime()) / coeff
    return Number.parseInt(timediff)
}

export function roundSeconds(value) {
    return Math.round(Number(value) * 100) / 100
}

export function secondsBetween(date1, date2) {
    const parsedDate1 = new Date(date1)
    const parsedDate2 = date2 && new Date(date2) || new Date()

    const coeff = (1000)
    const timediff = (parsedDate2.getTime() - parsedDate1.getTime()) / coeff
    return roundSeconds(timediff)
}

export function effectiveResultStart(result) {
    if (!result?.endDate || !result?.startDate) {
        return result?.startDate || result?.endDate || null
    }

    const end = new Date(result.endDate).getTime()
    const start = new Date(result.startDate).getTime()
    if (Number.isNaN(end) || Number.isNaN(start)) {
        return result.startDate
    }

    const spanSec = (end - start) / 1000
    const duration = result.duration || result.qaHubDuration || spanSec
    if (duration > spanSec + 0.05) {
        return new Date(end - duration * 1000).toISOString()
    }

    return result.startDate
}

export function earliestTimelineStart(timelineData) {
    if (!timelineData?.startDate && !(timelineData?.runners || []).length) {
        return null
    }

    let min = new Date(timelineData.startDate).getTime()
    if (Number.isNaN(min)) {
        min = Number.POSITIVE_INFINITY
    }

    ;(timelineData.runners || []).forEach((runner) => {
        (runner.devices || []).forEach((device) => {
            (device.results || []).forEach((result) => {
                const start = new Date(effectiveResultStart(result)).getTime()
                if (!Number.isNaN(start)) {
                    min = Math.min(min, start)
                }
            })
        })
    })

    if (!Number.isFinite(min)) {
        return timelineData.startDate || null
    }

    return new Date(min).toISOString()
}