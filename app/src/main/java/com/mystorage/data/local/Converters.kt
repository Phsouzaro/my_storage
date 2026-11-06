package com.mystorage.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

/** LocalDate é salvo como epochDay (Long), o que mantém ordenação e comparação em SQL. */
class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)
}
