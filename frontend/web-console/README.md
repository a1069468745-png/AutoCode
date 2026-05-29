# Web Console

## Current MVP pages
- `overview`: current phase and runtime snapshot
- `projects`: create project, list projects, set active project, sync local real indexes
- `queries`: run code/history/knowledge/impact/traceability queries

## Environment variables
- `VITE_API_BASE_URL` (default `/api`)
- `VITE_REQUEST_TIMEOUT` (default `10000`)
- `VITE_APP_TITLE` (default `AutoCode Web Console`)

## Local run
```powershell
pnpm install
pnpm dev
```

## Build gate
```powershell
pnpm build
```

## Browser verification
1. Open `http://127.0.0.1:4173`
2. Go to `Project Management`
3. Confirm the target project shows `READY`
4. Click `Sync indexes` to rebuild local real index data from the current workspace
5. Go to `Query Playground`
6. Run sample queries such as `ContextQueryController`, `需求文档`, or `feat`
