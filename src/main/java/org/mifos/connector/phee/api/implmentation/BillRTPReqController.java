package org.mifos.connector.phee.api.implmentation;

import org.mifos.connector.phee.api.definition.BillRtpReqApi;
import org.mifos.connector.phee.api.definition.BillRtpReqApi;
import org.mifos.connector.phee.data.BillRTPReqDTO;
import org.mifos.connector.phee.data.ResponseDTO;
import org.mifos.connector.phee.service.BillRTPReqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

import static org.mifos.connector.phee.utils.BillPayrEnum.*;

@RestController
public class BillRTPReqController implements BillRtpReqApi {

    @Autowired
    private BillRTPReqService billRTPReqService;

    @Override
    public ResponseEntity<ResponseDTO> billRTPReq(String tenantId, String correlationId,
                                                  String callbackUrl, String billerId, BillRTPReqDTO billRTPReqDTO)
            throws ExecutionException, InterruptedException {
            try {
                billRTPReqService.billRtpReq(tenantId, correlationId,callbackUrl, billerId ,billRTPReqDTO);

            } catch (Exception e) {
                ResponseDTO responseDTO = new ResponseDTO(FAILED_RESPONSE_CODE.getValue(), FAILED_RESPONSE_MESSAGE.getValue(), correlationId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
            }
        ResponseDTO responseDTO = new ResponseDTO(SUCCESS_RESPONSE_CODE.getValue(), SUCCESS_RESPONSE_MESSAGE.getValue(), correlationId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseDTO);
        }
    }
