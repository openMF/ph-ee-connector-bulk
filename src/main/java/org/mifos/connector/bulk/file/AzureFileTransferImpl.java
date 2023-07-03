package org.mifos.connector.bulk.file;

import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.models.BlobProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@Qualifier("azureStorage")
@ConditionalOnProperty(
        value="cloud.azure.enabled",
        havingValue = "true")
public class AzureFileTransferImpl implements FileTransferService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    BlobClientBuilder client;

    @Override
    public String uploadFile(MultipartFile file, String bucketName) {

        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            client.containerName(bucketName).blobName(fileName).buildClient().upload(file.getInputStream(), file.getSize());
            return fileName;
        } catch (IOException e) {
            logger.error("Error uploading file to Azure", e);
        }

        return null;
    }

    @Override
    public String uploadFile(File file, String bucketName) {
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getName();
            client.containerName(bucketName).blobName(fileName).buildClient().upload(Files.newInputStream(file.toPath()), file.length());
            return fileName;
        } catch (IOException e) {
            logger.error("Error uploading file to Azure", e);
        }
        return null;
    }

    @Override
    public byte[] downloadFile(String fileName, String bucketName) {
        try {
            File temp = new File("/temp/"+fileName);
            BlobProperties properties = client.containerName(bucketName).blobName(fileName).buildClient().downloadToFile(temp.getPath());
            byte[] content = Files.readAllBytes(Paths.get(temp.getPath()));
            temp.delete();
            return content;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void deleteFile(String fileName, String bucketName) {
        client.containerName(bucketName).blobName(fileName).buildClient().delete();
    }

    @Override
    public byte[] downloadFileAsStream(String fileName, String bucketName) {
        return new byte[0];
    }
}
