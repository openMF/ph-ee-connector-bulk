package org.mifos.connector.phee.zeebe.workers.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
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

    @Value("${json_web_signature.privateKey}")
    private String privateKeyString;

    @Autowired
    private PaymentModeConfiguration paymentModeConfiguration;

    @Value("${tenant}")
    public String tenant;

    @Autowired
    private CsvMapper csvMapper;

    @Override
    public void setup() {
        logger.info("## generating " + INIT_BATCH_TRANSFER + "zeebe worker");
        newWorker(INIT_BATCH_TRANSFER, (client, job) ->{
            Map<String, Object> variables = job.getVariablesAsMap();
            String debulkingDfspId = variables.get(DEBULKINGDFSPID).toString();
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
            else{
                String updatedCsvData = updateCsvDataPaymentMode(csvData, filePath);
                String clientCorrelationId = String.valueOf(UUID.randomUUID());
                String batchId = invokeBatchTransactionApi(fileName, updatedCsvData, filePath, clientCorrelationId, debulkingDfspId);
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

    private String invokeBatchTransactionApi(String filename, String csvData, String filePath, String clientCorrelationId, String tenant) throws NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeySpecException, InvalidKeyException, KeyStoreException, KeyManagementException {

        String signature = generateSignature(clientCorrelationId, tenant, csvData, true, filePath);

        RestTemplate restTemplate = new RestTemplate();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

        //ResponseEntity<String> response = null;

        ContentDisposition contentDisposition = ContentDisposition
                .builder("form-data")
                .name("file")
                .filename(filename)
                .build();

        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        HttpEntity<byte[]> fileEntity = new HttpEntity<>(csvData.getBytes(), fileMap);
        //MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        //body.add("file", fileEntity);
        // HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String batchTransactionUrl = bulkProcessorContactPoint + batchTransactionEndpoint;
        String url = UriComponentsBuilder.fromHttpUrl(batchTransactionUrl)
                .queryParam("type", "csv").toUriString();
/*        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);
            logger.debug(response.toString());
        } catch (HttpClientErrorException e) {
            e.printStackTrace();
        }*/
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("purpose", "test payment");
        headers.set("filename", filename);
        headers.set("X-CorrelationID", clientCorrelationId);
        headers.set("Platform-TenantId", tenant);
        headers.set("X-SIGNATURE", signature);
        headers.set("Type", "csv");
        // headers.set("X-Registering-Institution-ID", "SocialWelfare");

        // Set the file data
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("data", Files.readString(Paths.get(filePath)));

        // Create the request entity
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);



        // Make the HTTP POST request
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class);


        String batchTransactionResponse = response != null ? response.getBody() : null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(batchTransactionResponse);
            return jsonNode.get("PollingPath").asText().split("/")[3];
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
            transaction.setBatchId(transactionFields[14]);
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


}