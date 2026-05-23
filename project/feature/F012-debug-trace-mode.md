# Feature: Debug Trace Mode (F012)

## Meta

- **Epic**: Core → Developer Experience
- **Roll-up target**: ## MVP v1.0.0
- **Status**: implemented
- **Target release**: v1.0.0
- **Created**: 2026-05-21
- **Related cases**: CP001, CP002, CP003

## Goal

Пользователь видит пошаговое выполнение healing-процесса, может инспектировать кластеры и понимать почему был выбран тот или иной элемент. Debug Mode повышает прозрачность работы fletta и помогает в отладке тестов.

## User workflow

1. Пользователь включает debug режим в конфиге: `debug: true` или `debug: 'verbose'`
2. Запускает тест как обычно через Playwright
3. При healing (или его попытке) система записывает промежуточные шаги
4. После выполнения генерируется debug-отчёт с визуализацией кластеров
5. Пользователь открывает HTML-отчёт и видит:
   - Сколько элементов спарсилось
   - Какие кластеры построены
   - Топ кандидатов с confidence scores
   - Почему принято то или иное решение

## Scope

### In

- Флаг `debug: boolean | 'verbose'` в `FlettaConfig`
- Структура `DebugReport` с пошаговой трассировкой
- Этапы трассировки: `dom_parsed`, `clusters_built`, `candidates_ranked`, `healing_decision`
- HTML viewer для визуализации кластеров (аналог Allure/Playwright report)
- Интеграция с `FlettaReporter` для сохранения debug-информации
- JSON dump кластеров для программного анализа

### Out

- Интерактивный debugger (breakpoint'ы)
- Real-time streaming логов
- Визуальная подсветка элементов на скриншотах (это F007 Visual Fingerprint)
- Сравнение DOM между запусками (это отдельная фича анализа)

## Acceptance criteria

- [x] Флаг `debug: boolean | 'verbose'` работает в `FlettaConfig`
- [x] При `debug: true` создаётся `DebugReport` с минимальной информацией (шаги + кластеры)
- [ ] При `debug: 'verbose'` добавляются полные сигнатуры всех кандидатов (future enhancement)
- [x] HTML-отчёт `fletta-debug.html` генерируется рядом с `fletta-report.json`
- [x] При 2+ debug-тестах: **Classic (A)** — `fletta-debug.html` (индекс с группами, prev/next на деталях); **Explorer (B)** — `fletta-debug-explorer.html` (sidebar + поиск + iframe с `?embed=1`)
- [x] Тёмная тема на индексе A, explorer B и детальных страницах (localStorage `fletta-theme`)
- [x] В отчёте видны все этапы: DOM parsed (N элементов), Clusters built (M кластеров), Candidates ranked (топ-3)
- [x] Для CP002 в отчёте видно почему элемент был найден (confidence, diff с оригиналом)
- [x] Для CP003 в отчёте видно почему healing отказался (ambiguity, low confidence)

## Implementation notes (sketch)

### Затронутые файлы

```
sdk/typescript/src/config.ts          # + debug поле в FlettaConfig
sdk/typescript/src/core.ts            # + DebugStep запись в heal()
sdk/typescript/src/debug.ts             # Новый файл: DebugReport, DebugTracer
adapters/playwright/src/reporter.ts       # onEnd: generateAllDebugHtml
adapters/playwright/src/debug-viewer.ts   # Classic view (A)
adapters/playwright/src/debug-explorer.ts # Explorer view (B)
adapters/playwright/src/debug-manifest.ts # manifest.json + группировка
adapters/playwright/src/debug-chrome.ts   # шапка, nav, theme toggle
adapters/playwright/src/debug-status.ts   # success/warning/failure
```

### Структуры данных

```typescript
// sdk/typescript/src/debug.ts
export interface DebugReport {
  timestamp: string;
  testName: string;
  duration_ms: number;
  steps: DebugStep[];
  clusters: ClusterView[];
  healing: HealingReport;
}

export interface DebugStep {
  name: 'dom_parsed' | 'clusters_built' | 'candidates_ranked' | 'healing_decision';
  timestamp: string;
  duration_ms: number;
  input?: unknown;
  output?: unknown;
}

export interface ClusterView {
  id: string;
  prefix: string;
  element_count: number;
  elements: Array<{
    selector: string;
    signature_preview: string;
    text_content?: string;
  }>;
}

export class DebugTracer {
  private steps: DebugStep[] = [];
  private startTime: number;

  step(name: DebugStep['name'], input?: unknown): void;
  endStep(name: DebugStep['name'], output?: unknown): void;
  buildReport(clusters: ClusterView[], healing: HealingReport): DebugReport;
}
```

### Алгоритм работы

```typescript
// В HealingEngine.heal()
heal(primarySelector, originalSignature, domSnapshot): HealResult {
  const tracer = this.config.debug ? new DebugTracer() : null;
  
  if (tracer) {
    tracer.step('dom_parsed', { selector: primarySelector });
  }
  
  // ... existing code ...
  
  if (tracer) {
    tracer.endStep('dom_parsed', { 
      element_count: domSnapshot.elements.length 
    });
    tracer.step('clusters_built');
  }
  
  const candidates = this.findCandidates(originalSignature, domSnapshot);
  
  if (tracer) {
    tracer.endStep('clusters_built', {
      cluster_count: this.getClusterCount(),
      candidates_in_cluster: candidates.length
    });
    tracer.step('candidates_ranked');
  }
  
  // ... ranking ...
  
  if (tracer) {
    tracer.endStep('candidates_ranked', {
      top_candidates: candidates.slice(0, 3).map(c => ({
        selector: c.selector,
        confidence: c.confidence
      }))
    });
    tracer.step('healing_decision');
  }
  
  // ... decision ...
  
  if (tracer) {
    tracer.endStep('healing_decision', {
      healed: result.healed,
      confidence: result.confidence,
      reason: result.healed ? 'threshold_met' : 'threshold_not_met'
    });
    
    // Сохраняем отчёт
    const report = tracer.buildReport(
      this.buildClusterViews(),
      result
    );
    this.saveDebugReport(report);
  }
  
  return result;
}
```

### HTML Viewer структура

```html
<!-- fletta-debug.html -->
<!DOCTYPE html>
<html>
<head><title>Fletta Debug Report</title></head>
<body>
  <h1>Debug Report: ${testName}</h1>
  
  <section class="timeline">
    <h2>Execution Timeline</h2>
    <!-- Временная шкала с шагами -->
  </section>
  
  <section class="clusters">
    <h2>DOM Clusters (${cluster_count})</h2>
    <!-- Древовидная визуализация кластеров -->
  </section>
  
  <section class="candidates">
    <h2>Healing Candidates</h2>
    <!-- Таблица топ-3 кандидатов с confidence -->
  </section>
  
  <section class="decision">
    <h2>Decision</h2>
    <!-- Почему принято решение healed/failed -->
  </section>
</body>
</html>
```

### Риски и зависимости

- Производительность: verbose режим может замедлить выполнение на 10-20%
- Размер отчётов: при больших DOM (>10k элементов) JSON может быть большим
- Зависит от существующей структуры `HealingReport` — нужно синхронизировать поля

## Verification / Test plan

### Manual smoke

```bash
# CP002 с debug mode
npx playwright test cp002-refactor-heal.spec.ts --reporter=list
# Ожидаем: в консоли появляются шаги [fletta:debug]
# Ожидаем: создан fletta-reports/fletta-debug.html

# Просмотр отчёта
open fletta-reports/fletta-debug.html
# Ожидаем: видны все этапы, кластеры, кандидаты
```

### Automation

- Юнит-тест `DebugTracer`: проверка что все шаги записываются
- Интеграционный тест: при `debug: true` файл `fletta-debug.html` создаётся
- Проверка структуры `DebugReport` через JSON schema

## Related docs

- [F001-self-healing.md](./F001-self-healing.md) — основной healing процесс
- [F002-unified-context.md](./F002-unified-context.md) — timeline и контекст (пересечение по отчётам)
- [project/architecture/clustering.md](../../project/architecture/clustering.md) — детали кластеризации
- [docs/cases.md](../../docs/cases.md) — CP001, CP002, CP003 кейсы для проверки
