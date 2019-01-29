package com.philya.delivery.db;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "round")
public class Round implements Serializable {

    @PrimaryKey
    @NonNull
    public String id;

    public String number;

    public Date date;

    public String car;

    public String driverId;

    public boolean complete = false;

    public String from;

}
