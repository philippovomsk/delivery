package com.philya.delivery.db;

import android.arch.persistence.room.*;
import io.reactivex.Flowable;
import io.reactivex.Maybe;

import java.util.List;

@Dao
public abstract class RoundDocDAO implements ExchangeDAO<RoundDoc> {

    @Delete
    protected abstract void delete(Round doc);

    @Query("DELETE FROM roundrow WHERE owner = :id")
    protected abstract void deleteRows(String id);

    @Insert(onConflict = OnConflictStrategy.FAIL)
    protected abstract void insert(Round doc);

    @Insert(onConflict = OnConflictStrategy.FAIL)
    protected abstract void insertRows(List<RoundRow> docRows);

    @Update(onConflict = OnConflictStrategy.FAIL)
    protected abstract void update(Round doc);

    @Override
    @Transaction
    public void deleteAll(List<RoundDoc> items) {
        for (RoundDoc doc : items) {
            delete(doc.head);
        }
    }

    @Override
    @Transaction
    public void insertAll(List<RoundDoc> items) {
        for (RoundDoc doc : items) {
            deleteRows(doc.getId());
            insert(doc.head);
            insertRows(doc.rows);
        }
    }

    @Override
    @Transaction
    public void updateAll(List<RoundDoc> items) {
        for (RoundDoc doc : items) {
            deleteRows(doc.getId());
            update(doc.head);
            insertRows(doc.rows);
        }

    }

    @Transaction
    public void update(RoundDoc doc) {
        deleteRows(doc.getId());
        update(doc.head);
        insertRows(doc.rows);
    }

    @Override
    @Transaction
    @Query("SELECT * FROM round")
    public abstract List<RoundDoc> getAll();

    @Transaction
    @Query("SELECT * FROM round WHERE driverId = :driverId ORDER BY date, number")
    public abstract Flowable<List<RoundDoc>> listAll(String driverId);

    @Transaction
    @Query("SELECT * FROM round WHERE driverId = :driverId ORDER BY date, number")
    public abstract List<RoundDoc> getForSend(String driverId);

    @Query("SELECT COUNT(id) FROM round WHERE driverId = ''")
    public abstract Flowable<Integer> countFreeRounds();

    @Transaction
    @Query("SELECT * FROM round WHERE driverId = '' ORDER BY date, number")
    public abstract Flowable<List<RoundDoc>> listAllFree();
}
