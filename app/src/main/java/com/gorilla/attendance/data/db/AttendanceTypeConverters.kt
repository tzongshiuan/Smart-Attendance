package com.gorilla.attendance.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gorilla.attendance.data.model.*

object AttendanceTypeConverters {

    val gson = Gson()

    @TypeConverter
    @JvmStatic
    fun fromString(data: String?): ArrayList<ModulesModes>? {
        val listType = object : TypeToken<ArrayList<ModulesModes>>() {
        }.type
        return gson.fromJson(data, listType)
    }

    @TypeConverter
    @JvmStatic
    fun fromArrayList(modulesModes: ArrayList<ModulesModes>?): String? {
        return gson.toJson(modulesModes)
    }

    @TypeConverter
    @JvmStatic
    fun fromEmployeeString(data: String?): ArrayList<Employees>? {
        val listType = object : TypeToken<ArrayList<Employees>>() {
        }.type
        return gson.fromJson(data, listType)
    }

    @TypeConverter
    @JvmStatic
    fun fromEmployeeArrayList(employees: ArrayList<Employees>?): String? {
        return gson.toJson(employees)
    }

    @TypeConverter
    @JvmStatic
    fun fromVisitorString(data: String?): ArrayList<Visitors>? {
        val listType = object : TypeToken<ArrayList<Visitors>>() {
        }.type
        return gson.fromJson(data, listType)
    }

    @TypeConverter
    @JvmStatic
    fun fromVisitorArrayList(visitors: ArrayList<Visitors>?): String? {
        return gson.toJson(visitors)
    }

    @TypeConverter
    @JvmStatic
    fun fromMarqueeString(data: String?): ArrayList<Marquees>? {
        val listType = object : TypeToken<ArrayList<Marquees>>() {
        }.type
        return gson.fromJson(data, listType)
    }

    @TypeConverter
    @JvmStatic
    fun fromMarqueeArrayList(marquees: ArrayList<Marquees>?): String? {
        return gson.toJson(marquees)
    }

    @TypeConverter
    @JvmStatic
    fun fromVideoString(data: String?): ArrayList<Videos>? {
        val listType = object : TypeToken<ArrayList<Videos>>() {
        }.type
        return gson.fromJson(data, listType)
    }

    @TypeConverter
    @JvmStatic
    fun fromVideoArrayList(videos: ArrayList<Videos>?): String? {
        return gson.toJson(videos)
    }
}