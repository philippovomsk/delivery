package com.philya.delivery.db;

import android.arch.persistence.room.TypeConverter;

import java.util.Date;

public class DbConverters {
    @TypeConverter
    public static Date fromTimestampToDate(long value) {
        return new Date(value);
    }

    @TypeConverter
    public static long fromDateToTimestamp(Date date) {
        return date == null ? 0 : date.getTime();
    }
}
