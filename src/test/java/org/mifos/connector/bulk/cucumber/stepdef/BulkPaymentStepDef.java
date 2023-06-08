package org.mifos.connector.bulk.cucumber.stepdef;

import com.google.gson.Gson;
import io.restassured.http.Headers;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.MultiPartSpecification;
import io.restassured.specification.RequestSpecification;
import org.mifos.connector.bulk.schema.BatchDTO;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import static com.google.common.truth.Truth.assertThat;

public class BulkPaymentStepDef extends BaseStepDef {

    private String batchId;

    private int completionPercent;

    @Value("${config.completion-threshold-check.completion-threshold}")
    private int thresholdPercent;

    @Given("the CSV file is available")
    public boolean isCsvFileAvailable(){
        String fileName = "test.csv";
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(fileName);

        return resource != null && resource.getPath().endsWith(".csv");
    }

    @When("initiate the batch transaction API with the input CSV file")
    public void initiateBatchTransactionApi(){
        Map<String, String> headers = new HashMap<>();
        headers.put("Purpose", "test paymnent");
        headers.put("filename", "test.csv");
        headers.put("X-CorrelationID", "12345678-6897-6798-6798-098765432134");
        headers.put("Platform-TenantId", "gorilla");
//        String fileContent = getFileContent(BPMN_FILE_URL);
        String fileContent = getFileContent("test.csv");
        logger.info("file content: " + fileContent);
        RequestSpecification requestSpec = getDefaultSpec();
        String response =  RestAssured.given(requestSpec)
                .baseUri("https://bulk-connector.sandbox.fynarfin.io")
                .multiPart(getMultiPart(fileContent))
                .headers(headers)
                .expect()
                .spec(new ResponseSpecBuilder().expectStatusCode(200).build())
                .when()
                .post("/batchtransactions?type=csv")
                .andReturn().asString();
        batchId = fetchBatchId(response);
        logger.info("Batch transaction API response: " + response);
    }



    @Given("the batch ID for the submitted CSV file")
    public void isBatchIdAvailable(){
        assertThat(batchId).isNotEmpty();
    }

    @When("poll the batch summary API using the batch ID")
    public void initiateBatchSummaryApi(){
        Map<String, String> headers = new HashMap<>();
        headers.put("Platform-TenantId", "lion");
        RequestSpecification requestSpec = getDefaultSpec();
        String batchSummaryResponse =  RestAssured.given(requestSpec)
                .baseUri("http://localhost:8080")
                .param("batchId", batchId)
                .param("X-CorrelationID", "12345678-6897-6798-6798-098765432134")
                .headers(headers)
                .expect()
                .spec(new ResponseSpecBuilder().expectStatusCode(200).build())
                .when()
                .post("/mockapi/v1/batch")
                .andReturn().asString();
        Gson gson = new Gson();
        BatchDTO batchDTO = gson.fromJson(batchSummaryResponse, BatchDTO.class);
        completionPercent = (int) (batchDTO.getSuccessful()/ batchDTO.getTotal() * 100);
        assertThat(completionPercent).isNotNull();
    }

//    @Then("API should return the response with total, successful, failed and ongoing transactions count")
//    public void checkTransactionCount(){
//        assertThat(batchDTO).isNotNull();
//        assertThat(batchDTO.getFailed()).isNotNull();
//        assertThat(batchDTO.getOngoing()).isNotNull();
//        assertThat(batchDTO.getSuccessful()).isNotNull();
//        assertThat(batchDTO.getTotal()).isNotNull();
//    }

    @Then("successful transactions percentage should be greater than or equal to minimum threshold")
    public void batchSummarySuccessful(){
        assertThat(completionPercent).isGreaterThan(thresholdPercent);
    }

    private static RequestSpecification getDefaultSpec() {
        RequestSpecification requestSpec = new RequestSpecBuilder().build();
        requestSpec.relaxedHTTPSValidation();
        return requestSpec;
    }

    private MultiPartSpecification getMultiPart(String fileContent) {
        return new MultiPartSpecBuilder(fileContent.getBytes()).
                fileName("test.csv").
                controlName("file").
                mimeType("text/plain").
                build();
    }

    private String getFileContent(String filePath) {
        File file = new File(filePath);
        Reader reader;
        CSVFormat csvFormat;
        CSVParser csvParser = null;
        try {
            reader = new FileReader(file);
            csvFormat = CSVFormat.DEFAULT.withDelimiter(',');
            csvParser = new CSVParser(reader, csvFormat);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        StringJoiner stringJoiner = new StringJoiner("\n");

        for (CSVRecord csvRecord : csvParser) {
            stringJoiner.add(csvRecord.toString());
        }
        return stringJoiner.toString();
    }

    private String fetchBatchId(String response) {
        String[] split = response.split(",");
        return split[0].substring(31);
    }

}
