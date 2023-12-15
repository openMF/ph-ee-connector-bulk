package org.mifos.connector.phee.utils;

public enum BillPayrEnum {
    SUCCESS_RESPONSE_CODE("00"),
    FAILED_RESPONSE_CODE("01"),
    SUCCESS_RESPONSE_MESSAGE("Request successfully received by Pay-BB"),
    FAILED_RESPONSE_MESSAGE("Request not acknowledged by Pay-BB");

    private final String value;


    BillPayrEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
