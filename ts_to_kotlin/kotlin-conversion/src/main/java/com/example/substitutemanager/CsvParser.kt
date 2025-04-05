package com.example.substitutemanager

import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Utility class for parsing CSV files
 */
object CsvParser {
    private const val TAG = "CsvParser"
    
    /**
     * Parse a timetable CSV file into a list of rows
     * 
     * @param inputStream The input stream for the CSV file
     * @return List of parsed rows
     */
    fun parseTimetable(inputStream: InputStream): List<Map<String, String>> {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val headerLine = reader.readLine() ?: throw IllegalArgumentException("Empty CSV file")
            
            // Parse headers, removing any BOM characters
            val headers = headerLine
                .replace("\uFEFF", "") // Remove BOM if present
                .split(",")
                .map { it.trim() }
            
            // Parse rows
            val rows = mutableListOf<Map<String, String>>()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                line?.let { 
                    // Skip empty lines
                    if (it.isBlank()) return@let
                    
                    val values = it.split(",").map { value -> value.trim() }
                    val row = mutableMapOf<String, String>()
                    
                    // Map values to headers
                    headers.forEachIndexed { index, header ->
                        val value = if (index < values.size) values[index] else ""
                        row[header] = value
                    }
                    
                    rows.add(row)
                }
            }
            
            rows
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing timetable CSV", e)
            emptyList()
        }
    }
    
    /**
     * Parse a substitutes CSV file into a map of names to phone numbers
     * 
     * @param inputStream The input stream for the CSV file
     * @return Map of substitute names to phone numbers
     */
    fun parseSubstitutes(inputStream: InputStream): Map<String, String> {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val substitutes = mutableMapOf<String, String>()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                line?.let { 
                    // Skip empty lines
                    if (it.isBlank()) return@let
                    
                    // Remove BOM if present
                    val cleanLine = it.replace("\uFEFF", "")
                    
                    val parts = cleanLine.split(",").map { part -> part.trim() }
                    if (parts.size >= 2) {
                        val name = parts[0].lowercase()
                        val phone = parts[1]
                        substitutes[name] = phone
                    }
                }
            }
            
            substitutes
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing substitutes CSV", e)
            emptyMap()
        }
    }
    
    /**
     * Fix common issues in CSV content
     * 
     * @param content The raw CSV content
     * @return Fixed CSV content
     */
    fun fixCsvContent(content: String): String {
        var fixed = content
        
        // Remove BOM character if present
        fixed = fixed.replace("\uFEFF", "")
        
        // Fix Windows/Mac line endings
        fixed = fixed.replace("\r\n", "\n").replace("\r", "\n")
        
        // Remove quotes from cells
        fixed = fixed.replace("\"", "")
        
        // Fix any consecutive commas
        fixed = fixed.replace(",,", ", ,")
        
        return fixed
    }
}
