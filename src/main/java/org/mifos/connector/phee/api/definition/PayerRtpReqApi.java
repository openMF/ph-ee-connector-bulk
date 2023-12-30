package org.mifos.connector.phee.api.definition;

import org.mifos.connector.phee.data.PayerRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.mifos.connector.phee.data.ResponseDTO;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.concurrent.ExecutionException;

@Tag(name = "GOV")
public interface PayerRtpReqApi {

    @Operation(
            summary = "Bill RTP Req API from PBB to PFI")
    @PostMapping(value = "/billTransferRequests", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> payerRtpReq(@RequestHeader(value="X-Platform-TenantId") String tenantId,
                                            @RequestHeader(value="X-Client-Correlation-ID") String correlationId,
                                            @RequestHeader(value = "X-Callback-URL")
                                                               String callbackUrl,
                                            @RequestHeader(value = "X-Biller-Id") String billerId,
                                            @RequestBody String payerRequestDTO)
            throws ExecutionException, InterruptedException;

}