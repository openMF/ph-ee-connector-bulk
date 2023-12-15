package org.mifos.connector.bulk.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentModeMapping {

    private String id, endpoint;
    private PaymentModeType type;
}
