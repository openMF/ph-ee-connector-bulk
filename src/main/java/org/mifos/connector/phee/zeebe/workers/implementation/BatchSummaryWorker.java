package org.mifos.connector.phee.zeebe.workers.implementation;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.connector.phee.camel.routes.RouteId;
import org.mifos.connector.phee.zeebe.workers.BaseWorker;
import org.mifos.connector.phee.zeebe.workers.Worker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.mifos.connector.phee.zeebe.ZeebeVariables.*;

@Component
public class BatchSummaryWorker extends BaseWorker {

    @Value("${config.completion-threshold-check.max-retry-count}")
    public int maxRetryCount;

    @Override
    public void setup() {
        newWorker(Worker.BATCH_SUMMARY, (client, job)->{
            Map<String, Object> variables = job.getVariablesAsMap();
            int currentRetryCount = (int) variables.getOrDefault(CURRENT_RETRY_COUNT, 1);

            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(BATCH_ID, variables.get(BATCH_ID));
            exchange.setProperty(TENANT_ID, variables.get(TENANT_ID));

            sendToCamelRoute(RouteId.BATCH_SUMMARY, exchange);

            boolean isBatchSummarySuccess = (boolean) exchange.getProperty(BATCH_SUMMARY_SUCCESS);

            variables.put(MAX_RETRY_COUNT, maxRetryCount);
            variables.put(CURRENT_RETRY_COUNT, ++currentRetryCount);
            variables.put(ONGOING_TRANSACTION, exchange.getProperty(ONGOING_TRANSACTION));
            variables.put(FAILED_TRANSACTION, exchange.getProperty(FAILED_TRANSACTION));
            variables.put(TOTAL_TRANSACTION, exchange.getProperty(TOTAL_TRANSACTION));
            variables.put(COMPLETED_TRANSACTION, exchange.getProperty(COMPLETED_TRANSACTION));
            variables.put(ONGOING_AMOUNT, exchange.getProperty(ONGOING_AMOUNT));
            variables.put(FAILED_AMOUNT, exchange.getProperty(FAILED_AMOUNT));
            variables.put(COMPLETED_AMOUNT, exchange.getProperty(COMPLETED_AMOUNT));
            variables.put(TOTAL_AMOUNT, exchange.getProperty(TOTAL_AMOUNT));
            variables.put(COMPLETION_RATE, exchange.getProperty(COMPLETION_RATE));

            variables.put(BATCH_SUMMARY_SUCCESS, isBatchSummarySuccess);

            if(!isBatchSummarySuccess){
                variables.put(ERROR_CODE, exchange.getProperty(ERROR_CODE));
                variables.put(ERROR_DESCRIPTION, exchange.getProperty(ERROR_DESCRIPTION));
                logger.info("Error: {}, {}", variables.get(ERROR_CODE), variables.get(ERROR_DESCRIPTION));
            }

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }
}
