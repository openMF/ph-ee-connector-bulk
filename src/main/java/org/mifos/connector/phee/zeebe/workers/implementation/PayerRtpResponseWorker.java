package org.mifos.connector.phee.zeebe.workers.implementation;

import org.mifos.connector.phee.data.PayerRTPResponse;
import org.mifos.connector.phee.data.ResponseDTO;
import org.mifos.connector.phee.zeebe.workers.BaseWorker;
import org.mifos.connector.phee.zeebe.workers.Worker;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.mifos.connector.phee.zeebe.ZeebeVariables.RTP_STATUS;

@Component
public class PayerRtpResponseWorker extends BaseWorker {
    @Override
    public void setup() {

        newWorker(Worker.PAYER_RTP_RESPONSE, (client, job) -> {
            Map<String, Object> variables = job.getVariablesAsMap();
            variables.put("billAccepted", true);
            variables.put(RTP_STATUS ,"00");
            RestTemplate restTemplate = new RestTemplate();
            String billId = (String) variables.get("billId");
            String transactionId = (String) variables.get("transactionId");
            String callbackUrl = (String) variables.get("payerCallbackUrl");
            String tenantId = (String) variables.get("payerTenantId");

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Platform-TenantId", tenantId);
            headers.set("X-Client-Correlation-ID", (String) variables.get("X-CorrelationID"));
            headers.set("X-Biller-Id", (String) variables.get("billerId"));
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
