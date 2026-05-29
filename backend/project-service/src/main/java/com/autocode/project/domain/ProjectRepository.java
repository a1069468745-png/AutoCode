package com.autocode.project.domain;

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
public class ProjectRepository {

    private static final String DEFAULT_STATUS = "CREATED";

    private final JdbcClient jdbcClient;

    public ProjectRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public ProjectRecord create(String name,
                                String repoUrl,
                                String defaultBranch,
                                String languageStack,
                                String docRepoPath) throws DataIntegrityViolationException {
        HashMap<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("repoUrl", repoUrl);
        params.put("defaultBranch", defaultBranch);
        params.put("languageStack", languageStack);
        params.put("docRepoPath", docRepoPath);
        params.put("status", DEFAULT_STATUS);

        return jdbcClient.sql("""
                insert into app.projects (
                    name,
                    repo_url,
                    default_branch,
                    language_stack,
                    doc_repo_path,
                    status,
                    index_error
                ) values (
                    :name,
                    :repoUrl,
                    :defaultBranch,
                    :languageStack,
                    :docRepoPath,
                    :status,
                    null
                )
                returning id, name, repo_url, default_branch, language_stack, doc_repo_path, status, index_error, created_at, updated_at
                """)
                .params(params)
                .query(this::mapRecord)
                .single();
    }

    public List<ProjectRecord> findAll() {
        return jdbcClient.sql("""
                select id, name, repo_url, default_branch, language_stack, doc_repo_path, status, index_error, created_at, updated_at
                from app.projects
                order by id desc
                """)
                .query(this::mapRecord)
                .list();
    }

    public Optional<ProjectRecord> findById(long id) {
        return jdbcClient.sql("""
                select id, name, repo_url, default_branch, language_stack, doc_repo_path, status, index_error, created_at, updated_at
                from app.projects
                where id = :id
                """)
                .param("id", id)
                .query(this::mapRecord)
                .optional();
    }

    public void updateStatus(long id, String status) {
        jdbcClient.sql("""
                update app.projects
                set status = :status,
                    updated_at = now()
                where id = :id
                """)
                .param("id", id)
                .param("status", status)
                .update();
    }

    public void updateStatusWithError(long id, String status, String indexError) {
        jdbcClient.sql("""
                update app.projects
                set status = :status,
                    index_error = :indexError,
                    updated_at = now()
                where id = :id
                """)
                .param("id", id)
                .param("status", status)
                .param("indexError", indexError)
                .update();
    }

    private ProjectRecord mapRecord(ResultSet resultSet, int rowNum) throws SQLException {
        return new ProjectRecord(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("repo_url"),
                resultSet.getString("default_branch"),
                resultSet.getString("language_stack"),
                resultSet.getString("doc_repo_path"),
                resultSet.getString("status"),
                resultSet.getString("index_error"),
                resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("updated_at", OffsetDateTime.class).toInstant()
        );
    }
}
