package com.edstem.interviewprep.common.exception;

public record ErrorResponse(int status, String errorCode, String message) {}
