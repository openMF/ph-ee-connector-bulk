package org.mifos.connector.bulk.utils;

import org.mifos.connector.bulk.schema.Transaction;
import org.mifos.connector.bulk.schema.TransactionResult;
import org.mifos.connector.common.channel.dto.TransactionChannelRequestDTO;
import org.mifos.connector.common.gsma.dto.*;
import org.mifos.connector.common.mojaloop.dto.MoneyData;
import org.mifos.connector.common.mojaloop.dto.Party;
import org.mifos.connector.common.mojaloop.dto.PartyIdInfo;
import org.mifos.connector.common.mojaloop.type.IdentifierType;

import java.io.*;

// TODO: Duplicate file (Also exists in <service-name>)
public class Utils {

    public static String getTenantSpecificWorkflowId(String originalWorkflowName, String tenantName) {
        return originalWorkflowName.replace("{dfspid}", tenantName);
    }

    public static String getBulkConnectorBpmnName(String originalWorkflowName,
                                                  String paymentMode, String tenantName) {
        return originalWorkflowName.replace("{MODE}", paymentMode.toLowerCase()).replace("{dfspid}", tenantName);
    }

    public static String mergeCsvFile(String file1, String file2) {
        try {
            // create a writer for permFile
            BufferedWriter out = new BufferedWriter(new FileWriter(file1, true));
            // create a reader for tmpFile
            BufferedReader in = new BufferedReader(new FileReader(file2));
            String str;
            boolean isFirstLine = true;
            while ((str = in.readLine()) != null) {
                if (isFirstLine) {
                    // used for skipping header writing
                    isFirstLine = false;
                    continue;
                }
                out.write(str+"\n");
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return file1;
    }

    public static String getAwsFileUrl(String baseUrl, String filename) {
        return String.format("%s/%s", baseUrl, filename);
    }

    /**
     * takes initial timer in the ISO 8601 durations format
     * for more info check
     * https://docs.camunda.io/docs/0.26/reference/bpmn-workflows/timer-events/#time-duration
     *
     * @param initialTimer initial timer in the ISO 8601 durations format, ex: PT45S
     * @return next timer value in the ISO 8601 durations format
     */
    public static String getNextTimer(String initialTimer){
        String stringSecondsValue = initialTimer.split("T")[1].split("S")[0];
        int initialSeconds = Integer.parseInt(stringSecondsValue);

        int currentPower = (int) ( Math.log(initialSeconds) / Math.log(2) );
        int next = (int) Math.pow(2, ++currentPower);

        return String.format("PT%sS", next);
    }

    public static String getZeebeTimerValue(int timer) {
        return String.format("PT%sS", timer);
    }

    public static TransactionResult mapToResultDTO(Transaction transaction) {
        TransactionResult transactionResult = new TransactionResult();
        transactionResult.setId(transaction.getId());
        transactionResult.setRequestId(transaction.getRequestId());
        transactionResult.setPaymentMode(transaction.getPaymentMode());
        transactionResult.setPayerIdentifierType(transaction.getPayerIdentifierType());
        transactionResult.setPayerIdentifier(transaction.getPayerIdentifier());
        transactionResult.setAmount(transaction.getAmount());
        transactionResult.setCurrency(transaction.getCurrency());
        transactionResult.setNote(transaction.getNote());
        transactionResult.setPayeeIdentifierType(transaction.getPayeeIdentifierType());
        if (transaction.getAccountNumber() != null) {
            transactionResult.setPayeeIdentifier(transaction.getAccountNumber());
        } else {
            transactionResult.setPayeeIdentifier(transaction.getPayeeIdentifier());
        }
        transactionResult.setProgramShortCode(transaction.getProgramShortCode());
        transactionResult.setCycle(transactionResult.getCycle());
        return transactionResult;
    }

}
