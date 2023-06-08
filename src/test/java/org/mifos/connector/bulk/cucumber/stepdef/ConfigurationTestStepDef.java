package org.mifos.connector.bulk.cucumber.stepdef;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.camel.Exchange;
import org.mifos.connector.bulk.config.PaymentModeMapping;
import org.mifos.connector.bulk.config.PaymentModeType;
import org.mifos.connector.bulk.utils.Utils;

import java.util.function.Function;

import static com.google.common.truth.Truth.assertThat;

public class ConfigurationTestStepDef extends BaseStepDef {

    @Given("Application context is loaded")
    public void applicationContextIsLoaded() {
        assertThat(context).isNotNull();
    }

    @When("I assert the payment mode config")
    public void paymentModeConfigAssert() {
        assertThat(paymentModeConfiguration).isNotNull();
    }

    @Then("I should get the non empty payment modes")
    public void nonEmptyPaymentModesCheck() {
        assertThat(paymentModeConfiguration.getMappings()).isNotEmpty();
    }

    @And("I should be able fetch the mapping for mode {string}")
    public void fetchMappingForMode(String mode) {
        PaymentModeMapping mapping = paymentModeConfiguration.getByMode(mode);
        assertThat(mapping).isNotNull();
        BaseStepDef.paymentModeMapping = mapping;
    }

    @And("I should get enum value {} for mode {string}")
    public void getEnumValueForMode(PaymentModeType modeType, String mode) {
        PaymentModeMapping mapping = paymentModeConfiguration.getByMode(mode);
        assertThat(mapping.getType()).isEqualTo(modeType);
    }

    @When("I have payment mode {string}")
    public void setPaymentMode(String paymentMode) {
        BaseStepDef.paymentMode = paymentMode;
        assertThat(BaseStepDef.paymentMode).isNotEmpty();
    }

    @Then("I should get the bulk connector bpmn name {string}")
    public void validateBulkConnectorBpmnName(String bpmnName) {
        PaymentModeMapping mapping = BaseStepDef.paymentModeMapping;
        String generatedBpmnName = Utils.getBulkConnectorBpmnName(mapping.getEndpoint(),
                mapping.getId(), BaseStepDef.tenant);
        assertThat(bpmnName).isEqualTo(generatedBpmnName);
    }

    @And("I have tenant as {string}")
    public void setTenant(String tenant) {
        BaseStepDef.tenant = tenant;
        assertThat(BaseStepDef.tenant).isNotEmpty();
    }

//    @When("I assert the external api payload config")
//    public void externalApiPayloadConfigAssert() {
//        assertThat(externalApiPayloadConfig).isNotNull();
//    }
//
//    @Then("I should get the non empty external api payload config")
//    public void nonEmptyExternalApiPayloadConfigCheck() {
//        int size = externalApiPayloadConfig.getPayloadMap().keySet().size();
//        assertThat(size).isGreaterThan(0);
//    }
//
//    @And("I should be able fetch the payload setter for mode {string}")
//    public void fetchPayloadSetterForMode(String mode) {
//        Function<Exchange, String> payloadSetter = externalApiPayloadConfig.getApiPayloadSetter(mode);
//        assertThat(payloadSetter).isNotNull();
//    }

}
