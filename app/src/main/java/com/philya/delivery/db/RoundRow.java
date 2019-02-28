package com.philya.delivery.db;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.support.annotation.NonNull;

import java.io.Serializable;

import static android.arch.persistence.room.ForeignKey.*;

@Entity(tableName = "roundrow", primaryKeys = {"owner", "rowNumber"},
        foreignKeys = {@ForeignKey(entity = Round.class, parentColumns = "id", childColumns = "owner", onDelete = CASCADE)})
public class RoundRow implements Serializable {

    @NonNull
    public String owner;

    @NonNull
    public int rowNumber;

    public String point;

    public String address;

    public String phone;

    public String docid;

    public boolean complete = false;

    public String fio;

}
