package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class EdiConverterApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(EdiConverterApplication.class, args);

        // Get the EdiConverter bean from Spring context
        EdiConverter converter = context.getBean(EdiConverter.class);

        // Path to input EDI file
        String inputPath = "C:/Users/35311/Desktop/EDI852Converter/data/input/sample_852.edi";
        File inputFile = new File(inputPath);

        if (inputFile.exists()) {
            System.out.println("Processing EDI file: " + inputFile.getAbsolutePath());
            converter.processFile(inputFile);
            System.out.println("File processing completed!");
        } else {
            System.out.println("Input file not found: " + inputPath);
        }
    }
}

