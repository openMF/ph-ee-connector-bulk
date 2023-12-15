package org.mifos.connector.phee.camel.routes;

public enum RouteId {
    INIT_BATCH_TRANSFER("direct:init-batch-transfer"),

    BATCH_SUMMARY("direct:batch-summary"),

    BATCH_DETAIL("direct:batch-detail"),

    UPLOAD_RESULT_FILE("direct:upload-result-file");


    private final String value;


    private RouteId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
