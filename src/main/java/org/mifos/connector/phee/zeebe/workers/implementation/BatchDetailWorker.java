package org.mifos.connector.phee.zeebe.workers.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.connector.phee.camel.routes.RouteId;
import org.mifos.connector.phee.config.MockPaymentSchemaConfig;
import org.mifos.connector.phee.schema.BatchDetailResponse;
import org.mifos.connector.phee.schema.Transaction;
import org.mifos.connector.phee.schema.TransactionResult;
import org.mifos.connector.phee.zeebe.workers.BaseWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mifos.connector.phee.zeebe.ZeebeVariables.BATCH_DETAIL_SUCCESS;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.COMPLETED_TRANSACTION_COUNT;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.CURRENT_TRANSACTION_COUNT;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.ERROR_CODE;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.ERROR_DESCRIPTION;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.FAILED_TRANSACTION_COUNT;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.ONGOING_TRANSACTION_COUNT;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.PAGE_NO;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.PAGE_SIZE;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.REQUEST_ID_STATUS_MAP;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.TENANT_ID;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.TOTAL_TRANSACTION;
import static org.mifos.connector.phee.zeebe.workers.Worker.BATCH_DETAILS;


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
            //BatchDetailResponse batchDetailResponse = callApi((String) variables.get(BATCH_ID), 1, 10, (String) variables.get(TENANT_ID));

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

    public void processBatchDetail(String batchId, int pageNo, int pageSize) {
        String apiResponse = callBatchDetailApi(batchId, pageNo, pageSize);
        if (apiResponse != null) {
            handleBatchDetailApiResponse(apiResponse, pageNo, pageSize);
            uploadResultFile();
        } else {
            // Handle API call failure
        }
    }

    private String callBatchDetailApi(String batchId, int pageNo, int pageSize) {
        // Make API call and return the response as a String
        // Replace Camel code with your API call logic here
        return null; // Placeholder for API response (replace with actual logic)
    }
    private void handleBatchDetailApiResponse(String apiResponse, int pageNo, int pageSize) {
        try {
            BatchDetailResponse batchDetailResponse = objectMapper.readValue(apiResponse, BatchDetailResponse.class);
            // Process batch details and update properties accordingly
            // Logic from direct:batch-detail-response-handler can be moved here
            // I'll create a dummy implementation for demonstration purposes
            int currentTransferCount = 10; // Replace with your logic
            int totalTransferCount = 100; // Replace with your logic

            if (currentTransferCount >= totalTransferCount) {
                // Set property for successful batch detail
            } else {
                // Set property for unsuccessful batch detail
            }
        } catch (Exception e) {
            // Handle exception during API response handling
        }
    }
    private void uploadResultFile() {
        // Logic for uploading result file
        // Logic from direct:upload-result-file can be moved here
        // I'll create a dummy implementation for demonstration purposes
        boolean batchDetailSuccess = true; // Replace with your logic
        if (batchDetailSuccess) {
            downloadFile();
            getTransactionArray();
            String serverFileName = "ServerFile.txt"; // Replace with actual file name
            String batchId = "12345"; // Replace with actual batch ID
            String resultFile = String.format("Result_%s", serverFileName);
            List<Transaction> transactionList = new ArrayList<>(); // Replace with your list of transactions
            Map<String, String> requestIdStatusMap = new HashMap<>(); // Replace with your map
            List<TransactionResult> transactionResultList = fetchTransactionResult(transactionList, requestIdStatusMap, batchId);
            updateResultFile(resultFile, transactionResultList);
            uploadFile(resultFile);
        } else {
            // Handle unsuccessful batch detail
        }
    }
    private void downloadFile() {
        // Logic for downloading file
    }

    private void getTransactionArray() {
        // Logic for getting transaction array
    }

    private List<TransactionResult> fetchTransactionResult(List<Transaction> transactionList, Map<String, String> requestIdStatusMap, String batchId) {
        List<TransactionResult> transactionResultList = new ArrayList<>();
        // Logic for fetching transaction result
        return transactionResultList;
    }

    private void updateResultFile(String resultFile, List<TransactionResult> transactionResultList) {
        // Logic for updating result file
    }

    private void uploadFile(String resultFile) {
        // Logic for uploading file
    }

}
