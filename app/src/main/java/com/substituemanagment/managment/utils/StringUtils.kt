package com.substituemanagment.managment.utils

import java.util.Locale

/**
 * Extension function to capitalize the first letter of each word in a string
 */
fun String.capitalizeWords(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
    }
} 