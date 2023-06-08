package org.mifos.connector.bulk.cucumber.stepdef;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.mifos.connector.bulk.config.PaymentModeConfiguration;
import org.mifos.connector.bulk.config.PaymentModeMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

// this class is the base for all the cucumber step definitions
public class BaseStepDef {

    @Autowired
    ProducerTemplate template;

    @Autowired
    CamelContext context;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PaymentModeConfiguration paymentModeConfiguration;

//    @Autowired
//    ExternalApiPayloadConfig externalApiPayloadConfig;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static String tenant;
    protected static String paymentMode;
    protected static PaymentModeMapping paymentModeMapping;

    protected static Exchange exchange;

}
