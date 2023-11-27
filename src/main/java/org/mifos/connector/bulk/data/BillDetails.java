package org.mifos.connector.bulk.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BillDetails {
    private String billId;
    private String billerName;
    private Integer amount;
}
