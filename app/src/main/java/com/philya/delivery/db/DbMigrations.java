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

    public static Migration Migration3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase supportSQLiteDatabase) {
            supportSQLiteDatabase.execSQL("ALTER TABLE roundrow ADD COLUMN 'fio' TEXT;");
        }
    };

    public static Migration Migration4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase supportSQLiteDatabase) {
            supportSQLiteDatabase.execSQL("ALTER TABLE round ADD COLUMN 'contractPrice' INTEGER NOT NULL DEFAULT 0;");
            supportSQLiteDatabase.execSQL("ALTER TABLE round ADD COLUMN 'weight' INTEGER NOT NULL DEFAULT 0;");
            supportSQLiteDatabase.execSQL("ALTER TABLE round ADD COLUMN 'mileage' INTEGER NOT NULL DEFAULT 0;");
        }
    };
}
