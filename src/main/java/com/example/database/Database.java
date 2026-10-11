package com.example.database;

import java.util.List;
import java.util.Map;

public interface Database {

    int insert(String table, Map<String, Object> data);

    List<Map<String, Object>> findAll(String table);

    Map<String, Object> findById(String table, Object id);

    int update(String table, Object id, Map<String, Object> data);

    int delete(String table, Object id);
}