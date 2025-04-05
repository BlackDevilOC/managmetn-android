package substitutemanager.utils

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Utility class for parsing CSV files.
 */
object CsvUtils {
    /**
     * Parses a CSV file from an input stream.
     *
     * @param inputStream The input stream to read from
     * @param hasHeader Whether the CSV has a header row
     * @param delimiter The delimiter used in the CSV
     * @return A list of rows, where each row is a list of columns
     */
    fun parseCSV(
        inputStream: InputStream, 
        hasHeader: Boolean = true, 
        delimiter: Char = ','
    ): List<List<String>> {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val result = mutableListOf<List<String>>()
        
        var firstLine = true
        var line: String? = reader.readLine()
        
        while (line != null) {
            if (firstLine && hasHeader) {
                firstLine = false
                line = reader.readLine()
                continue
            }
            
            if (line.isNotBlank()) {
                val row = line.split(delimiter).map { it.trim() }
                result.add(row)
            }
            
            line = reader.readLine()
        }
        
        return result
    }
    
    /**
     * Parses a CSV string.
     *
     * @param content The CSV content
     * @param hasHeader Whether the CSV has a header row
     * @param delimiter The delimiter used in the CSV
     * @return A list of rows, where each row is a list of columns
     */
    fun parseCSVString(
        content: String, 
        hasHeader: Boolean = true, 
        delimiter: Char = ','
    ): List<List<String>> {
        val lines = content.split("\n")
        val result = mutableListOf<List<String>>()
        
        var firstLine = true
        
        for (line in lines) {
            if (firstLine && hasHeader) {
                firstLine = false
                continue
            }
            
            if (line.isNotBlank()) {
                val row = line.split(delimiter).map { it.trim() }
                result.add(row)
            }
        }
        
        return result
    }

    /**
     * Fixes common CSV format issues.
     *
     * @param content The CSV content to fix
     * @param expectedColumns The expected number of columns
     * @return The fixed CSV content
     */
    fun fixCSVContent(content: String, expectedColumns: Int): String {
        val lines = content.split("\n")
        val fixedLines = lines.map { line ->
            // Remove extra quotes if they're unbalanced
            val quoteCount = line.count { it == '"' }
            var fixedLine = if (quoteCount % 2 != 0) {
                line.replace("\"", "")
            } else {
                line
            }
            
            // Ensure each line ends with the right number of commas
            val commaCount = fixedLine.count { it == ',' }
            
            if (commaCount > expectedColumns - 1) {
                // Too many commas, trim excess
                val parts = fixedLine.split(",").take(expectedColumns)
                fixedLine = parts.joinToString(",")
            } else if (commaCount < expectedColumns - 1 && fixedLine.isNotBlank()) {
                // Too few commas, add missing ones
                val missingCommas = expectedColumns - 1 - commaCount
                fixedLine = fixedLine + ",".repeat(missingCommas)
            }
            
            fixedLine
        }
        
        return fixedLines.joinToString("\n")
    }
}