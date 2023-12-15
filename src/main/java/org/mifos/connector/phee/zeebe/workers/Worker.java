package org.mifos.connector.phee.zeebe.workers;

public enum Worker {

    INIT_BATCH_TRANSFER("initBatchTransfer"),

    BATCH_SUMMARY("batchSummary"),

    BATCH_DETAILS("batchDetails");

    private final String value;

    private Worker(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
