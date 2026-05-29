package com.autocode.codegraph.index;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

@Repository
public class CodegraphIndexRepository {

    private final JdbcClient jdbcClient;

    public CodegraphIndexRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional
    public void deleteProjectData(long projectId) {
        jdbcClient.sql("delete from app.symbol_edges where project_id = :projectId")
                .param("projectId", projectId)
                .update();
        jdbcClient.sql("delete from app.symbols where project_id = :projectId")
                .param("projectId", projectId)
                .update();
    }

    public long insertSymbol(long projectId, String filePath, String name, String kind,
                             String signature, int lineStart, int lineEnd) {
        var params = new HashMap<String, Object>();
        params.put("projectId", projectId);
        params.put("filePath", filePath);
        params.put("name", name);
        params.put("kind", kind);
        params.put("signature", signature);
        params.put("lineStart", lineStart);
        params.put("lineEnd", lineEnd);

        return jdbcClient.sql("""
                insert into app.symbols (
                    project_id, file_path, name, kind, signature, line_start, line_end
                ) values (
                    :projectId, :filePath, :name, :kind, :signature, :lineStart, :lineEnd
                )
                returning id
                """)
                .params(params)
                .query((rs, rowNum) -> rs.getLong("id"))
                .single();
    }

    public void insertEdge(long projectId, long sourceSymbolId, long targetSymbolId, String edgeType) {
        var params = new HashMap<String, Object>();
        params.put("projectId", projectId);
        params.put("sourceSymbolId", sourceSymbolId);
        params.put("targetSymbolId", targetSymbolId);
        params.put("edgeType", edgeType);

        jdbcClient.sql("""
                insert into app.symbol_edges (
                    project_id, source_symbol_id, target_symbol_id, edge_type
                ) values (
                    :projectId, :sourceSymbolId, :targetSymbolId, :edgeType
                )
                """)
                .params(params)
                .update();
    }
}