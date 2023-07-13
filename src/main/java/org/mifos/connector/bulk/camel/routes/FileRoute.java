package org.mifos.connector.bulk.camel.routes;

import org.mifos.connector.bulk.file.FileTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;

import static org.mifos.connector.bulk.zeebe.ZeebeVariables.FILE_NAME;

@Component
public class FileRoute extends BaseRouteBuilder {

    private static final String LOCAL_FILE_PATH = "localFilePath";
    private static final String SERVER_FILE_NAME = "serverFileName";
    @Autowired
    @Qualifier("awsStorage")
    private FileTransferService fileTransferService;

    @Value("${application.bucket-name}")
    private String bucketName;

    @Override
    public void configure() throws Exception {

        from("direct:download-file")
                .id("direct:download-file")
                .log("Starting route: direct:download-file")
                .process(exchange -> {
                    String filename = exchange.getProperty(FILE_NAME, String.class);

                    byte[] csvFile = fileTransferService.downloadFile(filename, bucketName);
                    File file = new File(filename);
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(csvFile);
                    }
                    exchange.setProperty(LOCAL_FILE_PATH, file.getAbsolutePath());
                    logger.info("File downloaded");
                });


        /**
         * Uploads the file to cloud and returns the file name in cloud
         * Input the local file path through exchange variable: [LOCAL_FILE_PATH]
         * Output the server file name through exchange variable: [SERVER_FILE_NAME]
         */
        from("direct:upload-file")
                .id("direct:upload-file")
                .log("Starting route: direct:upload-file")
                .process(exchange -> {
                    String filepath = exchange.getProperty(LOCAL_FILE_PATH, String.class);
                    String serverFileName = fileTransferService.uploadFile(new File(filepath), bucketName);
                    exchange.setProperty(SERVER_FILE_NAME, serverFileName);
                    logger.info("Uploaded file: {}", serverFileName);
                })
                .to("direct:delete-local-file");

        /**
         * Deletes file at LOCAL_FILE_PATH
         * Input the local file path through exchange variable: [LOCAL_FILE_PATH]
         */
        from("direct:delete-local-file")
                .id("direct:delete-local-file")
                .log("Deleting local file")
                .process(exchange -> {
                    String filepath = exchange.getProperty(LOCAL_FILE_PATH, String.class);
                    File file = new File(filepath);
                    boolean success = file.delete();
                    logger.info("Delete file: {}, isSuccess: {}", filepath, success);
                });
    }
}
