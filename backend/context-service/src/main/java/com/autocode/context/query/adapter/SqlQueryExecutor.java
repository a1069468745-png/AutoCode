package com.autocode.context.query.adapter;

import java.util.List;
import java.util.Map;

public interface SqlQueryExecutor {
    List<Map<String, Object>> queryForList(String sql, Map<String, ?> params);
}
