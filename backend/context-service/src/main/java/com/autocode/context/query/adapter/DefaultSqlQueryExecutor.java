package com.autocode.context.query.adapter;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DefaultSqlQueryExecutor implements SqlQueryExecutor {
    @Override
    public List<Map<String, Object>> queryForList(String sql, Map<String, ?> params) {
        return List.of();
    }
}
