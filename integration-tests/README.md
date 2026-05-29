# Integration Tests

## Scope
- `IT-05` 查询接口联调契约（`context-service`）
- `IT-06` 调用关系与历史综合聚合场景（`context-service`）
- `IT-07` 需求追溯聚合场景（`context-service`）
- `PS-03` 项目成员权限映射（`project-service`）
- `GW-03` 项目级查询授权拦截（`api-gateway`）

## Run
```powershell
./integration-tests/run-mvp-smoke.ps1
```

## Notes
- The script runs module-level tests in a fixed order and stops on first failure.
- Each module test already includes endpoint-level assertions for the covered integration scope.
