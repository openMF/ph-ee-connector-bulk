package org.mifos.connector.bulk.config;

import lombok.Getter;

@Getter
public enum PaymentModeType {

    PAYMENT("PAYMENT"),
    BULK("BULK");

    private String modeType;

    PaymentModeType(String modeType) {
        this.modeType = modeType;
    }

}
