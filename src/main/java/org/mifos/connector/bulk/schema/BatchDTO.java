package org.mifos.connector.bulk.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// TODO: Duplicate file (Also exists in <service-name>)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BatchDTO {


    private String batchId;

    private String requestId;

    private Long total;

    private Long ongoing;

    private Long failed;

    private Long successful;

    private BigDecimal totalAmount;

    private BigDecimal successfulAmount;

    private BigDecimal pendingAmount;

    private BigDecimal failedAmount;

    private String file;

    private String notes;

    private String createdAt;

    private String status;

    private String modes;

    private String purpose;

    private String failPercentage;

    private String successPercentage;
}