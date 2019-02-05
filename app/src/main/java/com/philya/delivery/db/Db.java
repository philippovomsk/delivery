package com.philya.delivery.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

@Database(entities = {Driver.class, Round.class, RoundRow.class}, version = 3, exportSchema = true)
@TypeConverters({DbConverters.class})
public abstract class Db extends RoomDatabase {
    public abstract DriverDAO driverDAO();

    public abstract RoundDocDAO roundDocDAO();
}
