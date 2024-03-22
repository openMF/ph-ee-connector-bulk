package org.mifos.connector.phee.zeebe.workers.implementation;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.mifos.connector.phee.data.PayerRTPResponse;
import org.mifos.connector.phee.data.ResponseDTO;
import org.mifos.connector.phee.zeebe.workers.BaseWorker;
import org.mifos.connector.phee.zeebe.workers.Worker;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.mifos.connector.phee.zeebe.ZeebeVariables.HEADER_BILLER_ID;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.HEADER_CLIENTCORRELATIONID;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.HEADER_TENANT;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.RTP_STATUS;

@Component
public class PayerRtpResponseWorker extends BaseWorker {
    @Override
    public void setup() {

        newWorker(Worker.PAYER_RTP_RESPONSE, (client, job) -> {
            Map<String, Object> variables = job.getVariablesAsMap();
            variables.put("billAccepted", true);
            variables.put("state", "IN PROGRESS");
            variables.put(RTP_STATUS ,"00");
            RestTemplate restTemplate = new RestTemplate();
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build())
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
            String billId = (String) variables.get("billId");
            String transactionId = (String) variables.get("transactionId");
            String callbackUrl = (String) variables.get("payerCallbackUrl");
            String tenantId = (String) variables.get("payerTenantId");

            HttpHeaders headers = new HttpHeaders();
            headers.set(HEADER_TENANT, tenantId);
            headers.set(HEADER_CLIENTCORRELATIONID, (String) variables.get("X-CorrelationID"));
            headers.set(HEADER_BILLER_ID, (String) variables.get("billerId"));
            headers.setContentType(MediaType.APPLICATION_JSON);
            PayerRTPResponse payerRTPResponse = createMockBody(billId, transactionId);


            HttpEntity<PayerRTPResponse> request = new HttpEntity<>(payerRTPResponse, headers);


            ResponseEntity<ResponseDTO> response = restTemplate.exchange(
                    callbackUrl,
                    HttpMethod.PUT,
                    request,
                    ResponseDTO.class
            );
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }
        public PayerRTPResponse createMockBody(String billId, String transactionId){
            PayerRTPResponse payerRTPResponse =  new PayerRTPResponse();
            payerRTPResponse.setBillId(billId);
            payerRTPResponse.setTxnId(transactionId);
            payerRTPResponse.setRtpStatus("00");
            payerRTPResponse.setRejectReason("");
            return payerRTPResponse;

        }

}
