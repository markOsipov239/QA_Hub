import {useEffect, useRef} from "react";
import styles from "./LogRow.module.css";

export default function LogRow({ line, index, selectedLogRow, ...props }) {
  const ref = useRef(null);
  const isSelected = selectedLogRow === index

  useEffect(() => {
    if (isSelected) {
      ref.current?.scrollIntoView({ behavior: 'smooth', block: "center" });
    }
  }, [isSelected])

  return <div
    className={styles.row}
    data-line={index + 1}
    style={props.style}
    ref={ref}
  >
    <label className={`${styles.line} ${isSelected ? styles.lineSelected : ""}`}>{line}</label>
  </div>
}
