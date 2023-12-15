package org.mifos.connector.bulk.zeebe.workers.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.connector.bulk.camel.routes.RouteId;
import org.mifos.connector.bulk.config.MockPaymentSchemaConfig;
import org.mifos.connector.bulk.schema.BatchDTO;
import org.mifos.connector.bulk.schema.BatchDetailResponse;
import org.mifos.connector.bulk.zeebe.workers.BaseWorker;
import org.mifos.connector.bulk.zeebe.workers.Worker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.mifos.connector.bulk.zeebe.ZeebeVariables.*;
import static org.mifos.connector.bulk.zeebe.workers.Worker.BATCH_DETAILS;

@Component
public class BatchDetailWorker extends BaseWorker {
    @Autowired
    public MockPaymentSchemaConfig mockPaymentSchemaConfig;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setup() {
        logger.info("## generating " + BATCH_DETAILS + "zeebe worker");
        newWorker(BATCH_DETAILS, (client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();

            int pageNumber = (int) variables.getOrDefault(PAGE_NO, 1);
            int pageSize = (int) variables.getOrDefault(PAGE_SIZE, 5);
            int currentTransactionCount = (int) variables.getOrDefault(CURRENT_TRANSACTION_COUNT, 0);
            int completedTransactionCount = (int) variables.getOrDefault(COMPLETED_TRANSACTION_COUNT, 0);
            int failedTransactionCount = (int) variables.getOrDefault(FAILED_TRANSACTION_COUNT, 0);
            int ongoingTransactionCount = (int) variables.getOrDefault(ONGOING_TRANSACTION_COUNT, 0);
            //BatchDetailResponse batchDetailResponse =  callApi(variables.get(BATCH_ID).toString(), pageNumber, pageSize, variables.get(TENANT_ID).toString());

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

    public BatchDetailResponse callApi(String batchId, int pageNo, int pageSize, String tenant) {
        // Set up the RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Platform-TenantId", tenant);

        // Construct URL with query parameters
        String apiUrl = mockPaymentSchemaConfig.batchDetailUrl + "?"
                + BATCH_ID + "=" + batchId + "&"
                + PAGE_NO + "=" + pageNo + "&"
                + PAGE_SIZE + "=" + pageSize;

        // Construct request entity with headers
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        BatchDetailResponse batchDetailResponse = null;

        try {
            // Make the API call
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );
            batchDetailResponse = objectMapper.readValue(response.getBody(), BatchDetailResponse.class);

            // Log response
            logger.info("Batch detail API response: \n\n" + response.getBody());
        } catch (Exception e) {
            // Handle exceptions
            logger.warn("Exception occurred: " + e.getMessage());
        }
        return batchDetailResponse;
    }
}