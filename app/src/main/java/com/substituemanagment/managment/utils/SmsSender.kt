package com.substituemanagment.managment.utils

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.substituemanagment.managment.data.SmsHistoryDto
import com.substituemanagment.managment.data.SmsDataManager
import com.substituemanagment.managment.data.TeacherContactDto
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for sending SMS messages.
 * This implementation uses the device's SMS functionality.
 */
object SmsSender {
    private const val TAG = "SmsSender"
    private const val SMS_SENT_ACTION = "SMS_SENT"
    private const val SMS_DELIVERED_ACTION = "SMS_DELIVERED"
    private const val SMS_PERMISSION_REQUEST_CODE = 101
    
    /**
     * Check if the app has the necessary permissions to send SMS.
     * This should be called before attempting to send SMS.
     *
     * @param context The application context
     * @param activity The activity from which to request permissions if needed
     * @return True if permissions are granted, false otherwise
     */
    fun checkSmsPermission(context: Context, activity: Activity?): Boolean {
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasSmsPermission && activity != null) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST_CODE
            )
            return false
        }
        
        return hasSmsPermission
    }
    
    /**
     * Send SMS messages to the specified recipients.
     * 
     * @param context The application context
     * @param recipients List of TeacherContactDto objects representing recipients
     * @param message The message content to send
     * @param saveToHistory Whether to save the message to history
     * @return A pair of (success, error message)
     */
    fun sendSms(
        context: Context,
        recipients: List<TeacherContactDto>,
        message: String,
        saveToHistory: Boolean = true
    ): Pair<Boolean, String?> {
        if (recipients.isEmpty()) {
            return Pair(false, "No recipients specified")
        }
        
        if (message.isBlank()) {
            return Pair(false, "Message is empty")
        }
        
        // Check if we have permission to send SMS
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED) {
            return Pair(false, "SMS permission not granted")
        }
        
        try {
            // Get the SMS manager
            val smsManager = SmsManager.getDefault()
            
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
                    
                    // Generate unique IDs for this SMS
                    val smsId = UUID.randomUUID().toString()
                    val timestamp = System.currentTimeMillis()
                    
                    try {
                        // Create sent and delivered intents
                        val sentPI = PendingIntent.getBroadcast(
                            context, 0, Intent(SMS_SENT_ACTION),
                            PendingIntent.FLAG_IMMUTABLE
                        )
                        
                        val deliveredPI = PendingIntent.getBroadcast(
                            context, 0, Intent(SMS_DELIVERED_ACTION),
                            PendingIntent.FLAG_IMMUTABLE
                        )
                        
                        // Register broadcast receivers for sent and delivered status
                        context.registerReceiver(object : BroadcastReceiver() {
                            override fun onReceive(context: Context, intent: Intent) {
                                when (resultCode) {
                                    Activity.RESULT_OK -> {
                                        Log.d(TAG, "SMS sent successfully to ${recipient.phone}")
                                        successCount++
                                        
                                        // Save to history
                                        if (saveToHistory) {
                                            val historyEntry = SmsHistoryDto(
                                                id = smsId,
                                                recipients = listOf(recipient.name),
                                                message = processedMessage,
                                                timestamp = timestamp,
                                                status = "Sent"
                                            )
                                            SmsDataManager.addSmsToHistory(context, historyEntry)
                                        }
                                    }
                                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                                        Log.e(TAG, "Generic failure for ${recipient.phone}")
                                        failedRecipients.add(recipient.name)
                                        saveFailedSms(context, smsId, recipient.name, processedMessage, timestamp, saveToHistory)
                                    }
                                    SmsManager.RESULT_ERROR_NO_SERVICE -> {
                                        Log.e(TAG, "No service for ${recipient.phone}")
                                        failedRecipients.add(recipient.name)
                                        saveFailedSms(context, smsId, recipient.name, processedMessage, timestamp, saveToHistory)
                                    }
                                    SmsManager.RESULT_ERROR_NULL_PDU -> {
                                        Log.e(TAG, "Null PDU for ${recipient.phone}")
                                        failedRecipients.add(recipient.name)
                                        saveFailedSms(context, smsId, recipient.name, processedMessage, timestamp, saveToHistory)
                                    }
                                    SmsManager.RESULT_ERROR_RADIO_OFF -> {
                                        Log.e(TAG, "Radio off for ${recipient.phone}")
                                        failedRecipients.add(recipient.name)
                                        saveFailedSms(context, smsId, recipient.name, processedMessage, timestamp, saveToHistory)
                                    }
                                }
                                context.unregisterReceiver(this)
                            }
                        }, IntentFilter(SMS_SENT_ACTION), Context.RECEIVER_NOT_EXPORTED)
                        
                        context.registerReceiver(object : BroadcastReceiver() {
                            override fun onReceive(context: Context, intent: Intent) {
                                when (resultCode) {
                                    Activity.RESULT_OK -> {
                                        Log.d(TAG, "SMS delivered to ${recipient.phone}")
                                        // Update status if needed
                                    }
                                    Activity.RESULT_CANCELED -> {
                                        Log.d(TAG, "SMS not delivered to ${recipient.phone}")
                                        // Update status if needed
                                    }
                                }
                                context.unregisterReceiver(this)
                            }
                        }, IntentFilter(SMS_DELIVERED_ACTION), Context.RECEIVER_NOT_EXPORTED)
                        
                        // Send the SMS using SmsManager API
                        if (messageLength > 160) {
                            // For longer messages, use divideMessage
                            val messageParts = smsManager.divideMessage(processedMessage)
                            val sentIntents = ArrayList<PendingIntent>()
                            val deliveredIntents = ArrayList<PendingIntent>()
                            
                            for (i in messageParts.indices) {
                                sentIntents.add(sentPI)
                                deliveredIntents.add(deliveredPI)
                            }
                            
                            smsManager.sendMultipartTextMessage(
                                recipient.phone,
                                null,
                                messageParts,
                                sentIntents,
                                deliveredIntents
                            )
                        } else {
                            // For short messages, use sendTextMessage
                            smsManager.sendTextMessage(
                                recipient.phone,
                                null,
                                processedMessage,
                                sentPI,
                                deliveredPI
                            )
                        }
                        
                        // Show a toast notification
                        Toast.makeText(context, "Sending SMS to ${recipient.name}...", Toast.LENGTH_SHORT).show()
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send SMS to ${recipient.phone}: ${e.message}")
                        failedRecipients.add(recipient.name)
                        
                        if (saveToHistory) {
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
                    Log.e(TAG, "Failed to process SMS to ${recipient.phone}: ${e.message}")
                    failedRecipients.add(recipient.name)
                }
            }
            
            return Pair(true, "Sending SMS messages...")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in SMS sending process: ${e.message}")
            return Pair(false, "SMS sending error: ${e.message}")
        }
    }
    
    /**
     * Helper function to save a failed SMS to history
     */
    private fun saveFailedSms(
        context: Context, 
        smsId: String, 
        recipientName: String, 
        message: String, 
        timestamp: Long, 
        saveToHistory: Boolean
    ) {
        if (saveToHistory) {
            val historyEntry = SmsHistoryDto(
                id = smsId,
                recipients = listOf(recipientName),
                message = message,
                timestamp = timestamp,
                status = "Failed"
            )
            
            SmsDataManager.addSmsToHistory(context, historyEntry)
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
    
    /**
     * Send a single SMS message to one teacher.
     *
     * @param context The application context
     * @param phoneNumber The recipient's phone number
     * @param message The message content to send (already processed with real data)
     * @return true if the message was sent successfully, false otherwise
     */
    fun sendSingleSms(
        context: Context,
        phoneNumber: String,
        message: String
    ): Boolean {
        if (phoneNumber.isBlank()) {
            Log.e(TAG, "Phone number is empty")
            return false
        }
        
        if (message.isBlank()) {
            Log.e(TAG, "Message is empty")
            return false
        }
        
        // Check if we have permission to send SMS
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SMS permission not granted")
            return false
        }
        
        try {
            // Get the SMS manager
            val smsManager = SmsManager.getDefault()
            
            // Since Android uses multithreaded operation, we use this to track completion
            val messageSent = mutableListOf<Boolean>()
            val messageDelivered = mutableListOf<Boolean>()
            
            // Synchronization to wait for callbacks
            val lock = Object()
            
            // Create sent and delivered intents
            val sentPI = PendingIntent.getBroadcast(
                context, 0, Intent(SMS_SENT_ACTION),
                PendingIntent.FLAG_IMMUTABLE
            )
            
            val deliveredPI = PendingIntent.getBroadcast(
                context, 0, Intent(SMS_DELIVERED_ACTION),
                PendingIntent.FLAG_IMMUTABLE
            )
            
            // Register broadcast receivers for sent status
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    synchronized(lock) {
                        when (resultCode) {
                            Activity.RESULT_OK -> {
                                Log.d(TAG, "SMS sent successfully to $phoneNumber")
                                messageSent.add(true)
                            }
                            else -> {
                                Log.e(TAG, "Failed to send SMS to $phoneNumber, result code: $resultCode")
                                messageSent.add(false)
                            }
                        }
                        lock.notify()
                    }
                    context.unregisterReceiver(this)
                }
            }, IntentFilter(SMS_SENT_ACTION), Context.RECEIVER_NOT_EXPORTED)
            
            // Register broadcast receivers for delivered status
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    synchronized(lock) {
                        when (resultCode) {
                            Activity.RESULT_OK -> {
                                Log.d(TAG, "SMS delivered to $phoneNumber")
                                messageDelivered.add(true)
                            }
                            else -> {
                                Log.d(TAG, "SMS not delivered to $phoneNumber")
                                messageDelivered.add(false)
                            }
                        }
                        lock.notify()
                    }
                    context.unregisterReceiver(this)
                }
            }, IntentFilter(SMS_DELIVERED_ACTION), Context.RECEIVER_NOT_EXPORTED)
            
            // Send the SMS based on message length
            val messageLength = message.length
            if (messageLength > 160) {
                // For longer messages, use divideMessage
                val messageParts = smsManager.divideMessage(message)
                val sentIntents = ArrayList<PendingIntent>()
                val deliveredIntents = ArrayList<PendingIntent>()
                
                for (i in messageParts.indices) {
                    sentIntents.add(sentPI)
                    deliveredIntents.add(deliveredPI)
                }
                
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    messageParts,
                    sentIntents,
                    deliveredIntents
                )
            } else {
                // For short messages, use sendTextMessage
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    sentPI,
                    deliveredPI
                )
            }
            
            // Show a toast notification
            Toast.makeText(context, "Sending SMS to $phoneNumber...", Toast.LENGTH_SHORT).show()
            
            // Wait for the callbacks with a timeout
            synchronized(lock) {
                try {
                    lock.wait(5000) // Wait for 5 seconds max
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Wait interrupted", e)
                }
            }
            
            // Return success if the message was sent
            return messageSent.isNotEmpty() && messageSent.first()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS: ${e.message}", e)
            return false
        }
    }
} 