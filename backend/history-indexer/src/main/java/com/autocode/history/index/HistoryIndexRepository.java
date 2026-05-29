package com.autocode.history.index;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

@Repository
public class HistoryIndexRepository {

    private final JdbcClient jdbcClient;

    public HistoryIndexRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional
    public void deleteProjectData(long projectId) {
        jdbcClient.sql("delete from app.commit_symbols where project_id = :projectId")
                .param("projectId", projectId)
                .update();
        jdbcClient.sql("delete from app.commits where project_id = :projectId")
                .param("projectId", projectId)
                .update();
    }

    public long insertCommit(long projectId, String hash, String author, String commitTime,
                             String message, String branchName) {
        var params = new HashMap<String, Object>();
        params.put("projectId", projectId);
        params.put("commitHash", hash);
        params.put("author", author);
        params.put("commitTime", commitTime);
        params.put("message", message);
        params.put("branchName", branchName);

        return jdbcClient.sql("""
                insert into app.commits (
                    project_id, commit_hash, author, commit_time, message, branch_name
                ) values (
                    :projectId, :commitHash, :author, :commitTime::timestamptz, :message, :branchName
                )
                on conflict (project_id, commit_hash) do update
                    set author = excluded.author,
                        commit_time = excluded.commit_time,
                        message = excluded.message,
                        branch_name = excluded.branch_name
                returning id
                """)
                .params(params)
                .query((rs, rowNum) -> rs.getLong("id"))
                .single();
    }

    public long findSymbolId(long projectId, String name, String filePath) {
        return jdbcClient.sql("""
                select id from app.symbols
                where project_id = :projectId and name = :name and file_path = :filePath
                limit 1
                """)
                .param("projectId", projectId)
                .param("name", name)
                .param("filePath", filePath)
                .query((rs, rowNum) -> rs.getLong("id"))
                .optional()
                .orElse(0L);
    }

    public void insertCommitSymbol(long projectId, long commitId, long symbolId, String changeType) {
        var params = new HashMap<String, Object>();
        params.put("projectId", projectId);
        params.put("commitId", commitId);
        params.put("symbolId", symbolId);
        params.put("changeType", changeType);

        jdbcClient.sql("""
                insert into app.commit_symbols (
                    project_id, commit_id, symbol_id, change_type
                ) values (
                    :projectId, :commitId, :symbolId, :changeType
                )
                """)
                .params(params)
                .update();
    }
}