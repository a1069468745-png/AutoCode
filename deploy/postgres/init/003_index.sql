create index if not exists idx_symbols_project_symbol_name
    on app.symbols (project_id, symbol_name);

create index if not exists idx_symbols_project_file_path
    on app.symbols (project_id, file_path);

create index if not exists idx_symbol_edges_project_source
    on app.symbol_edges (project_id, source_symbol_id);

create index if not exists idx_symbol_edges_project_target
    on app.symbol_edges (project_id, target_symbol_id);

create index if not exists idx_commits_project_commit_time
    on app.commits (project_id, commit_time desc);

create index if not exists idx_commit_symbols_project_symbol
    on app.commit_symbols (project_id, symbol_id);

create index if not exists idx_requirements_project_code
    on app.requirements (project_id, requirement_code);

create index if not exists idx_documents_project_doc_path
    on app.documents (project_id, doc_path);

create index if not exists idx_document_links_project_document
    on app.document_links (project_id, document_id);

create index if not exists idx_query_logs_project_created_at
    on app.query_logs (project_id, created_at desc);

create index if not exists idx_query_logs_project_query_type
    on app.query_logs (project_id, query_type);
