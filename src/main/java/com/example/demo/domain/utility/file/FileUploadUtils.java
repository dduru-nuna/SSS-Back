package com.example.demo.domain.utility.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

@Slf4j
public class FileUploadUtils {

    public static String generateUniqueFileName(String originalFileName) {
        UUID uuid = UUID.randomUUID();
        String editedFileName = uuid + originalFileName;
        return editedFileName;
    }

    public static void writeFile(MultipartFile multipartFile, String filePath) throws IOException {
        byte[] fileContent = multipartFile.getBytes();

        try (OutputStream outputStream = new FileOutputStream(filePath)) {
            outputStream.write(fileContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
