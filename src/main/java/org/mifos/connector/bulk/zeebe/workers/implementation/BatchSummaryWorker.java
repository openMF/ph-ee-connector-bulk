package org.mifos.connector.bulk.zeebe.workers.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.mifos.connector.bulk.camel.routes.RouteId;
import org.mifos.connector.bulk.config.MockPaymentSchemaConfig;
import org.mifos.connector.bulk.schema.BatchDTO;
import org.mifos.connector.bulk.zeebe.workers.BaseWorker;
import org.mifos.connector.bulk.zeebe.workers.Worker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static org.mifos.connector.bulk.zeebe.ZeebeVariables.*;
import static org.mifos.connector.bulk.zeebe.workers.Worker.BATCH_DETAILS;
import static org.mifos.connector.bulk.zeebe.workers.Worker.BATCH_SUMMARY;

@Component
public class BatchSummaryWorker extends BaseWorker {

    @Value("${config.completion-threshold-check.max-retry-count}")
    public int maxRetryCount;
    @Autowired
    public MockPaymentSchemaConfig mockPaymentSchemaConfig;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setup() {
        logger.info("## generating " + BATCH_SUMMARY + "zeebe worker");
        newWorker(BATCH_SUMMARY, (client, job)->{
            Map<String, Object> variables = job.getVariablesAsMap();
            int currentRetryCount = (int) variables.getOrDefault(CURRENT_RETRY_COUNT, 1);

            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(BATCH_ID, variables.get(BATCH_ID));
            exchange.setProperty(TENANT_ID, variables.get(TENANT_ID));
            //BatchDTO batchDTO =  new BatchDTO();
            BatchDTO batchDTO = callApi(variables.get(BATCH_ID).toString(), variables.get(TENANT_ID).toString());

            //sendToCamelRoute(RouteId.BATCH_SUMMARY, exchange);

            // boolean isBatchSummarySuccess = (boolean) exchange.getProperty(BATCH_SUMMARY_SUCCESS);

            variables.put(MAX_RETRY_COUNT, maxRetryCount);
            variables.put(CURRENT_RETRY_COUNT, ++currentRetryCount);
            variables.put(ONGOING_TRANSACTION, batchDTO.getOngoing());
            variables.put(FAILED_TRANSACTION, batchDTO.getFailed());
            variables.put(TOTAL_TRANSACTION, batchDTO.getTotal());
            variables.put(COMPLETED_TRANSACTION, batchDTO.getSuccessful());
            variables.put(ONGOING_AMOUNT, batchDTO.getPendingAmount());
            variables.put(FAILED_AMOUNT, batchDTO.getFailedAmount());
            variables.put(COMPLETED_AMOUNT, batchDTO.getSuccessfulAmount());
            variables.put(TOTAL_AMOUNT, batchDTO.getTotalAmount());
            long percentage = (long)(((double)
                    (batchDTO.getSuccessful() + batchDTO.getFailed())/batchDTO.getTotal()) *100);
            variables.put(COMPLETION_RATE, percentage);

            if(batchDTO!=null) {

                variables.put(BATCH_SUMMARY_SUCCESS, true);
            } else {
                variables.put(ERROR_CODE, exchange.getProperty(ERROR_CODE));
                variables.put(ERROR_DESCRIPTION, exchange.getProperty(ERROR_DESCRIPTION));
                logger.info("Error: {}, {}", variables.get(ERROR_CODE), variables.get(ERROR_DESCRIPTION));
            }

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

    public BatchDTO callApi(String batchId, String tenant) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        RestTemplate restTemplate = new RestTemplate();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));


        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Platform-TenantId", tenant);

        // Construct URL
        String apiUrl = mockPaymentSchemaConfig.mockPaymentSchemaContactPoint + "/batches/" + batchId + "/summary";

        // Construct request entity with headers
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        BatchDTO batchDTO =  null;

        try {
            // Make the API call
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, requestEntity, String.class);
            batchDTO = objectMapper.readValue(response.getBody(), BatchDTO.class);
            // Log response
            logger.info("Batch summary API response: \n\n" + response.getBody());
            return batchDTO;

        } catch (Exception e) {
            // Handle exceptions
            logger.warn("Exception occurred: " + e.getMessage());
        }
        return batchDTO;
    }
}