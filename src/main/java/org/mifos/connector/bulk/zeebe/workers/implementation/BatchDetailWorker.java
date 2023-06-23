package org.mifos.connector.bulk.zeebe.workers.implementation;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.connector.bulk.camel.routes.RouteId;
import org.mifos.connector.bulk.zeebe.workers.BaseWorker;
import org.mifos.connector.bulk.zeebe.workers.Worker;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static org.mifos.connector.bulk.zeebe.ZeebeVariables.*;

@Component
public class BatchDetailWorker extends BaseWorker {

    @Override
    public void setup() {

        // review comment: make the below method shorter
        newWorker(Worker.BATCH_DETAILS, (client, job) -> {
            Map<String, Object> variables = job.getVariablesAsMap();
            int pageNumber = (int) variables.getOrDefault(PAGE_NO, 1);
            int pageSize = (int) variables.getOrDefault(PAGE_SIZE, 5);
            int currentTransactionCount = (int) variables.getOrDefault(CURRENT_TRANSACTION_COUNT, 0);
            int completedTransactionCount = (int) variables.getOrDefault(COMPLETED_TRANSACTION_COUNT, 0);
            int failedTransactionCount = (int) variables.getOrDefault(FAILED_TRANSACTION_COUNT, 0);
            int ongoingTransactionCount = (int) variables.getOrDefault(ONGOING_TRANSACTION_COUNT, 0);

            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(BATCH_ID, variables.get(BATCH_ID));
            exchange.setProperty(PAGE_NO, pageNumber);
            exchange.setProperty(PAGE_SIZE, pageSize);
            exchange.setProperty(TENANT_ID, variables.get(TENANT_ID));
            exchange.setProperty(TOTAL_TRANSACTION, variables.get(TOTAL_TRANSACTION));
            exchange.setProperty(CURRENT_TRANSACTION_COUNT, currentTransactionCount);
            exchange.setProperty(COMPLETED_TRANSACTION_COUNT, completedTransactionCount);
            exchange.setProperty(FAILED_TRANSACTION_COUNT, failedTransactionCount);
            exchange.setProperty(ONGOING_TRANSACTION_COUNT, ongoingTransactionCount);
            exchange.setProperty(FILE_NAME, variables.get(FILE_NAME));
            exchange.setProperty(REQUEST_ID_STATUS_MAP, variables.getOrDefault(REQUEST_ID_STATUS_MAP, new HashMap<>()));

            sendToCamelRoute(RouteId.BATCH_DETAIL, exchange);

            boolean isReconciliationSuccess = exchange.getProperty(BATCH_DETAIL_SUCCESS, Boolean.class);

            if (!isReconciliationSuccess) {
                variables.put(ERROR_CODE, exchange.getProperty(ERROR_CODE));
                variables.put(ERROR_DESCRIPTION, exchange.getProperty(ERROR_DESCRIPTION));
            }
            else{
                exchange.setProperty(FILE_NAME, variables.get(FILE_NAME));
                sendToCamelRoute(RouteId.UPLOAD_RESULT_FILE, exchange);
            }

            variables.put(BATCH_DETAIL_SUCCESS, isReconciliationSuccess);
            variables.put(CURRENT_TRANSACTION_COUNT, exchange.getProperty(CURRENT_TRANSACTION_COUNT));
            variables.put(COMPLETED_TRANSACTION_COUNT, exchange.getProperty(COMPLETED_TRANSACTION_COUNT));
            variables.put(FAILED_TRANSACTION_COUNT, exchange.getProperty(FAILED_TRANSACTION_COUNT));
            variables.put(ONGOING_TRANSACTION_COUNT, exchange.getProperty(ONGOING_TRANSACTION_COUNT));
            variables.put(PAGE_NO, ++pageNumber);
            variables.put(REQUEST_ID_STATUS_MAP, exchange.getProperty(REQUEST_ID_STATUS_MAP));

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });

    }
}
