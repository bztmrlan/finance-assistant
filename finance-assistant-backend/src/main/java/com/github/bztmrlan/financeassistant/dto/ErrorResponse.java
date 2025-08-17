package com.github.bztmrlan.financeassistant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;


@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private String errorCode;
    private String message;
    private String details;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String path;
    private String method;
    
    private int httpStatus;
    
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String> fieldErrors;
    
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> globalErrors;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String traceId;
} 