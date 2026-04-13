Overview
The purpose of this application is to automate the extraction of retail inventory and product activity data from X12 EDI 852 files. This allows non-EDI systems (like Excel or BI tools) to consume point-of-sale and inventory data.

Data Mapping Logic
The converter must map specific EDI segments to CSV columns based on the following logic:

CSV Column    EDI Segment   Element Position   Description
Product_ID     LIN              LIN03              Typically the UPC or GTIN.
Vendor_PN      LIN              LIN05              The Vendor Part Number.
Activity_Code  ZA               ZA01               QA (On Hand), QS (Sold), QO (Out of Stock).
Quantity       ZA               ZA02               The numeric value of the activity.
UOM            ZA               ZA03               Unit of Measure (e.g., EA for Each).
Report_Date    XQ               XQ02               The start date of the reporting period.

Technical Constraints
Input: Flat file (ASCII/UTF-8) with ~ segment terminators.
Output: Standard RFC 4180 CSV format.
Language: Java 11 or higher (No external dependencies for the core demo).