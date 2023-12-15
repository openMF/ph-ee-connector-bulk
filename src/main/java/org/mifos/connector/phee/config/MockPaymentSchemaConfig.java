package org.mifos.connector.phee.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class MockPaymentSchemaConfig {

    @Value("${mock-payment-schema.contactpoint}")
    public String mockPaymentSchemaContactPoint;

    @Value("${mock-payment-schema.endpoints.batch-summary}")
    public String mockBatchSummaryEndpoint;

    @Value("${mock-payment-schema.endpoints.batch-detail}")
    public String mockBatchDetailEndpoint;

    public String batchSummaryUrl;

    public String batchDetailUrl;


    @PostConstruct
    private void setup() {
        batchSummaryUrl = mockPaymentSchemaContactPoint + mockBatchSummaryEndpoint;
        batchDetailUrl = mockPaymentSchemaContactPoint + mockBatchDetailEndpoint;
    }
}
