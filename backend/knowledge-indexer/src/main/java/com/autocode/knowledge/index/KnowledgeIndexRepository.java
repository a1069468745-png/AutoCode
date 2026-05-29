package com.autocode.knowledge.index;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

@Repository
public class KnowledgeIndexRepository {

    private final JdbcClient jdbcClient;

    public KnowledgeIndexRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional
    public void deleteProjectData(long projectId) {
        jdbcClient.sql("delete from app.document_links where project_id = :projectId")
                .param("projectId", projectId)
                .update();
        jdbcClient.sql("delete from app.requirements where project_id = :projectId")
                .param("projectId", projectId)
                .update();
        jdbcClient.sql("delete from app.documents where project_id = :projectId")
                .param("projectId", projectId)
                .update();
    }

    public long insertDocument(long projectId, String docPath, String docType, String title, String metadataJson) {
        var params = new HashMap<String, Object>();
        params.put("projectId", projectId);
        params.put("docPath", docPath);
        params.put("docType", docType);
        params.put("title", title);
        params.put("metadataJson", metadataJson);

        return jdbcClient.sql("""
                insert into app.documents (
                    project_id, doc_path, doc_type, title, metadata_json
                ) values (
                    :projectId, :docPath, :docType, :title, :metadataJson::jsonb
                )
                on conflict (project_id, doc_path) do update
                    set doc_type = excluded.doc_type,
                        title = excluded.title,
                        metadata_json = excluded.metadata_json,
                        updated_at = now()
                returning id
                """)
                .params(params)
                .query((rs, rowNum) -> rs.getLong("id"))
                .single();
    }

    public long insertRequirement(long projectId, String requirementCode, String title, Long sourceDocId) {
        var params = new HashMap<String, Object>();
        params.put("projectId", projectId);
        params.put("requirementCode", requirementCode);
        params.put("title", title);
        params.put("sourceDocId", sourceDocId);

        return jdbcClient.sql("""
                insert into app.requirements (
                    project_id, requirement_code, title, source_doc_id
                ) values (
                    :projectId, :requirementCode, :title, :sourceDocId
                )
                on conflict (project_id, requirement_code) do update
                    set title = excluded.title,
                        source_doc_id = excluded.sourceDocId,
                        updated_at = now()
                returning id
                """)
                .params(params)
                .query((rs, rowNum) -> rs.getLong("id"))
                .single();
    }

    public void insertDocumentLink(long projectId, long documentId, Long symbolId, Long commitId, Long requirementId) {
        var params = new HashMap<String, Object>();
        params.put("projectId", projectId);
        params.put("documentId", documentId);
        params.put("symbolId", symbolId);
        params.put("commitId", commitId);
        params.put("requirementId", requirementId);

        jdbcClient.sql("""
                insert into app.document_links (
                    project_id, document_id, symbol_id, commit_id, requirement_id
                ) values (
                    :projectId, :documentId, :symbolId, :commitId, :requirementId
                )
                """)
                .params(params)
                .update();
    }
}