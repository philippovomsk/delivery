package com.philya.delivery.db;

import android.arch.persistence.room.*;
import io.reactivex.Flowable;
import io.reactivex.Maybe;

import java.util.List;

@Dao
public abstract class DriverDAO implements ExchangeDAO<Driver> {
    @Query("DELETE FROM driver")
    public abstract void clear();

    @Override
    @Delete
    public abstract void deleteAll(List<Driver> items);

    @Override
    @Insert(onConflict = OnConflictStrategy.FAIL)
    public abstract void insertAll(List<Driver> items);

    @Override
    @Update(onConflict = OnConflictStrategy.FAIL)
    public abstract void updateAll(List<Driver> items);

    @Query("SELECT * FROM driver ORDER BY name")
    public abstract Flowable<List<Driver>> listAll();

    @Override
    @Query("SELECT * FROM driver")
    public abstract List<Driver> getAll();

    @Query("SELECT * FROM driver WHERE id = :id")
    public abstract Maybe<Driver> getById(String id);

}
