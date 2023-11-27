package org.mifos.connector.bulk.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayerRequestDTO {
    private String requestId;
    private String transactionId;
    private Integer rtpId;
    private List<BillDetails> billDetails;

}
