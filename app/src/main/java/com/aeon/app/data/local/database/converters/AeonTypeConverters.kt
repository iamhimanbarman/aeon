package com.aeon.app.data.local.database.converters

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/*
 * AEON TYPE CONVERTERS
 *
 * Room only stores primitive-safe values.
 * These converters keep domain models expressive while database storage stays stable.
 */


class AeonTypeConverters {

    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? {
        return value?.toEpochMilli()
    }

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? {
        return value?.let(Instant::ofEpochMilli)
    }

    @TypeConverter
    fun localDateToIso(value: LocalDate?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun isoToLocalDate(value: String?): LocalDate? {
        return value?.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
    }

    @TypeConverter
    fun localTimeToIso(value: LocalTime?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun isoToLocalTime(value: String?): LocalTime? {
        return value?.takeIf { it.isNotBlank() }?.let(LocalTime::parse)
    }

    @TypeConverter
    fun bigDecimalToString(value: BigDecimal?): String? {
        return value?.toPlainString()
    }

    @TypeConverter
    fun stringToBigDecimal(value: String?): BigDecimal? {
        return value
            ?.takeIf { it.isNotBlank() }
            ?.let(::BigDecimal)
    }

    @TypeConverter
    fun stringListToStorage(value: List<String>?): String? {
        return value
            ?.map { it.replace("|", "\\|") }
            ?.joinToString(separator = "|")
    }

    @TypeConverter
    fun storageToStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()

        return value
            .split("|")
            .map { it.replace("\\|", "|") }
            .filter { it.isNotBlank() }
    }
}
