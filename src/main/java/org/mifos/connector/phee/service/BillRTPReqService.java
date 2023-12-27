package org.mifos.connector.phee.service;

import org.mifos.connector.phee.data.BillRTPReqDTO;
import org.mifos.connector.phee.data.PayerRTPResponse;
import org.mifos.connector.phee.data.PayerRequestDTO;
import org.mifos.connector.phee.data.ResponseDTO;
import org.mifos.connector.phee.zeebe.ZeebeProcessStarter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.mifos.connector.phee.zeebe.ZeebeVariables.CLIENTCORRELATIONID;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.TENANT_ID;


@Service
public class BillRTPReqService {
    @Autowired
    private ZeebeProcessStarter zeebeProcessStarter;

    String transactionId;

    @Async("asyncExecutor")
    public String payerRtpReq(String tenantId, String correlationId, String callBackUrl,
                             String billerId, PayerRequestDTO body) {
        Map<String, Object> extraVariables = new HashMap<>();
        extraVariables.put(TENANT_ID, tenantId);
        extraVariables.put(CLIENTCORRELATIONID, correlationId);
        extraVariables.put("billId", body.getBillDetails().getBillId());
        extraVariables.put("billerName", body.getBillDetails().getBillerName());
        extraVariables.put("amount", body.getBillDetails().getAmount());
        extraVariables.put("billerId", billerId);
        extraVariables.put("BillRTPReqBody", body);
        extraVariables.put("rtpStatus", "00");
        extraVariables.put("rejectReason", "");
        extraVariables.put("transactionId", body.getTransactionId());

        //call payer FI
        // here adding a mock response of happy flow from payer FI
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Platform-TenantId", tenantId);
        headers.set("X-Client-Correlation-ID", correlationId);
        headers.set("X-Biller-Id", billerId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        PayerRTPResponse payerRTPResponse =  createMockBody(body);


        HttpEntity<PayerRTPResponse> request = new HttpEntity<>(payerRTPResponse, headers);


        ResponseEntity<ResponseDTO> response = restTemplate.exchange(
                callBackUrl,
                HttpMethod.PUT,
                request,
                ResponseDTO.class
        );



        return transactionId;
    }
    public PayerRTPResponse createMockBody(PayerRequestDTO body){
        PayerRTPResponse payerRTPResponse =  new PayerRTPResponse();
        payerRTPResponse.setBillId(body.getBillDetails().getBillId());
        payerRTPResponse.setTxnId(body.getTransactionId());
        payerRTPResponse.setRtpStatus("00");
        payerRTPResponse.setRejectReason("");
        return payerRTPResponse;

    }

}
