package com.edstem.interviewprep.common.dto.response;

public record ErrorResponse(int status, String errorCode, String message) {}
