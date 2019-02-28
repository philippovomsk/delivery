package com.philya.delivery;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory;
import android.arch.persistence.room.testing.MigrationTestHelper;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.philya.delivery.db.Db;
import com.philya.delivery.db.DbMigrations;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    private static final String TEST_DB = "migration-test";

    @Rule
    public MigrationTestHelper helper;

    public ExampleInstrumentedTest() {
        helper = new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
                Db.class.getCanonicalName(),
                new FrameworkSQLiteOpenHelperFactory());
    }

    @Test
    public void migrate2To3() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 2);

        // db has schema version 1. insert some data using SQL queries.
        // You cannot use DAO classes because they expect the latest schema.
        //db.execSQL(...);
        ContentValues cvs = new ContentValues();
        cvs.put("id", "1");
        cvs.put("number", "222");
        cvs.put("date", 2);
        cvs.put("car", "");
        cvs.put("driverId", "");
        cvs.put("complete", false);
        cvs.put("'from'", "");
        db.insert("round", SQLiteDatabase.CONFLICT_REPLACE, cvs);

        cvs.clear();
        cvs.put("owner", "1");
        cvs.put("rowNumber", 1);
        cvs.put("point", "aaa");
        cvs.put("address", "");
        cvs.put("phone", "");
        db.insert("roundrow", SQLiteDatabase.CONFLICT_REPLACE, cvs);

        // Prepare for the next version.
        db.close();

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, DbMigrations.Migration2_3);

        cvs.put("docid", "2222222");
        cvs.put("complete", true);
        db.update("roundrow", SQLiteDatabase.CONFLICT_REPLACE, cvs, "owner = \"1\" AND rowNumber = 1", null);

        Cursor c = db.query("SELECT owner, rowNumber, docid, complete FROM roundrow");
        if(c.moveToFirst()) {
            Assert.assertEquals(1, c.getInt(1));
            Assert.assertEquals( "2222222", c.getString(2));
            Assert.assertEquals( 1, c.getInt(3));
        } else {
            Assert.fail();
        }

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
        db.close();
    }

    @Test
    public void migrate3To4() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 3);

        // db has schema version 1. insert some data using SQL queries.
        // You cannot use DAO classes because they expect the latest schema.
        //db.execSQL(...);
        ContentValues cvs = new ContentValues();
        cvs.put("id", "1");
        cvs.put("number", "222");
        cvs.put("date", 2);
        cvs.put("car", "");
        cvs.put("driverId", "");
        cvs.put("complete", false);
        cvs.put("'from'", "");
        db.insert("round", SQLiteDatabase.CONFLICT_REPLACE, cvs);

        cvs.clear();
        cvs.put("owner", "1");
        cvs.put("rowNumber", 1);
        cvs.put("point", "aaa");
        cvs.put("address", "");
        cvs.put("phone", "");
        cvs.put("docid", "2222222");
        cvs.put("complete", true);
        db.insert("roundrow", SQLiteDatabase.CONFLICT_REPLACE, cvs);

        // Prepare for the next version.
        db.close();

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, DbMigrations.Migration3_4);

        cvs.put("fio", "petrov");
        db.update("roundrow", SQLiteDatabase.CONFLICT_REPLACE, cvs, "owner = \"1\" AND rowNumber = 1", null);

        Cursor c = db.query("SELECT owner, rowNumber, fio FROM roundrow");
        if(c.moveToFirst()) {
            Assert.assertEquals(1, c.getInt(1));
            Assert.assertEquals( "petrov", c.getString(2));
        } else {
            Assert.fail();
        }

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
        db.close();
    }
}
