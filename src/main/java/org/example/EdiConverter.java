package org.example;

import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class EdiConverter {

    public List<String> processFile(InputStream inputFile) {
        List<String> outputLines = new ArrayList<>();
        // File-level header variables
        String interchange = "N/A", groupId = "N/A", fileId = "N/A", reportDate = "N/A", sender = "N/A", receiver = "N/A", vendorId = "N/A";
        // Transactional data rows
        List<String[]> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputFile))) {
            String line;
            String refNum = "N/A", upc = "N/A", activity = "N/A", price = "N/A";
            String storeId = "N/A", quantity = "N/A";
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] segmentParts = line.split("~");
                for (String segment : segmentParts) {
                    if (segment.isEmpty()) continue;
                    String[] elements = segment.split("\\*");
                    if (elements.length == 0) continue;
                    String segmentId = elements[0];
                    switch (segmentId) {
                        case "ISA" -> interchange = getVal(elements, 13); // ISA13
                        case "GS" -> groupId = getVal(elements, 1); // GS01
                        case "ST" -> fileId = getVal(elements, 2); // ST02
                        case "XQ" -> reportDate = getVal(elements, 2); // XQ02
                        case "N1" -> {
                            String role = getVal(elements, 1);
                            if ("FR".equals(role)) sender = getVal(elements, 2);
                            else if ("TO".equals(role)) receiver = getVal(elements, 2);
                        }
                        case "N9" -> {
                            if ("IA".equals(getVal(elements, 1))) vendorId = getVal(elements, 2);
                        }
                        case "LIN" -> {
                            refNum = getVal(elements, 2); // LIN02
                            upc = getVal(elements, 3); // LIN03
                        }
                        case "ZA" -> activity = getVal(elements, 1); // ZA01
                        case "CTP" -> price = getVal(elements, 3); // CTP03
                        case "SDQ" -> {
                            for (int i = 3; i < elements.length; i += 2) {
                                storeId = getVal(elements, i);
                                quantity = getVal(elements, i + 1);
                                if (!"N/A".equals(storeId)) {
                                    rows.add(new String[]{refNum, upc, activity, price, storeId, quantity});
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Build output lines
        outputLines.add("FILE LEVEL HEADERS");
        outputLines.add("Interchang,Group_ID,File_ID,Report_Dt,Sender,Receiver,Vendor_ID");
        outputLines.add(String.format("%s,%s,%s,%s,%s,%s,%s", interchange, groupId, fileId, reportDate, sender, receiver, vendorId));
        outputLines.add("");
        outputLines.add("TRANSACTIONAL DATA");
        outputLines.add("Ref_Num,UPC,Activity,Price,Store_ID,Quantity");
        for (String[] row : rows) {
            outputLines.add(String.join(",", row));
        }
        return outputLines;
    }

    private String getVal(String[] elements, int index) {
        return (index >= 0 && index < elements.length && !elements[index].isEmpty()) ? elements[index] : "N/A";
    }
}

