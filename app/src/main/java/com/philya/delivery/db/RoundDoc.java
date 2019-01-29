package com.philya.delivery.db;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import java.io.Serializable;
import java.util.List;

public class RoundDoc implements Serializable, EntityId {
    @Embedded
    public Round head;

    @Relation(parentColumn = "id", entityColumn = "owner", entity = RoundRow.class)
    public List<RoundRow> rows;

    @Override
    public String getId() {
        return head.id;
    }
}
