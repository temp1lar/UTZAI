package com.docstyler.backend.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProcessStatus {
    private String id;
    private String processId;
    private String status; // PROCESSING, COMPLETED, ERROR
    private Integer progress; // 0-100
    private String message;
    private String userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String resultFilePath;

    public ProcessStatus() {
        this.startTime = LocalDateTime.now();
        this.status = "PROCESSING";
        this.progress = 0;
    }

    public ProcessStatus(String processId, String userId) {
        this();
        this.processId = processId;
        this.userId = userId;
        this.message = "Документы приняты в обработку";
    }
}