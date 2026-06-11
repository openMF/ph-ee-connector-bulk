package org.mifos.connector.phee.zeebe.workers.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.mifos.connector.common.util.JsonWebSignature;
import org.mifos.connector.phee.config.PaymentModeConfiguration;
import org.mifos.connector.phee.config.PaymentModeMapping;
import org.mifos.connector.phee.file.FileTransferService;
import org.mifos.connector.phee.schema.Transaction;
import org.mifos.connector.phee.schema.TransactionResult;
import org.mifos.connector.phee.utils.Utils;
import org.mifos.connector.phee.zeebe.workers.BaseWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mifos.connector.phee.zeebe.ZeebeVariables.*;
import static org.mifos.connector.phee.zeebe.workers.Worker.INIT_BATCH_TRANSFER;


@Component
public class BatchTransferWorker extends BaseWorker {

    @Autowired
    @Qualifier("awsStorage")
    private FileTransferService fileTransferService;

    @Value("${application.bucket-name}")
    private String bucketName;

    @Value("${config.completion-threshold-check.wait-timer}")
    private String waitTimer;

    @Value("${bulk-processor.contactpoint}")
    private String bulkProcessorContactPoint;

    @Value("${bulk-processor.endpoints.batch-transaction}")
    private String batchTransactionEndpoint;

    @Value("${bulk-processor.endpoints.batch-execution}")
    private String batchExecutionEndpoint;

    @Value("${channel.contactpoint}")
    private String channelContactPoint;

    @Value("${channel.endpoints.transfer}")
    private String channelTransferEndpoint;

    @Value("${json_web_signature.privateKey}")
    private String privateKeyString;

    @Autowired
    private PaymentModeConfiguration paymentModeConfiguration;

    @Autowired
    private org.mifos.connector.phee.config.MockPaymentSchemaConfig mockPaymentSchemaConfig;

    @Value("${tenant}")
    public String tenant;

    @Autowired
    private CsvMapper csvMapper;

    @Override
    public void setup() {
        logger.info("## generating " + INIT_BATCH_TRANSFER + "zeebe worker");
        logger.info("## Channel config - contactpoint: {}, endpoint: {}", channelContactPoint, channelTransferEndpoint);
        newWorker(INIT_BATCH_TRANSFER, (client, job) ->{
            Map<String, Object> variables = job.getVariablesAsMap();
            String debulkingDfspId = variables.get(DEBULKINGDFSPID).toString();
            String payeeDfspId = variables.get(PAYEE_DFSP_ID) != null ? variables.get(PAYEE_DFSP_ID).toString() : null;
            variables.put("waitTimer", waitTimer);

            String paymentMode = (String) variables.get(PAYMENT_MODE);
            String fileName = (String) variables.get(FILE_NAME);

            byte[] bytes = fileTransferService.downloadFileAsStream((String) variables.get(FILE_NAME), bucketName);
            String csvData = new String(bytes);
            List<Transaction> transactionList = parseCSVDataToList(csvData);
            String rootDirectory = System.getProperty("user.dir");

            // Create the file path using the root directory and file name
            String filePath = rootDirectory + File.separator + fileName;

            if(!isPaymentModeValid(paymentMode)){
                String serverFileName = (String) variables.get(FILE_NAME);
                String resultFile = String.format("Result_%s", serverFileName);
                uploadResultFileWithError(transactionList, resultFile);
                variables.put(INIT_BATCH_TRANSFER_SUCCESS, false);
            }
            else if("closedloop".equalsIgnoreCase(paymentMode)){
                logger.info("Processing closedloop batch transfer via channel connector");
                String batchId = (String) variables.get(BATCH_ID);
                int[] counts = processClosedloopTransfers(transactionList, batchId, debulkingDfspId);
                int successCount = counts[0];
                int failureCount = counts[1];
                long total = transactionList.size();

                variables.put(INIT_BATCH_TRANSFER_SUCCESS, failureCount == 0);

                // Pre-populate batch summary counts so BatchSummaryWorker can use them
                // if mock-payment-schema returns 0 (it is not informed of closedloop results)
                variables.put(TOTAL_TRANSACTION, total);
                variables.put(COMPLETED_TRANSACTION, (long) successCount);
                variables.put(FAILED_TRANSACTION, (long) failureCount);
                variables.put(ONGOING_TRANSACTION, 0L);

                double totalAmt = transactionList.stream()
                        .mapToDouble(t -> t.getAmount() != null ? Double.parseDouble(t.getAmount()) : 0.0)
                        .sum();
                double failedAmt = transactionList.stream()
                        .skip(successCount)
                        .mapToDouble(t -> t.getAmount() != null ? Double.parseDouble(t.getAmount()) : 0.0)
                        .sum();
                double completedAmt = totalAmt - failedAmt;
                variables.put(TOTAL_AMOUNT, totalAmt);
                variables.put(COMPLETED_AMOUNT, completedAmt);
                variables.put(FAILED_AMOUNT, failedAmt);
                variables.put(ONGOING_AMOUNT, 0.0);
                variables.put(COMPLETION_RATE, total > 0 ? (long)(((double)(successCount + failureCount) / total) * 100) : 0L);

                logger.info("Closedloop processing complete. Success: {}, Failed: {}, batchId: {}",
                        successCount, failureCount, batchId);

                // Register actual results with mock-payment-schema so its summary endpoint returns real data
                registerClosedloopSummary(batchId, debulkingDfspId, total, successCount, failureCount,
                        totalAmt, completedAmt, failedAmt);
            }
            else{
                String updatedCsvData = updateCsvDataPaymentMode(csvData, filePath);
                String clientCorrelationId = String.valueOf(UUID.randomUUID());
                String batchId = invokeBatchTransactionApi(fileName, updatedCsvData, filePath, clientCorrelationId, debulkingDfspId, payeeDfspId);
                logger.info("invokeBatchTransactionApi: {}", batchId);
                if(!ObjectUtils.isEmpty(batchId)){
                    variables.put(INIT_BATCH_TRANSFER_SUCCESS, true);
                    logger.info("Source batchId: {}", variables.get(BATCH_ID));
                    logger.info("Destination batchId: {}", batchId);
                }
            }
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });

    }

    private String updateCsvDataPaymentMode(String csvData, String filePath) {

        String[] lines = csvData.split("\n");
        StringBuilder updatedCsvData = new StringBuilder();
        updatedCsvData.append(lines[0]);
        updatedCsvData.append("\n");

        for(int i=1; i<lines.length; i++){
            String updatedTransaction = lines[i].replaceAll("closedloop", "mojaloop");
            updatedCsvData.append(updatedTransaction);
            if(i!= lines.length-1) {
                updatedCsvData.append("\n");
            }
        }

        try {
            // Write the updated CSV data to a file
            writeCsvToFile(String.valueOf(updatedCsvData), filePath);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        return updatedCsvData.toString();
    }

    private void writeCsvToFile(String csvData, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Files.write(path, csvData.getBytes());
    }

    private void uploadResultFileWithError(List<Transaction> transactionList, String resultFile) {
        List<TransactionResult> transactionResultList = updateTransactionStatusToFailed(transactionList);

        try {
            csvWriter(transactionResultList, TransactionResult.class, csvMapper, true, resultFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fileTransferService.uploadFile(new File(resultFile), bucketName);
    }

    private boolean isPaymentModeValid(String paymentMode) {
        PaymentModeMapping mapping = paymentModeConfiguration.getByMode(paymentMode);
        return mapping != null;
    }
    public String invokeBatchTransactionApi(String filename, String csvData, String filePath, String clientCorrelationId, String tenant, String payeeDfspId) throws Exception {
        String signature = generateSignature(clientCorrelationId, tenant, csvData, true, filePath);
        String batchTransactionUrl = bulkProcessorContactPoint + batchTransactionEndpoint;
        String url = UriComponentsBuilder.fromHttpUrl(batchTransactionUrl)
                .queryParam("type", "csv").toUriString();

        HttpEntity<MultiValueMap<String, Object>> requestEntity = createHttpEntity(filename, csvData, filePath, clientCorrelationId, tenant, payeeDfspId, signature);
        return executeBatchTransactionRequest(url, requestEntity);
    }


    private RestTemplate createRestTemplate() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        RestTemplate restTemplate = new RestTemplate();
        CloseableHttpClient httpClient = createHttpClient();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
        return restTemplate;
    }

    private CloseableHttpClient createHttpClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return HttpClients.custom()
                // HttpClient 5: TLS config moved onto the connection manager
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(new SSLConnectionSocketFactory(
                                new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build(),
                                NoopHostnameVerifier.INSTANCE))
                        .build())
                .build();
    }

    private HttpEntity<MultiValueMap<String, Object>> createHttpEntity(String filename, String csvData, String filePath, String clientCorrelationId, String tenant, String payeeDfspId, String signature) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("purpose", "test payment");
        headers.set("filename", filename);
        headers.set("X-CorrelationID", clientCorrelationId);
        headers.set("Platform-TenantId", tenant);
        headers.set("X-SIGNATURE", signature);
        headers.set("Type", "csv");
        if(payeeDfspId != null){
            headers.set("X-PayeeDFSP-ID", payeeDfspId);
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("data", Files.readString(Paths.get(filePath)));

        return new HttpEntity<>(body, headers);
    }

    private String executeBatchTransactionRequest(String url, HttpEntity<MultiValueMap<String, Object>> requestEntity) throws JsonProcessingException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        RestTemplate restTemplate = createRestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        String batchTransactionResponse = response != null ? response.getBody() : null;
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(batchTransactionResponse);
        return jsonNode.get("PollingPath").asText().split("/")[3];
    }

    public String getListAsCsvString(List<Transaction> list){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("id,request_id,payment_mode,payer_identifier_type,payer_identifier,payee_identifier_type,payee_identifier,amount,currency,note\n");
        for(Transaction transaction : list){
            stringBuilder.append(transaction.getId()).append(",")
                    .append(transaction.getRequestId()).append(",")
                    .append(transaction.getPaymentMode()).append(",")
                    .append(transaction.getPayerIdentifierType()).append(",")
                    .append(transaction.getPayerIdentifier()).append(",")
                    .append(transaction.getPayeeIdentifierType()).append(",")
                    .append(transaction.getPayeeIdentifier()).append(",")
                    .append(transaction.getAmount()).append(",")
                    .append(transaction.getCurrency()).append(",")
                    .append(transaction.getNote()).append("\n");
        }
        return stringBuilder.toString();
    }

    private List<Transaction> parseCSVDataToList(String csvData) {
        List<Transaction> transactionList = new ArrayList<>();
        String[] lines = csvData.split("\n");

        for(int i=1; i<lines.length; i++){
            String transactionString = lines[i];
            String[] transactionFields = transactionString.split(",");

            Transaction transaction = new Transaction();
            transaction.setId(Integer.parseInt(transactionFields[0]));
            transaction.setRequestId(transactionFields[1]);
            transaction.setPaymentMode(transactionFields[2]);
            transaction.setPayerIdentifierType(transactionFields[3]);
            transaction.setPayerIdentifier(transactionFields[4]);
            transaction.setPayeeIdentifierType(transactionFields[5]);
            transaction.setPayeeIdentifier(transactionFields[6]);
            transaction.setAmount(transactionFields[7]);
            transaction.setCurrency(transactionFields[8]);
            transaction.setNote(transactionFields[9]);
            // batchId is set from Zeebe variables, not from CSV
            transactionList.add(transaction);
        }
        return transactionList;
    }

    private List<TransactionResult> updateTransactionStatusToFailed(List<Transaction> transactionList) {
        List<TransactionResult> transactionResultList = new ArrayList<>();
        for (Transaction transaction : transactionList) {
            TransactionResult transactionResult = Utils.mapToResultDTO(transaction);
            transactionResult.setErrorCode("404");
            transactionResult.setErrorDescription("Payment mode not configured");
            transactionResult.setStatus("Failed");
            transactionResultList.add(transactionResult);
        }
        return transactionResultList;
    }

    private <T> void csvWriter(List<T> data, Class<T> tClass, CsvMapper csvMapper,
                               boolean overrideHeader, String filepath) throws IOException {
        CsvSchema csvSchema = csvMapper.schemaFor(tClass);
        if (overrideHeader) {
            csvSchema = csvSchema.withHeader();
        } else {
            csvSchema = csvSchema.withoutHeader();
        }
        File file = new File(filepath);
        SequenceWriter writer = csvMapper.writerWithSchemaFor(tClass).with(csvSchema).writeValues(file);
        for (T object: data) {
            writer.write(object);
        }
    }
    protected String generateSignature(String clientCorrelationId, String tenant, String filename, boolean isDataAFile, String filePath) throws IOException,
            NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeySpecException, InvalidKeyException {


        JsonWebSignature jsonWebSignature = new JsonWebSignature.JsonWebSignatureBuilder()
                .setClientCorrelationId(clientCorrelationId)
                .setTenantId(tenant)
                .setIsDataAsFile(isDataAFile)
                .setData(filePath)
                .build();

        return jsonWebSignature.getSignature(privateKeyString);
    }

    private void registerClosedloopSummary(String batchId, String tenant, long total, int successCount,
            int failureCount, double totalAmt, double completedAmt, double failedAmt) {
        try {
            String url = mockPaymentSchemaConfig.mockPaymentSchemaContactPoint + "/batches/" + batchId + "/summary";

            Map<String, Object> body = new HashMap<>();
            body.put("batchId", batchId);
            body.put("total", total);
            body.put("successful", (long) successCount);
            body.put("failed", (long) failureCount);
            body.put("ongoing", 0L);
            body.put("totalAmount", totalAmt);
            body.put("successfulAmount", completedAmt);
            body.put("failedAmount", failedAmt);
            body.put("pendingAmount", 0.0);
            body.put("status", failureCount == 0 ? "COMPLETED" : "PARTIALLY_COMPLETED");
            long successPct = total > 0 ? (long) (((double) successCount / total) * 100) : 0L;
            body.put("successPercentage", String.valueOf(successPct));
            body.put("failPercentage", String.valueOf(100 - successPct));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Platform-TenantId", tenant);

            ObjectMapper objectMapper = new ObjectMapper();
            HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            RestTemplate restTemplate = createRestTemplate();
            restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Void.class);
            logger.info("## CLOSEDLOOP - Registered summary with mock-payment-schema: batchId={}, total={}, success={}, failed={}",
                    batchId, total, successCount, failureCount);
        } catch (Exception e) {
            logger.warn("## CLOSEDLOOP - Failed to register summary with mock-payment-schema: {}", e.getMessage());
        }
    }

    private int[] processClosedloopTransfers(List<Transaction> transactionList, String batchId, String tenant) {
        logger.info("## CLOSEDLOOP - Processing {} transactions for batchId: {}", transactionList.size(), batchId);

        int successCount = 0;
        int failureCount = 0;

        for (Transaction transaction : transactionList) {
            try {
                logger.info("## CLOSEDLOOP - Processing transaction: {}, amount: {}",
                    transaction.getId(), transaction.getAmount());

                // Call channel connector for individual transfer
                boolean transferSuccess = invokeChannelTransfer(transaction, batchId, tenant);

                if (transferSuccess) {
                    successCount++;
                    logger.info("## CLOSEDLOOP - Transaction {} succeeded", transaction.getId());
                } else {
                    failureCount++;
                    logger.warn("## CLOSEDLOOP - Transaction {} failed", transaction.getId());
                }

                // Report execution status to bulk-processor
                reportExecutionStatus(transaction, batchId, tenant, transferSuccess);

            } catch (Exception e) {
                failureCount++;
                logger.error("## CLOSEDLOOP - Error processing transaction {}: {}",
                    transaction.getId(), e.getMessage(), e);
            }
        }

        logger.info("## CLOSEDLOOP - Batch {} complete. Success: {}, Failed: {}",
            batchId, successCount, failureCount);

        return new int[]{successCount, failureCount};
    }

    private boolean invokeChannelTransfer(Transaction transaction, String batchId, String tenant) {
        try {
            String transferUrl = channelContactPoint + channelTransferEndpoint;
            logger.info("## CLOSEDLOOP - Channel transfer URL: {}", transferUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Platform-TenantId", tenant);
            headers.set("X-BatchID", batchId);
            headers.set("X-CorrelationID", transaction.getRequestId());


            // Build transfer request body with proper MoneyData structure
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> requestPayload = new HashMap<>();

            // Create MoneyData object for amount
            Map<String, String> amountData = new HashMap<>();
            amountData.put("amount", transaction.getAmount());
            amountData.put("currency", transaction.getCurrency());

            requestPayload.put("amount", amountData);
            requestPayload.put("payer", createParty(transaction.getPayerIdentifierType(), transaction.getPayerIdentifier()));
            requestPayload.put("payee", createParty(transaction.getPayeeIdentifierType(), transaction.getPayeeIdentifier()));

            String requestBody = objectMapper.writeValueAsString(requestPayload);
            logger.info("## CLOSEDLOOP - Channel transfer request body: {}", requestBody);

            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            RestTemplate restTemplate = createRestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                transferUrl, HttpMethod.POST, requestEntity, String.class);

            logger.info("## CLOSEDLOOP - Channel transfer response status: {} for transaction: {}",
                response.getStatusCode(), transaction.getId());

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            logger.error("## CLOSEDLOOP - Channel transfer failed for transaction {}: {}",
                transaction.getId(), e.getMessage(), e);
            return false;
        }
    }

    private Map<String, Object> createParty(String idType, String idValue) {
        Map<String, String> partyIdInfo = new HashMap<>();
        partyIdInfo.put("partyIdType", idType);
        partyIdInfo.put("partyIdentifier", idValue);

        Map<String, Object> party = new HashMap<>();
        party.put("partyIdInfo", partyIdInfo);
        return party;
    }

    private void reportExecutionStatus(Transaction transaction, String batchId, String tenant, boolean success) {
        try {
            String executionUrl = bulkProcessorContactPoint + batchExecutionEndpoint;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Platform-TenantId", tenant);
            headers.set("X-CorrelationID", batchId);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("transactionId", transaction.getId());
            body.add("batchId", batchId);
            body.add("status", success ? "SUCCESS" : "FAILED");
            body.add("completedTimestamp", System.currentTimeMillis());
            body.add("amount", transaction.getAmount());
            body.add("currency", transaction.getCurrency());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = createRestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                executionUrl, HttpMethod.POST, requestEntity, String.class);

            logger.info("## CLOSEDLOOP - Execution status reported for transaction {}: {}",
                transaction.getId(), response.getStatusCode());

        } catch (Exception e) {
            logger.error("## CLOSEDLOOP - Failed to report execution status for transaction {}: {}",
                transaction.getId(), e.getMessage(), e);
        }
    }


}
