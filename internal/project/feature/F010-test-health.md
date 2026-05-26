# Feature: Test Health Score (F010)

## Meta

- **Epic**: Feature → Observability
- **Roll-up target**: ## v2.0.0 (Scale)
- **Status**: draft
- **Target release**: v2.0.0
- **Created**: 2026-05-20
- **Related cases**: C003 (health score для flaky)

## Goal

Метрика стабильности теста на основе истории self-healing. Dashboard с health score и алертами.

## User workflow

1. Тесты регулярно запускаются в CI
2. Frap собирает метрики: healing events, время поиска, изменения UI
3. Рассчитывается health score (0–100%) для каждого теста
4. Dashboard показывает тренды
5. Алерты при деградации стабильности
6. Рекомендации по стабилизации

## Scope

### In
- Сбор метрик: healing count, latency, UI changes
- Health score алгоритм
- Dashboard (веб-интерфейс v1)
- Алерты: email/Slack при пороге
- Рекомендации: «добавьте explicit wait», «обновите селектор»

### Out
- История полных DOM snapshot (enterprise)
- Предиктивный ML (no-ML подход)
- Облачный dashboard (on-prem)

## Acceptance criteria

- [ ] Метрики собираются при каждом run
- [ ] Health score рассчитывается (0–100%)
- [ ] Dashboard: список тестов с health score
- [ ] Алерты: при score < порога
- [ ] Рекомендации: список действий по улучшению
- [ ] C003: health score выявляет flaky паттерн

## Implementation notes (sketch)

### Модули
```
crates/health/
├── src/
│   ├── metrics.rs       # Сбор метрик
│   ├── score.rs         # Расчёт health score
│   └── recommendations.rs # Генерация рекомендаций
```

### Health Score формула
```rust
struct HealthMetrics {
    total_runs: u32,
    healing_events: u32,
    failed_runs: u32,
    avg_healing_latency_ms: f64,
    ui_change_frequency: f64, // изменения UI в этом flow
}

fn calculate_health_score(m: &HealthMetrics) -> f64 {
    let healing_penalty = (m.healing_events as f64 / m.total_runs as f64) * 30.0;
    let failure_penalty = (m.failed_runs as f64 / m.total_runs as f64) * 50.0;
    let latency_penalty = if m.avg_healing_latency_ms > 100.0 { 10.0 } else { 0.0 };
    let ui_change_penalty = m.ui_change_frequency * 10.0;

    100.0 - healing_penalty - failure_penalty - latency_penalty - ui_change_penalty
}
```

### Dashboard (веб-интерфейс)
```
dashboard/
├── src/
│   ├── components/
│   │   ├── TestList.tsx      # Список тестов с health score
│   │   ├── TestDetails.tsx   # Детали конкретного теста
│   │   └── Alerts.tsx        # Настройка алертов
│   └── api/
│       └── metrics.ts        # API для метрик
```

### Риски и зависимости
- Storage: нужна БД для истории метрик
- Производительность: агрегация по многим тестам
- Приватность: метрики могут содержать чувствительные данные

## Verification / Test plan

### Manual smoke
```bash
# Запуск серии тестов
for i in {1..10}; do
  Frap replay --name "cart-flow" --metrics
done

# Просмотр health score
Frap health --name "cart-flow"
# Expected: score, healing count, recommendations

# Dashboard
npm run dashboard
# [открывается веб-интерфейс]
```

### Automation
- Юнит-тесты: расчёт score
- Интеграционные: сбор метрик

## Related docs

- [F001-self-healing.md](./F001-self-healing.md) — healing events
- [cases.md](../../docs/cases.md) — C003
