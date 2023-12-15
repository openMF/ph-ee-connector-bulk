package org.mifos.connector.phee.api.definition;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.mifos.connector.phee.data.BillRTPReqDTO;
import org.mifos.connector.phee.data.ResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.ExecutionException;

@Tag(name = "GOV")
public interface BillRtpRespApi {

    @Operation(
            summary = "Bill RTP Resp API from PFI to PBB")
    @PostMapping("/billTransferRequests")
    ResponseEntity<ResponseDTO> billRTPResp(@RequestHeader(value="X-Platform-TenantId") String tenantId,
                                           @RequestHeader(value="X-Client-Correlation-ID") String correlationId,
                                           @RequestParam(value = "X-Biller-Id") String billerId,
                                           @RequestBody BillRTPReqDTO billRTPReqDTO)
            throws ExecutionException, InterruptedException;
}