package org.mifos.connector.phee.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "id", "request_id", "payment_mode", "payer_identifier_type", "payer_identifier",
        "payee_identifier_type", "payee_identifier", "amount", "currency", "note", "program_shortcode", "cycle", "batch_id",
        "status", "error_code", "error_description"})
public class TransactionResult extends Transaction {

    @JsonProperty("status")
    private String status;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_description")
    private String errorDescription;

    @JsonProperty("account_number")
    @JsonIgnore
    private String accountNumber;

    @JsonIgnore
    @Override
    public void setAccountNumber(String accountNumber) {
        super.setAccountNumber(accountNumber);
    }

    @JsonIgnore
    @Override
    public String getAccountNumber() {
        return super.getAccountNumber();
    }
}
