package org.mifos.connector.phee.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BillInquiryResponseDTO implements Serializable {
    private String transactionId;
    //private List<PaymentModalityDTO> paymentModalityList;
}
