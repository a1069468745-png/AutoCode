# Integration Tests

## Scope
- `IT-05` query endpoint integration contract (`context-service`)
- `IT-06` call relation + history aggregation scenario (`context-service`)
- `IT-07` requirement traceability aggregation scenario (`context-service`)
- `PS-03` project member access mapping (`project-service`)
- `GW-03` project-level query authorization (`api-gateway`)
- `IT-08` project-management frontend integration gate (`web-console build`)
- `IT-09` query frontend integration gate (`web-console build`)
- `IT-10` phase-1 integrated smoke gate (single script)

## Run
```powershell
./integration-tests/run-mvp-smoke.ps1
```

## Notes
- The script runs module checks in a fixed order and stops on first failure.
- Frontend build is included to ensure project/query integration pages pass type-check and bundling gates.
- Real browser verification now also depends on the local PostgreSQL schema being initialized with
  `deploy/postgres/init/002_schema.sql` and `deploy/postgres/init/003_index.sql`.
- After schema init, use `POST /api/projects/{id}/sync-indexes` or the `Sync indexes` button in the
  web console to populate real code, history, and document index data for the current workspace.
