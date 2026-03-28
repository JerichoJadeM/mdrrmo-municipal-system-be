package com.isufst.mdrrmosystem.exception;

public class ApprovalRequiredException extends RuntimeException {
    private final String requestType;
    private final String referenceType;
    private final Long referenceId;

    public ApprovalRequiredException(String message, String requestType, String referenceType, Long referenceId) {
        super(message);
        this.requestType = requestType;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    public String getRequestType() { return requestType; }
    public String getReferenceType() { return referenceType; }
    public Long getReferenceId() { return referenceId; }
}
