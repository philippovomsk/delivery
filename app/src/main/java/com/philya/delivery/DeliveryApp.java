package com.philya.delivery;

import android.app.Application;
import android.arch.persistence.room.Room;
import android.preference.PreferenceManager;
import com.philya.delivery.db.Db;

import static com.philya.delivery.db.DbMigrations.Migration1_2;
import static com.philya.delivery.db.DbMigrations.Migration2_3;
import static com.philya.delivery.db.DbMigrations.Migration3_4;

public class DeliveryApp extends Application {

    private Db database;

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.settingsitems, false);

        Exchange.startExchangeJob(getApplicationContext(), 0);
    }

    public Db getDatabase() {
        if(database == null) {
            database = Room.databaseBuilder(getApplicationContext(), Db.class, "deliverydb").
                    addMigrations(Migration1_2, Migration2_3, Migration3_4).build();
        }

        return database;
    }
}
