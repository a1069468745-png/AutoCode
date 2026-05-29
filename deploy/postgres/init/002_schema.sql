create table if not exists app.projects (
    id bigserial primary key,
    name varchar(128) not null,
    repo_url varchar(512) not null,
    default_branch varchar(128) not null,
    language_stack varchar(256),
    doc_repo_path varchar(512),
    status varchar(32) not null default 'CREATED',
    index_error text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_projects_name unique (name),
    constraint chk_projects_status check (status in ('CREATED', 'INDEXING', 'READY', 'FAILED', 'DISABLED'))
);

create table if not exists app.documents (
    id bigserial primary key,
    project_id bigint not null,
    doc_path varchar(512) not null,
    doc_type varchar(64) not null,
    title varchar(256) not null,
    metadata_json jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fk_documents_project
        foreign key (project_id) references app.projects (id) on delete cascade,
    constraint uq_documents_project_doc_path unique (project_id, doc_path)
);

create table if not exists app.model_profiles (
    id bigserial primary key,
    project_id bigint not null,
    provider varchar(64) not null,
    base_url varchar(512),
    model_name varchar(128) not null,
    embedding_model varchar(128),
    timeout_seconds integer not null default 30,
    fallback_model varchar(128),
    enable_local_only boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fk_model_profiles_project
        foreign key (project_id) references app.projects (id) on delete cascade,
    constraint uq_model_profiles_project unique (project_id),
    constraint chk_model_profiles_timeout_seconds check (timeout_seconds > 0)
);

create table if not exists app.query_logs (
    id bigserial primary key,
    project_id bigint not null,
    user_id varchar(128) not null,
    query_type varchar(64) not null,
    query_text text not null,
    model_used varchar(128),
    cost_token bigint not null default 0,
    created_at timestamptz not null default now(),
    constraint fk_query_logs_project
        foreign key (project_id) references app.projects (id) on delete cascade,
    constraint chk_query_logs_cost_token check (cost_token >= 0)
);

create table if not exists app.requirements (
    id bigserial primary key,
    project_id bigint not null,
    requirement_code varchar(128) not null,
    title varchar(256) not null,
    status varchar(32) not null default 'DRAFT',
    source_doc_id bigint,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fk_requirements_project
        foreign key (project_id) references app.projects (id) on delete cascade,
    constraint fk_requirements_source_doc
        foreign key (source_doc_id) references app.documents (id) on delete set null,
    constraint uq_requirements_project_code unique (project_id, requirement_code),
    constraint chk_requirements_status check (status in ('DRAFT', 'ACTIVE', 'ARCHIVED', 'DONE'))
);

create table if not exists app.symbols (
    id bigserial primary key,
    project_id bigint not null,
    file_path varchar(512) not null,
    symbol_name varchar(256) not null,
    symbol_kind varchar(64) not null,
    signature text,
    line_start integer not null,
    line_end integer not null,
    created_at timestamptz not null default now(),
    constraint fk_symbols_project
        foreign key (project_id) references app.projects (id) on delete cascade,
    constraint chk_symbols_line_range check (line_start > 0 and line_end >= line_start)
);

create table if not exists app.commits (
    id bigserial primary key,
    project_id bigint not null,
    commit_hash varchar(64) not null,
    author varchar(128) not null,
    commit_time timestamptz not null,
    message text not null,
    branch_name varchar(128) not null,
    created_at timestamptz not null default now(),
    constraint fk_commits_project
        foreign key (project_id) references app.projects (id) on delete cascade,
    constraint uq_commits_project_hash unique (project_id, commit_hash)
);

create table if not exists app.symbol_edges (
    id bigserial primary key,
    project_id bigint not null,
    source_symbol_id bigint not null,
    target_symbol_id bigint not null,
    edge_type varchar(32) not null,
    created_at timestamptz not null default now(),
    constraint fk_symbol_edges_project
        foreign key (project_id) references app.projects (id) on delete cascade,
    constraint fk_symbol_edges_source_symbol
        foreign key (source_symbol_id) references app.symbols (id) on delete cascade,
    constraint fk_symbol_edges_target_symbol
        foreign key (target_symbol_id) references app.symbols (id) on delete cascade,
    constraint uq_symbol_edges unique (project_id, source_symbol_id, target_symbol_id, edge_type),
    constraint chk_symbol_edges_type check (edge_type in ('CALLS', 'IMPORTS', 'IMPLEMENTS', 'EXTENDS', 'REFERENCES'))
);

create table if not exists app.commit_symbols (
    id bigserial primary key,
    project_id bigint not null,
    commit_id bigint not null,
    symbol_id bigint not null,
    change_type varchar(32) not null,
    created_at timestamptz not null default now(),
    constraint fk_commit_symbols_project
        foreign key (project_id) references app.projects (id) on delete cascade,
    constraint fk_commit_symbols_commit
        foreign key (commit_id) references app.commits (id) on delete cascade,
    constraint fk_commit_symbols_symbol
        foreign key (symbol_id) references app.symbols (id) on delete cascade,
    constraint uq_commit_symbols unique (project_id, commit_id, symbol_id, change_type),
    constraint chk_commit_symbols_change_type check (change_type in ('ADDED', 'MODIFIED', 'DELETED', 'RENAMED', 'UNKNOWN'))
);

create table if not exists app.document_links (
    id bigserial primary key,
    project_id bigint not null,
    document_id bigint not null,
    symbol_id bigint,
    commit_id bigint,
    requirement_id bigint,
    created_at timestamptz not null default now(),
    constraint fk_document_links_project
        foreign key (project_id) references app.projects (id) on delete cascade,
    constraint fk_document_links_document
        foreign key (document_id) references app.documents (id) on delete cascade,
    constraint fk_document_links_symbol
        foreign key (symbol_id) references app.symbols (id) on delete cascade,
    constraint fk_document_links_commit
        foreign key (commit_id) references app.commits (id) on delete cascade,
    constraint fk_document_links_requirement
        foreign key (requirement_id) references app.requirements (id) on delete cascade,
    constraint chk_document_links_target_exists
        check (symbol_id is not null or commit_id is not null or requirement_id is not null)
);
