import {useEffect, useState} from "react";
import {getBackendVersion, getFrontendVersion} from "../../requests/UtilsRequests";
import {customTheme} from "../../styles/CustomTheme";

const unknownVersion = {
    branch: "unknown",
    commit: "unknown",
    dirty: false,
    buildTime: "unknown"
}

function formatVersion(info) {
    const version = info || unknownVersion
    const dirtyLabel = version.dirty ? "dirty" : "clean"

    return `${version.branch} @ ${version.commit} (${dirtyLabel}) · ${version.buildTime}`
}

export default function BuildVersionInfo() {
    const [frontendVersion, setFrontendVersion] = useState(unknownVersion)
    const [backendVersion, setBackendVersion] = useState(unknownVersion)

    useEffect(() => {
        getFrontendVersion()
            .then(response => {
                if (response?.data) {
                    setFrontendVersion(response.data)
                }
            })
            .catch(() => setFrontendVersion(unknownVersion))

        getBackendVersion()
            .then(response => {
                if (response?.data) {
                    setBackendVersion(response.data)
                }
            })
            .catch(() => setBackendVersion(unknownVersion))
    }, [])

    return <div style={{
        position: "absolute",
        right: "16px",
        bottom: "12px",
        textAlign: "right",
        fontSize: "12px",
        lineHeight: 1.5,
        color: customTheme.palette.text.faded,
        pointerEvents: "none",
        zIndex: 1
    }}>
        <div>Frontend: {formatVersion(frontendVersion)}</div>
        <div>Backend: {formatVersion(backendVersion)}</div>
    </div>
}
