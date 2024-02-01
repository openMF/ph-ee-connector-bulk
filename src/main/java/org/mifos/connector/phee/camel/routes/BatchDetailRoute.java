package org.mifos.connector.phee.camel.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.mifos.connector.phee.config.MockPaymentSchemaConfig;
import org.mifos.connector.phee.schema.BatchDetailResponse;
import org.mifos.connector.phee.schema.Transaction;
import org.mifos.connector.phee.schema.TransactionResult;
import org.mifos.connector.phee.schema.Transfer;
import org.mifos.connector.phee.schema.TransferStatus;
import org.mifos.connector.phee.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mifos.connector.phee.zeebe.ZeebeVariables.BATCH_DETAIL_SUCCESS;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.COMPLETED_TRANSACTION_COUNT;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.CURRENT_TRANSACTION_COUNT;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.ERROR_CODE;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.ERROR_DESCRIPTION;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.FAILED_TRANSACTION_COUNT;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.LOCAL_FILE_PATH;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.ONGOING_TRANSACTION_COUNT;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.OVERRIDE_HEADER;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.PAGE_NO;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.REQUEST_ID_STATUS_MAP;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.RESULT_FILE;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.RESULT_TRANSACTION_LIST;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.TOTAL_TRANSACTION;
import static org.mifos.connector.phee.zeebe.ZeebeVariables.TRANSACTION_LIST;


@Component
public class BatchDetailRoute extends BaseRouteBuilder {

    @Value("${config.completion-threshold-check.completion-threshold}")
    private int completionThreshold;

    @Value("${tenant}")
    public String tenant;

    @Autowired
    public MockPaymentSchemaConfig mockPaymentSchemaConfig;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure() throws Exception {

        from(RouteId.BATCH_DETAIL.getValue())
                .id(RouteId.BATCH_DETAIL.getValue())
                .log("Starting route " + RouteId.BATCH_DETAIL.name())
                .to("direct:batch-detail-api-call")
                .to("direct:batch-detail-response-handler");

        from("direct:batch-detail-api-call")
                .routeId("direct:batch-detail-api-call")
                .setHeader("CamelHttpMethod", constant("GET"))
                .setHeader("Accept", constant("application/json"))
                .toD(mockPaymentSchemaConfig.mockPaymentSchemaContactPoint+"/batches/"+ "${exchangeProperty.batchId}"+"/detail"+ "?" + "pageNo" + "=${exchangeProperty.pageNo}&"  + "pageSize"
                        + "=${exchangeProperty.pageSize}"  )
                .log("API Response: ${body}")
                .setProperty("apiResponse", body()) ; // Log the API response, you can modify this according to your needs


        from("direct:batch-detail-response-handler")
                .id("direct:batch-detail-response-handler")
                .log("Starting route direct:batch-detail-response-handler")
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .log(LoggingLevel.INFO, "Batch detail request successful")
                .process(exchange -> {
                    String apiResponse = exchange.getProperty("apiResponse", String.class);
                    logger.info(apiResponse);
                    BatchDetailResponse batchDetailResponse = objectMapper.readValue(apiResponse, BatchDetailResponse.class);
                    logger.info(batchDetailResponse.toString());

                    int pageNo = Integer.parseInt(exchange.getProperty(PAGE_NO, String.class));
                    int currentTransferCount = exchange.getProperty(CURRENT_TRANSACTION_COUNT)!=null ?
                            Integer.parseInt(exchange.getProperty(CURRENT_TRANSACTION_COUNT, String.class)) : 0;
                    int completedTransferCount = exchange.getProperty(COMPLETED_TRANSACTION_COUNT)!=null ?
                            Integer.parseInt(exchange.getProperty(COMPLETED_TRANSACTION_COUNT, String.class)) : 0;
                    int failedTransferCount = exchange.getProperty(FAILED_TRANSACTION_COUNT)!=null ?
                            Integer.parseInt(exchange.getProperty(FAILED_TRANSACTION_COUNT, String.class)) : 0;
                    int ongoingTransferCount = exchange.getProperty(ONGOING_TRANSACTION_COUNT)!=null ?
                            Integer.parseInt(exchange.getProperty(ONGOING_TRANSACTION_COUNT, String.class)) : 0;
                    int totalTransferCount = exchange.getProperty(TOTAL_TRANSACTION)!=null ?
                            Integer.parseInt(exchange.getProperty(TOTAL_TRANSACTION, String.class)) : 0;

                    List<Transfer> transfers = batchDetailResponse.getContent();
                    int completedTransferCountPerPage = 0;
                    int ongoingTransferCountPerPage = 0;
                    int failedTransferCountPerPage = 0;
                    int transferCountPerPage = 0;

                    Map<String, String> requestIdStatusMap;
                    if(exchange.getProperty(REQUEST_ID_STATUS_MAP)!=null){
                        requestIdStatusMap = (Map<String, String>) exchange.getProperty(REQUEST_ID_STATUS_MAP);
                    }
                    else{
                        requestIdStatusMap = new HashMap<>();
                    }

                    for(Transfer transfer : transfers){
                        TransferStatus transferStatus = transfer.getStatus();
                        requestIdStatusMap.put(transfer.getTransactionId(), transferStatus.toString());

                        if(TransferStatus.COMPLETED.equals(transferStatus)){
                            completedTransferCountPerPage++;
                        }
                        else if(TransferStatus.FAILED.equals(transferStatus)){
                            failedTransferCountPerPage++;
                        }
                        else if(TransferStatus.IN_PROGRESS.equals(transferStatus)){
                            ongoingTransferCountPerPage++;
                        }
                        transferCountPerPage++;
                    }

                    currentTransferCount += transferCountPerPage;
                    completedTransferCount += completedTransferCountPerPage;
                    failedTransferCount += failedTransferCountPerPage;
                    ongoingTransferCount += ongoingTransferCountPerPage;

                    exchange.setProperty(CURRENT_TRANSACTION_COUNT, currentTransferCount);
                    exchange.setProperty(COMPLETED_TRANSACTION_COUNT, completedTransferCount);
                    exchange.setProperty(FAILED_TRANSACTION_COUNT, failedTransferCount);
                    exchange.setProperty(ONGOING_TRANSACTION_COUNT, ongoingTransferCount);
                    exchange.setProperty(REQUEST_ID_STATUS_MAP, requestIdStatusMap);

                    if(currentTransferCount<=totalTransferCount){
                        exchange.setProperty(BATCH_DETAIL_SUCCESS, true);
                    }
                    else {
                        exchange.setProperty(BATCH_DETAIL_SUCCESS, false);
                    }

                })
                .otherwise()
                .log(LoggingLevel.ERROR, "Batch detail request unsuccessful")
                .process(exchange -> {
                    exchange.setProperty(BATCH_DETAIL_SUCCESS, false);
                    exchange.setProperty(ERROR_DESCRIPTION, exchange.getIn().getBody(String.class));
                    exchange.setProperty(ERROR_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
                });

        from("direct:upload-result-file")
                .id("direct:upload-result-file")
                .log("Starting route direct:upload-result-file")
                .choice()
                .when(exchange -> exchange.getProperty(BATCH_DETAIL_SUCCESS, Boolean.class))
                .to("direct:download-file")
                .to("direct:get-transaction-array")
                .process(exchange -> {
                    String serverFileName = exchange.getProperty(FILE_NAME, String.class);
                    String batchId = exchange.getProperty(BATCH_ID, String.class);
                    String resultFile = String.format("Result_%s", serverFileName);
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    Object property = exchange.getProperty(REQUEST_ID_STATUS_MAP);
                    Map<String, String> requestIdStatusMap = (Map<String, String>) property;
                    List<TransactionResult> transactionResultList = fetchTransactionResult(transactionList, requestIdStatusMap, requestIdStatusMap, batchId);
                    exchange.setProperty(RESULT_TRANSACTION_LIST, transactionResultList);
                    exchange.setProperty(RESULT_FILE, resultFile);
                    exchange.setProperty(LOCAL_FILE_PATH, exchange.getProperty(RESULT_FILE));
                    exchange.setProperty(OVERRIDE_HEADER, constant(true));
                })
                .to("direct:update-result-file")
                .to("direct:upload-file");
    }

    private List<TransactionResult> fetchTransactionResult(List<Transaction> transactionList, Object property, Map<String, String> requestIdStatusMap, String batchId) {
        List<TransactionResult> transactionResultList = new ArrayList<>();
        for (Transaction transaction : transactionList) {
            TransactionResult transactionResult = Utils.mapToResultDTO(transaction);
            transactionResult.setPaymentMode("CLOSEDLOOP");
            transactionResult.setBatchId(transaction.getBatchId());
            String status = requestIdStatusMap.get(transaction.getRequestId());
            transactionResult.setStatus(status);
            transactionResultList.add(transactionResult);
        }
        return transactionResultList;
    }
}
