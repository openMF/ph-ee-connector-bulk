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

        return transactionId;
    }


}
