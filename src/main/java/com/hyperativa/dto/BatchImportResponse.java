package com.hyperativa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchImportResponse {
    private int totalRecords;
    private int successCount;
    private int errorCount;
    private String message;
}
