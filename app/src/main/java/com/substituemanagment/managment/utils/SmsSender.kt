package com.substituemanagment.managment.utils

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import com.substituemanagment.managment.data.SmsHistoryDto
import com.substituemanagment.managment.data.SmsDataManager
import com.substituemanagment.managment.ui.viewmodel.SmsViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for sending SMS messages.
 * This implementation logs messages and prepares for actual SMS sending.
 */
object SmsSender {
    private const val TAG = "SmsSender"
    
    /**
     * Send SMS messages to the specified recipients.
     * 
     * @param context The application context
     * @param recipients List of SmsViewModel.TeacherContact objects representing recipients
     * @param message The message content to send
     * @param saveToHistory Whether to save the message to history
     * @return A pair of (success, error message)
     */
    fun sendSms(
        context: Context,
        recipients: List<SmsViewModel.TeacherContact>,
        message: String,
        saveToHistory: Boolean = true
    ): Pair<Boolean, String?> {
        if (recipients.isEmpty()) {
            return Pair(false, "No recipients specified")
        }
        
        if (message.isBlank()) {
            return Pair(false, "Message is empty")
        }
        
        try {
            // Count successful sends
            var successCount = 0
            
            // List to track failed recipients
            val failedRecipients = mutableListOf<String>()
            
            // Process messages for each recipient individually (personalized)
            recipients.forEach { recipient ->
                try {
                    // Process message templates with recipient-specific variables
                    val processedMessage = processMessageVariables(message, recipient.name)
                    
                    // Check message length - standard SMS is 160 characters
                    val messageLength = processedMessage.length
                    val messageParts = (messageLength / 160) + if (messageLength % 160 > 0) 1 else 0
                    Log.d(TAG, "Message length: $messageLength chars (${messageParts} part(s))")
                    
                    Log.d(TAG, "Sending SMS to: ${recipient.name} (${recipient.phone}): $processedMessage")
                    
                    // Simulate SMS sending (would be real in production)
                    // For long messages, we'd use divideMessage:
                    // val parts = smsManager.divideMessage(processedMessage)
                    // smsManager.sendMultipartTextMessage(recipient.phone, null, parts, null, null)
                    
                    // For simple messages:
                    // smsManager.sendTextMessage(recipient.phone, null, processedMessage, null, null)
                    
                    // Simulate successful send for most recipients
                    val randomSuccess = Math.random() > 0.1 // 90% success rate for simulation
                    if (randomSuccess) {
                        successCount++
                        
                        // Save individual SMS to history
                        if (saveToHistory) {
                            val smsId = UUID.randomUUID().toString()
                            val timestamp = System.currentTimeMillis()
                            
                            val historyEntry = SmsHistoryDto(
                                id = smsId,
                                recipients = listOf(recipient.name),
                                message = processedMessage,
                                timestamp = timestamp,
                                status = "Sent"
                            )
                            
                            SmsDataManager.addSmsToHistory(context, historyEntry)
                        }
                    } else {
                        failedRecipients.add(recipient.name)
                        
                        if (saveToHistory) {
                            val smsId = UUID.randomUUID().toString()
                            val timestamp = System.currentTimeMillis()
                            
                            val historyEntry = SmsHistoryDto(
                                id = smsId,
                                recipients = listOf(recipient.name),
                                message = processedMessage,
                                timestamp = timestamp,
                                status = "Failed"
                            )
                            
                            SmsDataManager.addSmsToHistory(context, historyEntry)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send SMS to ${recipient.phone}: ${e.message}")
                    failedRecipients.add(recipient.name)
                }
            }
            
            // Determine overall status
            val overallStatus = if (failedRecipients.isEmpty()) "Sent" else "Partial"
            val statusMessage = if (failedRecipients.isEmpty()) 
                "SMS sent to all recipients" 
            else 
                "Failed to send to: ${failedRecipients.joinToString(", ")}"
            
            Log.i(TAG, "SMS sending completed: $successCount/${recipients.size} successful")
            return Pair(successCount > 0, if (successCount == 0) "Failed to send any messages" else null)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in SMS sending process: ${e.message}")
            return Pair(false, "SMS sending error: ${e.message}")
        }
    }
    
    /**
     * Process message template variables and replace them with actual values.
     * 
     * @param message The message template with variables
     * @param recipientName The name of the teacher receiving the SMS
     * @return The processed message with all variables replaced
     */
    private fun processMessageVariables(message: String, recipientName: String = ""): String {
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault()) // Full day name e.g., "Monday"
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val today = Date()
        
        // Mock data for substitution details (in a real app, these would come from actual data)
        val className = getRandomClassName()
        val originalTeacher = getRandomTeacherName()
        val roomNumber = "Room " + (101 + Random().nextInt(20))
        
        // The substitute teacher name is the SMS recipient
        val substituteTeacher = if (recipientName.isNotEmpty()) recipientName else "Teacher"
        
        // Replace template variables
        return message
            .replace("{date}", dateFormat.format(today))
            .replace("{day}", dayFormat.format(today))
            .replace("{time}", timeFormat.format(today))
            .replace("{period}", getCurrentPeriod())
            .replace("{room}", roomNumber)
            .replace("{class}", className)
            .replace("{original_teacher}", originalTeacher)
            .replace("{substitute_teacher}", substituteTeacher)
    }
    
    /**
     * Get a random class name for simulation purposes.
     */
    private fun getRandomClassName(): String {
        val subjects = listOf(
            "Mathematics", "Physics", "Chemistry", "Biology", 
            "English", "History", "Geography", "Computer Science",
            "Art", "Music", "Physical Education", "Economics"
        )
        val grades = listOf("9A", "9B", "10A", "10B", "11A", "11B", "12A", "12B")
        
        return "${subjects[Random().nextInt(subjects.size)]} ${grades[Random().nextInt(grades.size)]}"
    }
    
    /**
     * Get a random teacher name for simulation purposes.
     */
    private fun getRandomTeacherName(): String {
        val teachers = listOf(
            "Mr. Smith", "Mrs. Johnson", "Dr. Williams", "Ms. Brown",
            "Mr. Jones", "Mrs. Miller", "Dr. Davis", "Mr. Wilson"
        )
        
        return teachers[Random().nextInt(teachers.size)]
    }
    
    /**
     * Get the current period based on time of day.
     * In a real implementation, this would use the school's actual period schedule.
     */
    private fun getCurrentPeriod(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        return when (hour) {
            in 8..8 -> "1st"
            in 9..9 -> "2nd"
            in 10..10 -> "3rd"
            in 11..11 -> "4th"
            in 12..12 -> "5th"
            in 13..13 -> "6th"
            in 14..14 -> "7th"
            in 15..15 -> "8th"
            else -> "N/A"
        }
    }
} 