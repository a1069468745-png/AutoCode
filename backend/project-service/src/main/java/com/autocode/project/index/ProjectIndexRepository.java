package com.autocode.project.index;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectIndexRepository {

    private final JdbcClient jdbcClient;

    public ProjectIndexRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void deleteProjectIndexData(long projectId) {
        // Delete link tables first so a sync can rebuild the whole project snapshot idempotently.
        delete("delete from app.document_links where project_id = :projectId", projectId);
        delete("delete from app.commit_symbols where project_id = :projectId", projectId);
        delete("delete from app.symbol_edges where project_id = :projectId", projectId);
        delete("delete from app.requirements where project_id = :projectId", projectId);
        delete("delete from app.documents where project_id = :projectId", projectId);
        delete("delete from app.commits where project_id = :projectId", projectId);
        delete("delete from app.symbols where project_id = :projectId", projectId);
    }

    public long insertSymbol(long projectId,
                             String filePath,
                             String symbolName,
                             String symbolKind,
                             String signature,
                             int lineStart,
                             int lineEnd) {
        return jdbcClient.sql("""
                insert into app.symbols (
                    project_id,
                    file_path,
                    symbol_name,
                    symbol_kind,
                    signature,
                    line_start,
                    line_end
                ) values (
                    :projectId,
                    :filePath,
                    :symbolName,
                    :symbolKind,
                    :signature,
                    :lineStart,
                    :lineEnd
                )
                returning id
                """)
                .param("projectId", projectId)
                .param("filePath", filePath)
                .param("symbolName", symbolName)
                .param("symbolKind", symbolKind)
                .param("signature", signature)
                .param("lineStart", lineStart)
                .param("lineEnd", lineEnd)
                .query(Long.class)
                .single();
    }

    public long insertEdge(long projectId, long sourceSymbolId, long targetSymbolId, String edgeType) {
        return jdbcClient.sql("""
                insert into app.symbol_edges (
                    project_id,
                    source_symbol_id,
                    target_symbol_id,
                    edge_type
                ) values (
                    :projectId,
                    :sourceSymbolId,
                    :targetSymbolId,
                    :edgeType
                )
                returning id
                """)
                .param("projectId", projectId)
                .param("sourceSymbolId", sourceSymbolId)
                .param("targetSymbolId", targetSymbolId)
                .param("edgeType", edgeType)
                .query(Long.class)
                .single();
    }

    public long insertCommit(long projectId,
                             String commitHash,
                             String author,
                             String commitTime,
                             String message,
                             String branchName) {
        return jdbcClient.sql("""
                insert into app.commits (
                    project_id,
                    commit_hash,
                    author,
                    commit_time,
                    message,
                    branch_name
                ) values (
                    :projectId,
                    :commitHash,
                    :author,
                    cast(:commitTime as timestamptz),
                    :message,
                    :branchName
                )
                returning id
                """)
                .param("projectId", projectId)
                .param("commitHash", commitHash)
                .param("author", author)
                .param("commitTime", commitTime)
                .param("message", message)
                .param("branchName", branchName)
                .query(Long.class)
                .single();
    }

    public void insertCommitSymbol(long projectId, long commitId, long symbolId, String changeType) {
        jdbcClient.sql("""
                insert into app.commit_symbols (
                    project_id,
                    commit_id,
                    symbol_id,
                    change_type
                ) values (
                    :projectId,
                    :commitId,
                    :symbolId,
                    :changeType
                )
                """)
                .param("projectId", projectId)
                .param("commitId", commitId)
                .param("symbolId", symbolId)
                .param("changeType", changeType)
                .update();
    }

    public long insertDocument(long projectId,
                               String docPath,
                               String docType,
                               String title,
                               String metadataJson) {
        return jdbcClient.sql("""
                insert into app.documents (
                    project_id,
                    doc_path,
                    doc_type,
                    title,
                    metadata_json
                ) values (
                    :projectId,
                    :docPath,
                    :docType,
                    :title,
                    cast(:metadataJson as jsonb)
                )
                returning id
                """)
                .param("projectId", projectId)
                .param("docPath", docPath)
                .param("docType", docType)
                .param("title", title)
                .param("metadataJson", metadataJson)
                .query(Long.class)
                .single();
    }

    public long insertRequirement(long projectId, String requirementCode, String title, Long sourceDocId) {
        return jdbcClient.sql("""
                insert into app.requirements (
                    project_id,
                    requirement_code,
                    title,
                    status,
                    source_doc_id
                ) values (
                    :projectId,
                    :requirementCode,
                    :title,
                    'ACTIVE',
                    :sourceDocId
                )
                returning id
                """)
                .param("projectId", projectId)
                .param("requirementCode", requirementCode)
                .param("title", title)
                .param("sourceDocId", sourceDocId)
                .query(Long.class)
                .single();
    }

    public long insertDocumentLink(long projectId, long documentId, Long symbolId, Long commitId, Long requirementId) {
        return jdbcClient.sql("""
                insert into app.document_links (
                    project_id,
                    document_id,
                    symbol_id,
                    commit_id,
                    requirement_id
                ) values (
                    :projectId,
                    :documentId,
                    :symbolId,
                    :commitId,
                    :requirementId
                )
                returning id
                """)
                .param("projectId", projectId)
                .param("documentId", documentId)
                .param("symbolId", symbolId)
                .param("commitId", commitId)
                .param("requirementId", requirementId)
                .query(Long.class)
                .single();
    }

    private void delete(String sql, long projectId) {
        jdbcClient.sql(sql)
                .param("projectId", projectId)
                .update();
    }
}
