package org.mifos.connector.bulk.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "id", "request_id", "payment_mode", "account_number", "payer_identifier_type", "payer_identifier", "payee_identifier_type", "payee_identifier", "amount", "currency", "note", "program_shortcode", "cycle" })
public class Transaction implements CsvSchema {

    @JsonProperty("id")
    private int id;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("payment_mode")
    private String paymentMode;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("note")
    private String note;

    @JsonProperty(value = "payer_identifier_type")
    private String payerIdentifierType;

    @JsonProperty("payer_identifier")
    private String payerIdentifier;

    @JsonProperty("payee_identifier_type")
    private String payeeIdentifierType;

    @JsonProperty("payee_identifier")
    private String payeeIdentifier;

    @JsonProperty("program_shortcode")
    private String programShortCode;

    @JsonProperty("cycle")
    private String cycle;

    @JsonIgnore
    private String batchId;

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("Transaction{");
        buffer.append("id=").append(id);
        buffer.append(", request_id='").append(requestId);
        buffer.append(", payment_mode='").append(paymentMode);
        buffer.append(", account_number='").append(accountNumber);
        buffer.append(", amount='").append(amount);
        buffer.append(", currency='").append(currency);
        buffer.append(", note='").append(note);
        buffer.append(", batchId='").append(batchId);
        buffer.append(", status='").append(id).append('}');
        return buffer.toString();
    }

    @JsonIgnore
    @Override
    public String getCsvString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s", id, requestId, paymentMode, accountNumber, amount, currency, note);
    }

    @JsonIgnore
    @Override
    public String getCsvHeader() {
        return "id,request_id,payment_mode,account_number,amount,currency,note,status";
    }
}
