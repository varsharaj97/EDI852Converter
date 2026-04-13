package org.example;

import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamReader;
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

    @Value("${edi.output.path:C:/Users/35311/Desktop/EDI852Converter/data/output/}")
    private String outputDirPath;

    public void processFile(File inputFile) {
        // Ensure directory exists
        try {
            Files.createDirectories(Paths.get(outputDirPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Path outputPath = Paths.get(outputDirPath, inputFile.getName().replaceAll("(?i)\\.(edi|txt)$", ".csv"));
        EDIInputFactory factory = EDIInputFactory.newFactory();

        try (InputStream is = new FileInputStream(inputFile);
             EDIStreamReader reader = factory.createEDIStreamReader(is);
             PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8))) {

            // Header for CSV - Includes all requested segments
            pw.println("ISA_Control_No,GS_Sender_ID,Report_Date,Ref_IA,Sender_Name,Receiver_Name," +
                    "Product_ID,UPC,Activity_Code,Price,Store_ID,Quantity");

            // Variables to hold "State" as we move down the file
            String isaCtrl = "N/A", gsSender = "N/A", reportDate = "N/A", refIa = "N/A";
            String senderName = "N/A", receiverName = "N/A";
            String productId = "N/A", upc = "N/A", activityCode = "N/A", price = "N/A";

            while (reader.hasNext()) {
                EDIStreamEvent event = reader.next();

                if (event == EDIStreamEvent.START_SEGMENT) {
                    String segmentId = reader.getText();
                    List<String> elements = readSegment(reader);

                    switch (segmentId) {
                        case "ISA" -> isaCtrl = getVal(elements, 13);
                        case "GS"  -> gsSender = getVal(elements, 2);
                        case "XQ"  -> reportDate = getVal(elements, 2);
                        case "N9"  -> {
                            if ("IA".equals(getVal(elements, 1))) refIa = getVal(elements, 2);
                        }
                        case "N1" -> {
                            String role = getVal(elements, 1);
                            if ("FR".equals(role)) senderName = getVal(elements, 2);
                            else if ("TO".equals(role)) receiverName = getVal(elements, 2);
                        }
                        case "LIN" -> {
                            productId = getVal(elements, 3); // Product ID (Buyer Part Number)
                            upc = getVal(elements, 5);       // UPC Code
                        }
                        case "ZA" -> activityCode = getVal(elements, 1);
                        case "CTP" -> price = getVal(elements, 3);
                        case "SDQ" -> {
                            // SDQ contains pairs: SDQ03=Store, SDQ04=Qty, SDQ05=Store, SDQ06=Qty...
                            for (int i = 3; i < elements.size(); i += 2) {
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

    private List<String> readSegment(EDIStreamReader reader) throws Exception {
        List<String> elements = new ArrayList<>();
        while (reader.hasNext()) {
            EDIStreamEvent event = reader.next();
            if (event == EDIStreamEvent.ELEMENT_DATA) {
                elements.add(reader.getText());
            } else if (event == EDIStreamEvent.END_SEGMENT) {
                break;
            }
        }
        return elements;
    }

    private String getVal(List<String> elements, int index) {
        int listIndex = index - 1;
        return (listIndex >= 0 && listIndex < elements.size()) ? elements.get(listIndex) : "N/A";
    }
}