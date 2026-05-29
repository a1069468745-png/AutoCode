package com.autocode.llm.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class LlmCallLogger {

    private static final Logger log = LoggerFactory.getLogger(LlmCallLogger.class);

    private final JdbcClient jdbcClient;

    public LlmCallLogger(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void logCall(long projectId, String userId, String queryType, String queryText, String modelUsed, long costToken) {
        try {
            jdbcClient.sql("""
                    insert into app.query_logs (
                        project_id,
                        user_id,
                        query_type,
                        query_text,
                        model_used,
                        cost_token
                    ) values (
                        :projectId,
                        :userId,
                        :queryType,
                        :queryText,
                        :modelUsed,
                        :costToken
                    )
                    """)
                    .param("projectId", projectId)
                    .param("userId", userId)
                    .param("queryType", queryType)
                    .param("queryText", queryText)
                    .param("modelUsed", modelUsed)
                    .param("costToken", costToken)
                    .update();
        } catch (Exception e) {
            log.warn("Failed to persist LLM call audit log: {}", e.getMessage());
        }
    }
}
