create schema if not exists app;

create table if not exists app.platform_bootstrap_marker (
    marker_key varchar(64) primary key,
    marker_value varchar(256) not null,
    created_at timestamptz not null default now()
);

insert into app.platform_bootstrap_marker (marker_key, marker_value)
values ('platform', 'autocode')
on conflict (marker_key) do nothing;
