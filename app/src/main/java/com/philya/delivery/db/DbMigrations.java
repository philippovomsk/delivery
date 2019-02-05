package com.philya.delivery.db;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.migration.Migration;
import android.support.annotation.NonNull;

public class DbMigrations {
    public static Migration Migration1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase supportSQLiteDatabase) {
            supportSQLiteDatabase.execSQL("ALTER TABLE round ADD COLUMN 'from' TEXT;");
        }
    };

    public static Migration Migration2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase supportSQLiteDatabase) {
            supportSQLiteDatabase.execSQL("ALTER TABLE roundrow ADD COLUMN 'docid' TEXT;");
            supportSQLiteDatabase.execSQL("ALTER TABLE roundrow ADD COLUMN 'complete' INTEGER NOT NULL DEFAULT 0;");
        }
    };
}
