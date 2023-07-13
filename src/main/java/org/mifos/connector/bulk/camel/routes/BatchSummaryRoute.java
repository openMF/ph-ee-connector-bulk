package org.mifos.connector.bulk.camel.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.bulk.config.MockPaymentSchemaConfig;
import org.mifos.connector.bulk.schema.BatchDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static org.mifos.connector.bulk.zeebe.ZeebeVariables.*;

@Component
public class BatchSummaryRoute extends BaseRouteBuilder {

    @Value("${config.completion-threshold-check.completion-threshold}")
    private int completionThreshold;

    @Autowired
    public MockPaymentSchemaConfig mockPaymentSchemaConfig;

    @Value("${tenant}")
    public String tenant;

    @Override
    public void configure() throws Exception {

        from(RouteId.BATCH_SUMMARY.getValue())
                .id(RouteId.BATCH_SUMMARY.getValue())
                .log("Starting route " + RouteId.BATCH_SUMMARY.name())
                .to("direct:batch-summary-api-call")
                .to("direct:batch-summary-response-handler");


        getBaseExternalApiRequestRouteDefinition("batch-summary-api-call", HttpRequestMethod.GET)
                .setHeader(Exchange.REST_HTTP_QUERY, simple("batchId=${exchangeProperty." + BATCH_ID + "}"))
                .setHeader("Platform-TenantId", simple(tenant))
                .process(exchange -> {
                    logger.info(exchange.getIn().getHeaders().toString());
                })
                .toD(mockPaymentSchemaConfig.batchSummaryUrl + "?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log(LoggingLevel.INFO, "Batch summary API response: \n\n ${body}");

        from("direct:batch-summary-response-handler")
                .id("direct:batch-summary-response-handler")
                .log("Starting route direct:batch-summary-response-handler")
                //.setBody(exchange -> exchange.getIn().getBody(String.class))
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .log(LoggingLevel.INFO, "Batch summary request successful")
                .unmarshal().json(JsonLibrary.Jackson, BatchDTO.class)
                .process(exchange -> {
                    BatchDTO batchSummary = exchange.getIn().getBody(BatchDTO.class);

                    long failedTransfersCount = batchSummary.getFailed();
                    long ongoingTransfersCount = batchSummary.getOngoing();
                    long totalTransfersCount = batchSummary.getTotal();
                    long successfulTransfersCount = batchSummary.getSuccessful();
                    BigDecimal failedAmount = batchSummary.getFailedAmount();
                    BigDecimal pendingAmount = batchSummary.getPendingAmount();
                    BigDecimal successfulAmount = batchSummary.getSuccessfulAmount();
                    BigDecimal totalAmount = batchSummary.getTotalAmount();

                    long percentage = (long)(((double)
                            (batchSummary.getSuccessful() + batchSummary.getFailed())/batchSummary.getTotal()) *100);

                    exchange.setProperty(COMPLETION_RATE, percentage);
                    exchange.setProperty(ONGOING_TRANSACTION, ongoingTransfersCount);
                    exchange.setProperty(FAILED_TRANSACTION, failedTransfersCount);
                    exchange.setProperty(COMPLETED_TRANSACTION, successfulTransfersCount);
                    exchange.setProperty(TOTAL_TRANSACTION, totalTransfersCount);
                    exchange.setProperty(ONGOING_AMOUNT, pendingAmount);
                    exchange.setProperty(FAILED_AMOUNT, failedAmount);
                    exchange.setProperty(COMPLETED_AMOUNT, successfulAmount);
                    exchange.setProperty(TOTAL_AMOUNT, totalAmount);

                    if (percentage >= completionThreshold) {
                        exchange.setProperty(BATCH_SUMMARY_SUCCESS, true);
                        logger.info("Batch success threshold reached. Expected rate: {}, Actual Rate: {}",
                                completionThreshold, percentage);
                    }
                    else{
                        exchange.setProperty(BATCH_SUMMARY_SUCCESS, false);
                    }

                })
                .otherwise()
                .log(LoggingLevel.ERROR, "Batch summary request unsuccessful")
                .process(exchange -> {
                    exchange.setProperty(BATCH_SUMMARY_SUCCESS, false);
                    exchange.setProperty(ERROR_DESCRIPTION, exchange.getIn().getBody(String.class));
                    exchange.setProperty(ERROR_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
                });

    }
}
