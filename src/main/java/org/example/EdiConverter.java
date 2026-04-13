package org.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class EdiConverter {

    @Value("${edi.output.path:C:/Users/HP/IdeaProjects/EDI852Converter/data/output/}")
    private String outputDirPath;

    public void processFile(File inputFile) {
        // Ensure directory exists
        try {
            Files.createDirectories(Paths.get(outputDirPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Path outputPath = Paths.get(outputDirPath, inputFile.getName().replaceAll("(?i)\\.(edi|txt)$", ".csv"));

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
             PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8))) {

            // Header for CSV - Includes all requested segments
            pw.println("ISA_Control_No,GS_Sender_ID,Report_Date,Ref_IA,Sender_Name,Receiver_Name," +
                    "Product_ID,UPC,Activity_Code,Price,Store_ID,Quantity");

            // Variables to hold "State" as we move down the file
            String isaCtrl = "N/A", gsSender = "N/A", reportDate = "N/A", refIa = "N/A";
            String senderName = "N/A", receiverName = "N/A";
            String productId = "N/A", upc = "N/A", activityCode = "N/A", price = "N/A";

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Split by the delimiter (handle the segment terminator ~)
                String[] segmentParts = line.split("~");
                
                for (String segment : segmentParts) {
                    if (segment.isEmpty()) continue;

                    // Split by element delimiter * to get fields
                    String[] elements = segment.split("\\*");
                    if (elements.length == 0) continue;

                    String segmentId = elements[0];

                    switch (segmentId) {
                        case "ISA" -> isaCtrl = getVal(elements, 13); // ISA13 is at position 13 (0-indexed)
                        case "GS"  -> gsSender = getVal(elements, 2);  // GS02 is at position 2
                        case "XQ"  -> reportDate = getVal(elements, 2); // XQ02 is at position 2
                        case "N9"  -> {
                            // N901 is at position 1, N902 is at position 2
                            if ("IA".equals(getVal(elements, 1))) refIa = getVal(elements, 2);
                        }
                        case "N1" -> {
                            // N101 is at position 1, N102 is at position 2
                            String role = getVal(elements, 1);
                            if ("FR".equals(role)) senderName = getVal(elements, 2);
                            else if ("TO".equals(role)) receiverName = getVal(elements, 2);
                        }
                        case "LIN" -> {
                            // LIN03 is at position 3, LIN05 is at position 5
                            productId = getVal(elements, 3);
                            upc = getVal(elements, 5);
                        }
                        case "ZA" -> activityCode = getVal(elements, 1); // ZA01 is at position 1
                        case "CTP" -> price = getVal(elements, 3); // CTP03 is at position 3
                        case "SDQ" -> {
                            // SDQ contains: SDQ01=Unit, SDQ02=Unit, SDQ03=Store, SDQ04=Qty, SDQ05=Store, SDQ06=Qty...
                            for (int i = 3; i < elements.length; i += 2) {
                                String storeId = getVal(elements, i);
                                String qty = getVal(elements, i + 1);

                                if (!"N/A".equals(storeId)) {
                                    pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                                            isaCtrl, gsSender, reportDate, refIa, senderName, receiverName,
                                            productId, upc, activityCode, price, storeId, qty);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getVal(String[] elements, int index) {
        return (index >= 0 && index < elements.length && !elements[index].isEmpty()) ? elements[index] : "N/A";
    }
}