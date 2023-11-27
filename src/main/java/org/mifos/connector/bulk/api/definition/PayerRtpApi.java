package org.mifos.connector.bulk.api.definition;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.mifos.connector.bulk.data.PayerRequestDTO;
import org.mifos.connector.bulk.data.ResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.ExecutionException;

@Tag(name = "GOV")
public interface PayerRtpApi {
    @PostMapping("/beneficiary")
    ResponseEntity<ResponseDTO> payerRtpRequest(@RequestHeader(value = "X-CallbackURL") String callbackURL, @RequestHeader(value = "X-Platform-TenantId") String tenantId,
                                                @RequestHeader(value = "X-CorrelationID") String correlationId,
                                                @RequestHeader(value = "X-billerId") String billerId, @RequestBody PayerRequestDTO requestBody)
            throws ExecutionException, InterruptedException;
}
