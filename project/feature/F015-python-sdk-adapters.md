# Feature: Python SDK & Adapters (F015)

## Meta

- **Epic**: SDK → Python
- **Roll-up target**: ## backlog → v1.5.0 (после F000 JSON-RPC)
- **Status**: draft
- **Target release**: backlog
- **Created**: 2026-05-23
- **Related cases**: C004 (POM export Python), CP005 (report export)

## Goal

Python SDK с тем же контрактом, что F013: `configure`, `heal`, `discover`, events, reports. Адаптеры для **pytest** + Selenium WebDriver. Быстрый старт через **JSON-RPC к Core CLI** (F000); позже — in-process FFI/ctypes.

## User workflow

1. `pip install frap` (или path dependency из monorepo `sdk/python/`).
2. В `conftest.py`: fixture `frap_driver` оборачивает Selenium driver.
3. При `NoSuchElementException` → snapshot → `heal()` → retry или fail + diff.
4. Отчёты: `frap-events.jsonl`, опционально JUnit-compatible output для CI.

### Альтернатива (v1)

```bash
frap discover --url https://app.example/login  # без pytest, фаза 0
```

## Scope

### In

- `sdk/python/` — пакет `frap`, типы `HealResult`, config
- v1 transport: subprocess JSON-RPC к `frap` CLI (F000)
- pytest plugin / fixture для Selenium
- Паритет событий с TS/Java (`frap-events.jsonl`)
- Документация: align с [sdk-strategy.md](../architecture/sdk-strategy.md)

### In (later)

- In-process FFI (`ctypes`) к native Core
- pytest-playwright adapter (отдельный модуль или расширение F008 patterns)

### Out

- Замена pytest
- Полноценный Selenide-аналог на Python
- Mobile (F006)

## Acceptance criteria

- [ ] `sdk/python` устанавливается из monorepo, `pip install -e sdk/python`
- [ ] `heal()` через JSON-RPC возвращает структуру, совместимую с F013 `HealResult`
- [ ] pytest fixture: 1 pilot test с intentional locator drift → heal или safe-fail
- [ ] Events пишутся в `frap-reports/frap-events.jsonl`
- [ ] README: quick start < 20 мин
- [ ] (later) ctypes/FFI без subprocess overhead

### Target layout

```
sdk/python/
  frap/
    __init__.py
    client.py      # JSON-RPC → Core
    types.py
    config.py
adapters/pytest/   # или sdk/python/frap_pytest/
  plugin.py
  fixtures.py
```

## Implementation notes (sketch)

### Transport v1

```python
# client.py — sketch
result = core_client.heal(
    dom_json=snapshot,
    original_selector=selector,
    min_confidence=0.85,
)
```

Subprocess к `frap rpc` — см. F000 JSON-RPC spec.

### Зависимости

| Фича | Связь |
|------|--------|
| F000 | JSON-RPC CLI (blocking для v1) |
| F013 | API contract reference |
| F001 | Healing semantics |

### Риски

- Latency subprocess vs in-process — приемлемо для PoC, не для high-frequency
- GIL / async pytest — явная sync API сначала

## Verification / Test plan

### Manual smoke

```bash
cd sdk/python && pip install -e .
pytest tests/test_heal_smoke.py -v
```

### Automation

- Integration: mock Core JSON-RPC responses
- E2E с test-app + Selenium (backlog)

## Related docs

- [sdk-strategy.md](../architecture/sdk-strategy.md)
- [F000: Core Platform API](./F000-core-platform-api.md)
- [F013: TypeScript SDK](./F013-typescript-sdk.md)
- [F014: Java SDK & UI Adapters](./F014-java-sdk-ui-adapters.md)
- [integrations.md](../../docs/integrations.md)
