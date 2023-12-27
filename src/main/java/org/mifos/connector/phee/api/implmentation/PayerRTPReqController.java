package org.mifos.connector.phee.api.implmentation;

import org.mifos.connector.phee.api.definition.PayerRtpReqApi;
import org.mifos.connector.phee.data.PayerRequestDTO;
import org.mifos.connector.phee.data.ResponseDTO;
import org.mifos.connector.phee.service.BillRTPReqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

 import static org.mifos.connector.phee.utils.BillPayrEnum.FAILED_RESPONSE;
 import static org.mifos.connector.phee.utils.BillPayrEnum.SUCCESS_RESPONSE;

@RestController
public class PayerRTPReqController implements PayerRtpReqApi {

    @Autowired
    private BillRTPReqService billRTPReqService;

    @Override
    public ResponseEntity<ResponseDTO> payerRtpReq(String tenantId, String correlationId,
                                                   String callbackUrl, String billerId, PayerRequestDTO payerRequestDTO)
            throws ExecutionException, InterruptedException {
            try {
                billRTPReqService.payerRtpReq(tenantId, correlationId,callbackUrl, billerId ,payerRequestDTO);


            } catch (Exception e) {
                ResponseDTO responseDTO = new ResponseDTO(FAILED_RESPONSE.getValue(), FAILED_RESPONSE.getValue(), correlationId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
            }
        ResponseDTO responseDTO = new ResponseDTO(SUCCESS_RESPONSE.getValue(), SUCCESS_RESPONSE.getValue(), correlationId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseDTO);
        }
    }
