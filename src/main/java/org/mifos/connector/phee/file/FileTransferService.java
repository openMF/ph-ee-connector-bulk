package org.mifos.connector.phee.file;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

// TODO: Duplicate file (Also exists in <service-name>)
public interface FileTransferService {

    String uploadFile(MultipartFile file, String bucketName);

    String uploadFile(File file, String bucketName);

    byte[] downloadFile(String fileName, String bucketName);

    void deleteFile(String fileName, String bucketName);

    byte[] downloadFileAsStream(String fileName, String bucketName);

}
