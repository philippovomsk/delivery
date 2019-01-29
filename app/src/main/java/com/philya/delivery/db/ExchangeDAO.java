package com.philya.delivery.db;

import java.util.List;

public interface ExchangeDAO<T extends EntityId> {

    void insertAll(List<T> items);

    void deleteAll(List<T> items);

    void updateAll(List<T> items);

    List<T> getAll();
}
