package com.philya.delivery.db;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.io.Serializable;

@Entity(tableName = "driver")
public class Driver implements Serializable, EntityId {
    @PrimaryKey
    @NonNull
    public String id;

    public String name;

    public String password;

    @Override
    public String getId() {
        return id;
    }
}
