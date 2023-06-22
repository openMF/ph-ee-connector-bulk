package org.mifos.connector.bulk.zeebe.workers.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.mifos.connector.bulk.camel.routes.RouteId;
import org.mifos.connector.bulk.schema.Transaction;
import org.mifos.connector.bulk.zeebe.workers.BaseWorker;
import org.mifos.connector.bulk.zeebe.workers.Worker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.mifos.connector.bulk.zeebe.ZeebeVariables.*;

@Component
public class BatchTransferWorker extends BaseWorker {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Value("${config.completion-threshold-check.wait-timer}")
    private String waitTimer;

    @Override
    public void setup() {

        newWorker(Worker.INIT_BATCH_TRANSFER, (client, job) ->{
            Map<String, Object> variables = job.getVariablesAsMap();
            variables.put("waitTimer", waitTimer);

            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(FILE_NAME, variables.get(FILE_NAME));
            exchange.setProperty(TENANT_ID, variables.get(TENANT_ID));
            exchange.setProperty(BATCH_ID, variables.get(BATCH_ID));
            exchange.setProperty(REQUEST_ID, variables.get(REQUEST_ID));
            exchange.setProperty(PURPOSE, variables.get(PURPOSE));
            logger.info("Source batchId: " + variables.get(BATCH_ID));

            sendToCamelRoute(RouteId.INIT_BATCH_TRANSFER, exchange);

            String filename = (String) variables.get(FILE_NAME);
            List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
            String csvData = getListAsCsvString(transactionList);
            logger.info("Print CSV Data: " + csvData);
            String batchId = invokeBatchTransactionApi(filename, csvData);
            variables.put(BATCH_ID, batchId);
            logger.info("Destination batchId: " + batchId);

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });

    }
    
    private String invokeBatchTransactionApi(String filename, String csvData){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = null;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Purpose", "test purpose");
        headers.add("filename", filename);

        // review comment: review hard coding of rhino
        headers.add("Platform-TenantId", "rhino");
        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();

        // review comment: review hard coding of below parameters
        ContentDisposition contentDisposition = ContentDisposition
                .builder("form-data")
                .name("file")
                .filename("test.csv")
                .build();
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        HttpEntity<byte[]> fileEntity = new HttpEntity<>(csvData.getBytes(), fileMap);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileEntity);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        try {
            response = restTemplate.exchange(
                    // when adding correct url. add query param type=csv
                    "http://localhost:5002/batchtransactions?type=csv",
                    HttpMethod.POST,
                    requestEntity,
                    String.class);
            logger.debug(response.toString());
        } catch (HttpClientErrorException e) {
            e.printStackTrace();
        }

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
}
