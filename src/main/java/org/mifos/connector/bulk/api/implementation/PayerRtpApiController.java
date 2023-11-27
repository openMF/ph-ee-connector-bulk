package org.mifos.connector.bulk.api.implementation;

import org.mifos.connector.bulk.api.definition.PayerRtpApi;
import org.mifos.connector.bulk.data.PayerRequestDTO;
import org.mifos.connector.bulk.data.ResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
public class PayerRtpApiController implements PayerRtpApi {
    @Override
    public ResponseEntity<ResponseDTO> payerRtpRequest(String callbackURL, String tenantId, String correlationId, String billerId, PayerRequestDTO requestBody) throws ExecutionException, InterruptedException {
        ResponseDTO responseDTO = new ResponseDTO("00", "Request Successfully received by Payer FI", requestBody.getRequestId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseDTO);
    }
}
