package com.example.financesimply.data.db.entity

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromSplitParticipantList(value: List<SplitParticipant>?): String? {
        if (value == null) return null
        val type = object : TypeToken<List<SplitParticipant>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toSplitParticipantList(value: String?): List<SplitParticipant>? {
        if (value == null) return null
        val type = object : TypeToken<List<SplitParticipant>>() {}.type
        return gson.fromJson(value, type)
    }
}
