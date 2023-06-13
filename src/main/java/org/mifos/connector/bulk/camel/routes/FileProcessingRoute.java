package org.mifos.connector.bulk.camel.routes;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.mifos.connector.bulk.schema.Transaction;
import org.mifos.connector.bulk.schema.TransactionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mifos.connector.bulk.zeebe.ZeebeVariables.FILE_NAME;

@Component
public class FileProcessingRoute extends BaseRouteBuilder {

    private static final String TRANSACTION_LIST = "transactionList";
    private static final String TRANSACTION_LIST_LENGTH = "transactionListLength";
    private static final String TOTAL_AMOUNT = "totalAmount";
    private static final String ONGOING_AMOUNT = "ongoingAmount";
    private static final String FAILED_AMOUNT = "failedAmount";
    private static final String COMPLETED_AMOUNT = "completedAmount";
    private static final String LOCAL_FILE_PATH = "localFilePath";
    private static final String RESULT_TRANSACTION_LIST = "resultTransactionList";
    private static final String OVERRIDE_HEADER = "overrideHeader";

    @Autowired
    private CsvMapper csvMapper;

    @Override
    public void configure() throws Exception {

        /**
         * Parse the [Transaction] array from the csv file
         * exchangeInput: [LOCAL_FILE_PATH] the absolute path to the csv file
         * exchangeOutput: [TRANSACTION_LIST] containing the list of [Transaction]
         */
        from("direct:get-transaction-array")
                .id("direct:get-transaction-array")
                .log("Starting route direct:get-transaction-array")
                .process(exchange -> {
                    Double totalAmount = 0.0;
                    Long failedAmount = 0L;
                    Long completedAmount = 0L;
                    String filename = exchange.getProperty(FILE_NAME, String.class);
                    CsvSchema schema = CsvSchema.emptySchema().withHeader();
                    FileReader reader = new FileReader(filename);
                    MappingIterator<Transaction> readValues = csvMapper.readerWithSchemaFor(Transaction.class).with(schema).readValues(reader);
                    List<Transaction> transactionList = new ArrayList<>();
                    while (readValues.hasNext()) {
                        Transaction current = readValues.next();
                        transactionList.add(current);
                        totalAmount += Double.parseDouble(current.getAmount());
                    }
                    reader.close();
                    exchange.setProperty(TRANSACTION_LIST, transactionList);
                    exchange.setProperty(TRANSACTION_LIST_LENGTH, transactionList.size());
                    exchange.setProperty(TOTAL_AMOUNT, totalAmount);
                    exchange.setProperty(ONGOING_AMOUNT, totalAmount); // initially ongoing amount is same as total amount
                    exchange.setProperty(FAILED_AMOUNT, failedAmount);
                    exchange.setProperty(COMPLETED_AMOUNT, completedAmount);
                });

        /**
         * updates the data in local file
         * exchangeInput:
         *      [LOCAL_FILE_PATH] the absolute path to the csv file
         *      [RESULT_TRANSACTION_LIST] containing the list of [Transaction]
         *      [OVERRIDE_HEADER] if set to true will override the header or else use the existing once in csv file
         */
        from("direct:update-result-file")
                .id("direct:update-result-file")
                .log("Starting route direct:update-result-file")
                .process(exchange -> {
                    String filepath = exchange.getProperty(LOCAL_FILE_PATH, String.class);
                    List<TransactionResult> transactionList = exchange.getProperty(RESULT_TRANSACTION_LIST, List.class);

                    for(TransactionResult result : transactionList){
                        logger.info(result.toString());
                    }
                    // getting header
//                    Boolean overrideHeader = exchange.getProperty(OVERRIDE_HEADER, Boolean.class);
                    csvWriter(transactionList, TransactionResult.class, csvMapper, true, filepath);
                })
                .log("Update complete");

    }

    private <T> void csvWriter(List<T> data, Class<T> tClass, CsvMapper csvMapper,
                               boolean overrideHeader, String filepath) throws IOException {
        CsvSchema csvSchema = csvMapper.schemaFor(tClass);
        if (overrideHeader) {
            csvSchema = csvSchema.withHeader();
        } else {
            csvSchema = csvSchema.withoutHeader();
        }

        File file = new File(filepath);
        SequenceWriter writer = csvMapper.writerWithSchemaFor(tClass).with(csvSchema).writeValues(file);
        for (T object: data) {
            writer.write(object);
        }
    }
}
