package org.mifos.connector.bulk.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayerResponseDTO {
    private String requestId;
    private String transactionId;
    private String rtpId;
    private String billId;
    private String rtpStatus;
    private String rejectReason;
}
