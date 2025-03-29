package com.substituemanagment.managment.data.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StringListConverter {
    private val gson = Gson()
    private val type = object : TypeToken<List<String>>() {}.type

    @TypeConverter
    fun fromString(value: String): List<String> {
        return if (value.isEmpty()) {
            emptyList()
        } else {
            gson.fromJson(value, type)
        }
    }

    @TypeConverter
    fun toString(list: List<String>): String {
        return gson.toJson(list)
    }
} 