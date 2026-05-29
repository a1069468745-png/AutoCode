package com.autocode.llm.domain;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Repository
public class LlmModelProfileRepository {

    private final JdbcClient jdbcClient;

    public LlmModelProfileRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public LlmModelProfile upsert(long projectId,
                                   String provider,
                                   String baseUrl,
                                   String modelName,
                                   String embeddingModel,
                                   int timeoutSeconds,
                                   String fallbackModel,
                                   boolean enableLocalOnly) throws DataIntegrityViolationException {
        HashMap<String, Object> params = new HashMap<>();
        params.put("projectId", projectId);
        params.put("provider", provider);
        params.put("baseUrl", baseUrl);
        params.put("modelName", modelName);
        params.put("embeddingModel", embeddingModel);
        params.put("timeoutSeconds", timeoutSeconds);
        params.put("fallbackModel", fallbackModel);
        params.put("enableLocalOnly", enableLocalOnly);

        return jdbcClient.sql("""
                insert into app.model_profiles (
                    project_id,
                    provider,
                    base_url,
                    model_name,
                    embedding_model,
                    timeout_seconds,
                    fallback_model,
                    enable_local_only
                ) values (
                    :projectId,
                    :provider,
                    :baseUrl,
                    :modelName,
                    :embeddingModel,
                    :timeoutSeconds,
                    :fallbackModel,
                    :enableLocalOnly
                )
                on conflict (project_id) do update set
                    provider = excluded.provider,
                    base_url = excluded.base_url,
                    model_name = excluded.model_name,
                    embedding_model = excluded.embedding_model,
                    timeout_seconds = excluded.timeout_seconds,
                    fallback_model = excluded.fallback_model,
                    enable_local_only = excluded.enable_local_only,
                    updated_at = now()
                returning id, project_id, provider, base_url, model_name, embedding_model, timeout_seconds, fallback_model, enable_local_only, created_at, updated_at
                """)
                .params(params)
                .query(this::mapRecord)
                .single();
    }

    public Optional<LlmModelProfile> findByProjectId(long projectId) {
        return jdbcClient.sql("""
                select id, project_id, provider, base_url, model_name, embedding_model, timeout_seconds, fallback_model, enable_local_only, created_at, updated_at
                from app.model_profiles
                where project_id = :projectId
                """)
                .param("projectId", projectId)
                .query(this::mapRecord)
                .optional();
    }

    public List<LlmModelProfile> findAll() {
        return jdbcClient.sql("""
                select id, project_id, provider, base_url, model_name, embedding_model, timeout_seconds, fallback_model, enable_local_only, created_at, updated_at
                from app.model_profiles
                order by id desc
                """)
                .query(this::mapRecord)
                .list();
    }

    public void deleteByProjectId(long projectId) {
        jdbcClient.sql("delete from app.model_profiles where project_id = :projectId")
                .param("projectId", projectId)
                .update();
    }

    private LlmModelProfile mapRecord(ResultSet resultSet, int rowNum) throws SQLException {
        return new LlmModelProfile(
                resultSet.getLong("id"),
                resultSet.getLong("project_id"),
                resultSet.getString("provider"),
                resultSet.getString("base_url"),
                resultSet.getString("model_name"),
                resultSet.getString("embedding_model"),
                resultSet.getInt("timeout_seconds"),
                resultSet.getString("fallback_model"),
                resultSet.getBoolean("enable_local_only"),
                resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("updated_at", OffsetDateTime.class).toInstant()
        );
    }
}
