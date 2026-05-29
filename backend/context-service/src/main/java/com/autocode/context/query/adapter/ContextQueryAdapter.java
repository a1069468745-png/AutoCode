package com.autocode.context.query.adapter;

import com.autocode.context.query.ContextQueryRequest;
import com.autocode.context.query.QueryIntent;

public interface ContextQueryAdapter {
    boolean supports(QueryIntent intent);

    StandardQueryResult query(ContextQueryRequest request);
}
