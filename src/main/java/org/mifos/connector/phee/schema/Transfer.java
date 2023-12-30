package org.mifos.connector.phee.schema;

import java.math.BigDecimal;
import java.util.Date;

public class Transfer {

    private String id;

    private Long workflowInstanceKey;

    private String transactionId;

    private Date startedAt;

    private Date completedAt;

    private TransferStatus status;

    private String statusDetail;

    private String payeeDfspId;

    private String payeePartyId;

    private String payeePartyIdType;

    private BigDecimal payeeFee;

    private String payeeFeeCurrency;

    private String payeeQuoteCode;


    private String payerDfspId;

    private String payerPartyId;

    private String payerPartyIdType;

    private BigDecimal payerFee;

    private String payerFeeCurrency;

    private String payerQuoteCode;

    private BigDecimal amount;

    private String currency;

    private String direction;

    private String errorInformation;

    private String batchId;

    private String clientCorrelationId;

    public Transfer() {
    }

    public Transfer(Long workflowInstanceKey) {
        this.workflowInstanceKey = workflowInstanceKey;
        this.status = TransferStatus.IN_PROGRESS;
    }

    public Transfer(String id, Long workflowInstanceKey, String transactionId, Date startedAt, Date completedAt,
//    public Transfer(Long workflowInstanceKey, String transactionId, Date startedAt, Date completedAt,
                    TransferStatus status, String statusDetail, String payeeDfspId, String payeePartyId,
                    String payeePartyIdType, BigDecimal payeeFee, String payeeFeeCurrency, String payeeQuoteCode,
                    String payerDfspId, String payerPartyId, String payerPartyIdType, BigDecimal payerFee,
                    String payerFeeCurrency, String payerQuoteCode, BigDecimal amount, String currency, String direction,
                    String errorInformation, String batchId, String clientCorrelationId) {
        this.id = id;
        this.workflowInstanceKey = workflowInstanceKey;
        this.transactionId = transactionId;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.status = status;
        this.statusDetail = statusDetail;
        this.payeeDfspId = payeeDfspId;
        this.payeePartyId = payeePartyId;
        this.payeePartyIdType = payeePartyIdType;
        this.payeeFee = payeeFee;
        this.payeeFeeCurrency = payeeFeeCurrency;
        this.payeeQuoteCode = payeeQuoteCode;
        this.payerDfspId = payerDfspId;
        this.payerPartyId = payerPartyId;
        this.payerPartyIdType = payerPartyIdType;
        this.payerFee = payerFee;
        this.payerFeeCurrency = payerFeeCurrency;
        this.payerQuoteCode = payerQuoteCode;
        this.amount = amount;
        this.currency = currency;
        this.direction = direction;
        this.errorInformation = errorInformation;
        this.batchId = batchId;
        this.clientCorrelationId = clientCorrelationId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public BigDecimal getPayeeFee() {
        return payeeFee;
    }

    public void setPayeeFee(BigDecimal payeeFee) {
        this.payeeFee = payeeFee;
    }

    public String getPayeeQuoteCode() {
        return payeeQuoteCode;
    }

    public void setPayeeQuoteCode(String payeeQuoteCode) {
        this.payeeQuoteCode = payeeQuoteCode;
    }

    public BigDecimal getPayerFee() {
        return payerFee;
    }

    public void setPayerFee(BigDecimal payerFee) {
        this.payerFee = payerFee;
    }

    public String getPayerQuoteCode() {
        return payerQuoteCode;
    }

    public void setPayerQuoteCode(String payerQuoteCode) {
        this.payerQuoteCode = payerQuoteCode;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Long getWorkflowInstanceKey() {
        return workflowInstanceKey;
    }

    public void setWorkflowInstanceKey(Long paymentProcessId) {
        this.workflowInstanceKey = paymentProcessId;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public String getPayeePartyId() {
        return payeePartyId;
    }

    public void setPayeePartyId(String payeePartyId) {
        this.payeePartyId = payeePartyId;
    }

    public String getPayeePartyIdType() {
        return payeePartyIdType;
    }

    public void setPayeePartyIdType(String payeePartyType) {
        this.payeePartyIdType = payeePartyType;
    }

    public String getPayerPartyId() {
        return payerPartyId;
    }

    public void setPayerPartyId(String payerPartyId) {
        this.payerPartyId = payerPartyId;
    }

    public String getPayerPartyIdType() {
        return payerPartyIdType;
    }

    public void setPayerPartyIdType(String payerPartyType) {
        this.payerPartyIdType = payerPartyType;
    }

    public String getPayeeFeeCurrency() {
        return payeeFeeCurrency;
    }

    public void setPayeeFeeCurrency(String payeeFeeCurrency) {
        this.payeeFeeCurrency = payeeFeeCurrency;
    }

    public String getPayerFeeCurrency() {
        return payerFeeCurrency;
    }

    public void setPayerFeeCurrency(String payerFeeCurrency) {
        this.payerFeeCurrency = payerFeeCurrency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPayeeDfspId() {
        return payeeDfspId;
    }

    public void setPayeeDfspId(String payeeDfspId) {
        this.payeeDfspId = payeeDfspId;
    }

    public String getPayerDfspId() {
        return payerDfspId;
    }

    public void setPayerDfspId(String payerDfspId) {
        this.payerDfspId = payerDfspId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatusDetail() {
        return statusDetail;
    }

    public void setStatusDetail(String statusDetail) {
        this.statusDetail = statusDetail;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getErrorInformation() {
        return errorInformation;
    }

    public void setErrorInformation(String errorInformation) {
        this.errorInformation = errorInformation;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getClientCorrelationId() {
        return clientCorrelationId;
    }

    public void setClientCorrelationId(String clientCorrelationId) {
        this.clientCorrelationId = clientCorrelationId;
    }
}
