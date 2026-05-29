package com.autocode.context.query.adapter;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DefaultSqlQueryExecutor implements SqlQueryExecutor {
    private final JdbcClient jdbcClient;

    public DefaultSqlQueryExecutor(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Map<String, ?> params) {
        // Adapters share this executor so every query route goes through the same JDBC parameter binding path.
        return jdbcClient.sql(sql)
                .params(params)
                .query()
                .listOfRows();
    }
}
