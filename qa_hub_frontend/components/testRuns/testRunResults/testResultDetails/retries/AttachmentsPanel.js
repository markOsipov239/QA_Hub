import {useEffect, useRef, useState} from "react";
import { getTestAttachments } from "../../../../../requests/testResults/TestAttachmentsRequests";
import Loader from "../../../../common/Loader";
import { customTheme } from "../../../../../styles/CustomTheme";
import LogsPanel from "./logs/LogPanel";
import ScreenshotAttachmentPanel from "./logs/ScreenshotAttachmentPanel";

export default function AttachmentsPanel({ retry, selectedStep, setSelectedStep, ...props }) {

    const [attachments, setAttachments] = useState(retry.attachments)
    const [selectedAttachment, setSelectedAttachment] = useState(null)

    useEffect(() => {
        setAttachments(retry.attachments)
    }, [retry.testRunId, retry.fullName, retry.retry])

    return <div style={{marginLeft: "15px", ...props.style}}>
        <div style={{ display: "flex", alignItems: "center"}}>
            <label style={{padding: "5px 0px"}}>Attachments</label>
        </div>
         <div style={{display: "flex"}}>
            {
                attachments.map((attachment, index) => {
                    return <AttachmentTitle attachment={attachment} selectedAttachment={selectedAttachment} setSelectedAttachment={setSelectedAttachment} key={index}/>
                })
            }
        </div>
        {
            selectedAttachment?.type == "text" &&
            <LogsPanel attachment={selectedAttachment} retry={retry} selectedStep={selectedStep} setSelectedStep={setSelectedStep}></LogsPanel>
        }
        {
            selectedAttachment?.type == "image" &&
            <ScreenshotAttachmentPanel attachment={selectedAttachment} retry={retry}></ScreenshotAttachmentPanel>
        }
    </div>
}

function AttachmentTitle({attachment, selectedAttachment, setSelectedAttachment}) {
    const [hovered, setHovered] = useState(false)

    return <label style={{
            cursor: 'pointer',
            padding: '5px 10px',
            color: selectedAttachment === attachment ? 'white' : customTheme.palette.text.disabled,
            backgroundColor: hovered ? 'rgba(255, 255, 255, 0.07)' : 'unset'
        }}
        onClick={() => { setSelectedAttachment(attachment) }}
        onMouseOver={() => { setHovered(true) }}
        onMouseLeave={() => { setHovered(false) }}>{attachment.fileName}</label>

}