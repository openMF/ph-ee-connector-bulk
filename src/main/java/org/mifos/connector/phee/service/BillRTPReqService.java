package org.mifos.connector.phee.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.mifos.connector.common.channel.dto.TransactionChannelRequestDTO;
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
    @Autowired
    private ObjectMapper objectMapper;

    String transactionId;

    public String payerRtpReq(String tenantId, String correlationId, String callBackUrl,
                             String billerId, String requestBody) throws JsonProcessingException {
        //PayerRequestDTO body = objectMapper.readValue(requestBody, PayerRequestDTO.class);
        JSONObject json = new JSONObject(requestBody);
        JSONObject billDetailsJson = json.getJSONObject("billDetails");
        Map<String, Object> extraVariables = new HashMap<>();
        String billId = (String) billDetailsJson.get("billId");
        String transactionId = (String) json.get("transactionId");
        extraVariables.put(TENANT_ID, tenantId);
        extraVariables.put(CLIENTCORRELATIONID, correlationId);
        extraVariables.put("billId", billDetailsJson.get("billId"));
        extraVariables.put("billerName", billDetailsJson.get("billerName"));
        extraVariables.put("amount", billDetailsJson.get("amount"));
        extraVariables.put("billerId", billerId);
        extraVariables.put("BillRTPReqBody", requestBody);
        extraVariables.put("rtpStatus", "00");
        extraVariables.put("rejectReason", "");
        extraVariables.put("transactionId", json.get("transactionId"));

        //call payer FI
        // here adding a mock response of happy flow from payer FI
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Platform-TenantId", tenantId);
        headers.set("X-Client-Correlation-ID", correlationId);
        headers.set("X-Biller-Id", billerId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        PayerRTPResponse payerRTPResponse =  createMockBody(billId, transactionId);


        HttpEntity<PayerRTPResponse> request = new HttpEntity<>(payerRTPResponse, headers);


        ResponseEntity<ResponseDTO> response = restTemplate.exchange(
                callBackUrl,
                HttpMethod.PUT,
                request,
                ResponseDTO.class
        );



        return transactionId;
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
