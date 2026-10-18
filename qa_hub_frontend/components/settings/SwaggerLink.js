import process from "../../next.config";
import {customTheme} from "../../styles/CustomTheme";

export default function SwaggerLink() {
    const backendUrl = (process.env.NEXT_PUBLIC_QA_HUB_BACKEND || "").replace(/\/$/, "")
    const swaggerUrl = `${backendUrl}/swagger-ui.html`

    return <a
        href={swaggerUrl}
        target="_blank"
        rel="noopener noreferrer"
        style={{
            position: "absolute",
            left: "16px",
            bottom: "12px",
            fontSize: "12px",
            lineHeight: 1.5,
            color: customTheme.palette.text.faded,
            textDecoration: "none",
            zIndex: 1
        }}
    >
        Swagger
    </a>
}
