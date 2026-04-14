package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/ediconverter")
public class EdiController {

    @Autowired
    private EdiConverter ediConverter;

    @PostMapping("/convert")
    public ResponseEntity<byte[]> convertEdiToCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty".getBytes(StandardCharsets.UTF_8));
        }

        try {
            // Use absolute paths for data/request and data/response
            String baseDir = System.getProperty("user.dir");
            String reqDir = baseDir + File.separator + "data" + File.separator + "request" + File.separator;
            new File(reqDir).mkdirs();
            String ediFileName = reqDir + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File savedEdiFile = new File(ediFileName);
            file.transferTo(savedEdiFile);

            // Process the file
            List<String> csvLines = ediConverter.processFile(savedEdiFile);

            // Save generated CSV to data/response/ with a matching unique name
            String respDir = baseDir + File.separator + "data" + File.separator + "response" + File.separator;
            new File(respDir).mkdirs();
            String csvFileName = ediFileName.replace(reqDir, respDir).replaceAll("\\.edi$", ".csv").replaceAll("\\.txt$", ".csv");
            File savedCsvFile = new File(csvFileName);
            java.nio.file.Files.write(savedCsvFile.toPath(), String.join("\n", csvLines).getBytes(StandardCharsets.UTF_8));

            // Return as downloadable CSV
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", "converted.csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(String.join("\n", csvLines).getBytes(StandardCharsets.UTF_8));

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(("Error processing file: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }
}
