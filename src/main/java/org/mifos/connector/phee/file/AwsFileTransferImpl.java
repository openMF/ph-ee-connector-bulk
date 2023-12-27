package org.mifos.connector.phee.file;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Service
@Qualifier("awsStorage")
public class AwsFileTransferImpl implements FileTransferService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AmazonS3 s3Client;

    @Override
    public String uploadFile(MultipartFile file, String bucketName) {

        File fileObj = convertMultiPartFileToFile(file);
        return uploadFile(fileObj, bucketName);
    }

    @Override
    public String uploadFile(File file, String bucketName) {
        String fileName = file.getName();
        s3Client.putObject(new PutObjectRequest(bucketName, fileName, file));
        file.delete();

        return fileName;
    }

    @Override
    public byte[] downloadFile(String fileName, String bucketName) {
        S3Object s3Object = s3Client.getObject(bucketName, fileName);
        S3ObjectInputStream inputStream = s3Object.getObjectContent();
        try {
            byte[] content = IOUtils.toByteArray(inputStream);
            return content;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void deleteFile(String fileName, String bucketName) {
        s3Client.deleteObject(bucketName, fileName);
    }

    @Override
    public byte[] downloadFileAsStream(String fileName, String bucketName){
        S3Object s3Object = s3Client.getObject(bucketName, fileName);
        S3ObjectInputStream inputStream = s3Object.getObjectContent();

        try{
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int numberOfBytesToWrite;
            byte[] data = new byte[1024];

            while ((numberOfBytesToWrite = inputStream.read(data, 0, data.length)) != -1){
                outputStream.write(data, 0, numberOfBytesToWrite);
            }
            inputStream.close();
            outputStream.close();
            return outputStream.toByteArray();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    private File convertMultiPartFileToFile(MultipartFile file) {
        File convertedFile = new File(file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            fos.write(file.getBytes());
        } catch (IOException e) {
            logger.error("Error converting multipartFile to file", e);
        }
        return convertedFile;
    }
}
