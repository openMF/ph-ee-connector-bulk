package org.mifos.connector.phee.service;

import org.mifos.connector.phee.data.BillRTPReqDTO;
import org.mifos.connector.phee.zeebe.ZeebeProcessStarter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static org.mifos.connector.phee.zeebe.ZeebeVariables.CLIENTCORRELATIONID;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.TENANT_ID;


@Service
public class BillRTPReqService {
    @Autowired
    private ZeebeProcessStarter zeebeProcessStarter;

    @Value("${bpmn.flows.bill-pay}")
    String billPayFlow;

    String transactionId;

    @Async("asyncExecutor")
    public String billRtpReq(String tenantId, String correlationId, String callBackUrl,
                             String billerId, BillRTPReqDTO body) {
        Map<String, Object> extraVariables = new HashMap<>();
        extraVariables.put(TENANT_ID, tenantId);
        extraVariables.put(CLIENTCORRELATIONID, correlationId);
        extraVariables.put("billId", body.getBillId());
        extraVariables.put("billerId", billerId);
        extraVariables.put("Callback URL", callBackUrl);
        extraVariables.put("BillRTPReqBody", body);
        //call payer FI
        // here adding a mock response of happy flow from payer FI

        return transactionId;
    }

}
