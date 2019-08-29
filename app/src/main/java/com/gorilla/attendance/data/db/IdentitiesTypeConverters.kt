package com.gorilla.attendance.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gorilla.attendance.data.model.Employees

object IdentitiesTypeConverters {
    @TypeConverter
    @JvmStatic
    fun fromString(data: String?): ArrayList<Employees>? {
        var gson = Gson()
        val listType = object : TypeToken<ArrayList<Employees>>() {
        }.type
        return gson.fromJson(data, listType)
    }

    @TypeConverter
    @JvmStatic
    fun fromArrayList(employees: ArrayList<Employees>?): String? {
        var gson = Gson()
        return gson.toJson(employees)
    }
}