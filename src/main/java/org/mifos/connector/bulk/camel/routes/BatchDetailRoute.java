package org.mifos.connector.bulk.camel.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.bulk.schema.*;
import org.mifos.connector.bulk.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mifos.connector.bulk.zeebe.ZeebeVariables.*;

@Component
public class BatchDetailRoute extends BaseRouteBuilder {

    @Value("${config.completion-threshold-check.completion-threshold}")
    private int completionThreshold;

    private static final String OPS_APP_ACCESS_TOKEN = "opsAppAccessToken";

    @Override
    public void configure() throws Exception {

        from(RouteId.BATCH_DETAIL.getValue())
                .id(RouteId.BATCH_DETAIL.getValue())
                .log("Starting route " + RouteId.BATCH_DETAIL.name())
//                .to("direct:get-access-token")
//                .choice()
//                .when(exchange -> exchange.getProperty(OPS_APP_ACCESS_TOKEN, String.class) != null)
//                .log(LoggingLevel.INFO, "Got access token, moving on to API call")
                .to("direct:batch-detail")
                .to("direct:batch-detail-response-handler");
//                .otherwise()
//                .log(LoggingLevel.INFO, "Authentication failed.")
//                .endChoice();

        getBaseExternalApiRequestRouteDefinition("batch-detail", HttpRequestMethod.GET)
                .setHeader(
                        Exchange.REST_HTTP_QUERY,
                        simple(
                                BATCH_ID + "=${exchangeProperty." + BATCH_ID + "}&" +
                                        PAGE_NO + "=${exchangeProperty." + PAGE_NO + "}&" +
                                        PAGE_SIZE + "=${exchangeProperty." + PAGE_SIZE + "}"
                        )
                )
//                .setHeader("Authorization", simple("Bearer ${exchangeProperty."+OPS_APP_ACCESS_TOKEN+"}"))
//                .setHeader("Platform-TenantId", simple("${exchangeProperty." + TENANT_ID + "}"))
                .setHeader("Platform-TenantId", simple("rhino"))
                .process(exchange -> {
                    logger.info(exchange.getIn().getHeaders().toString());
                })
//                .toD(operationsAppConfig.batchDetailUrl +  "?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .toD("http://localhost:8080/mockapi/v1/batch/detail" +  "?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log(LoggingLevel.INFO, "Batch detail API response: \n\n ${body}");

        from("direct:batch-detail-response-handler")
                .id("direct:batch-detail-response-handler")
                .log("Starting route direct:batch-detail-response-handler")
                //.setBody(exchange -> exchange.getIn().getBody(String.class))
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .log(LoggingLevel.INFO, "Batch detail request successful")
                .unmarshal().json(JsonLibrary.Jackson, BatchDetailResponse.class)
                .process(exchange -> {
                    BatchDetailResponse batchDetailResponse = exchange.getIn().getBody(BatchDetailResponse.class);
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

                    Map<String, String> previousRequestIdStatusMap;
                    if(exchange.getProperty(REQUEST_ID_STATUS_MAP)!=null){
                        previousRequestIdStatusMap = (Map<String, String>) exchange.getProperty(REQUEST_ID_STATUS_MAP);
                    }
                    else{
                        previousRequestIdStatusMap = new HashMap<>();
                    }
                    Map<String, String> requestIdStatusMap = new HashMap<>(previousRequestIdStatusMap);
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

                    if(currentTransferCount>=totalTransferCount){
                        exchange.setProperty(BATCH_DETAIL_SUCCESS, true);
                    }
                    else {
                        exchange.setProperty(BATCH_DETAIL_SUCCESS, false);
                    }

                })
//                .to("direct:upload-result-file")
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
            transactionResult.setBatchId(batchId);
            String status = requestIdStatusMap.get(transaction.getRequestId());
            transactionResult.setStatus(status);
            transactionResultList.add(transactionResult);
        }
        return transactionResultList;
    }
}
