# Cases

Каталог сценариев для демонстрации, тестирования и разработки.
Каждый кейс имеет ID, привязан к фичам, содержит пошаговый сценарий.

**Conference demo (E2E):** [internal/testing/conference/README.md](../internal/testing/conference/README.md) · кейсы `CONF-*` · [CASES.md](../project/cases/conference/CASES.md)  
**PoC / benchmark:** CP001–CP005 (исторические ID) — см. [benchmark.md](./benchmark.md).  
**Полные демо:** C001–C008 ниже.

---

## Conference demo (CONF-*)

Основной автоматизированный demo-проект: **FixtureConf 2026 Spring**. Прогон: `./scripts/test.sh conference`.

| Было (CP) | Стало (CONF) |
|-----------|----------------|
| CP001 | CONF-PW-REG-PASS |
| CP002 | CONF-SH-SCHED-PASS |
| CP003 | CONF-SH-CFP-FAIL |

Полная матрица (pass + fail на каждую возможность Frap): [project/cases/conference/CASES.md](../project/cases/conference/CASES.md).

---

## PoC Cases (CP001–CP005, legacy IDs)

Исторические идентификаторы gates; реализация перенесена в Conference demo. Детали: [benchmark.md](./benchmark.md).

| ID | Название | CONF-* |
|----|----------|--------|
| CP001 | Happy path | CONF-PW-REG-PASS |
| CP002 | Refactor heal | CONF-SH-SCHED-PASS |
| CP003 | Safe fail | CONF-SH-CFP-FAIL |
| CP004 | Role locator | CONF-PW-SPK-PASS (+ будущие) |
| CP005 | CI export | CONF-RPT-RUN-PASS |

---

## C001: Payment Button Refactor

**Название:** Кнопка оплаты после рефакторинга

**Фичи:** F001 (Self-Healing Selectors)

**Контекст:**
- E-commerce приложение, страница checkout
- Кнопка "Оплатить" имеет `data-testid="pay-btn"`
- Разработчики меняют дизайн, ID меняется на `data-testid="checkout-pay"`

**Сценарий:**
1. Записываем тест: клик на `[data-testid="pay-btn"]`
2. Разработчик деплоит новую версию (ID изменён)
3. Запускаем тест — классический селектор не находит элемент
4. Frap ищет по сигнатуре: текст "Оплатить", близость к форме, структура
5. Находит новую кнопку, тест проходит
6. Обновляет селектор в базе (опционально)

**Демо-скрипт:**
```bash
# 1. Стартовая версия
Frap record --name "payment-flow" --url "http://demo-store.local/checkout"
# [user clicks pay button]
# Saved: selector [data-testid="pay-btn"]

# 2. Обновляем приложение (меняем ID)
docker-compose up -d checkout-v2

# 3. Воспроизведение
Frap replay --name "payment-flow"
# [Frap detects selector change, finds by signature]
# Result: PASSED (healed)
```

**Проверка успеха:** Тест проходит без ручного обновления селектора

---

## C002: API Timeout Root Cause

**Название:** Падение из-за таймаута API (RCA-demo)

**Фичи:** F002 (Unified Context), F003 (RCA)

**Контекст:**
- Форма checkout, кнопка "Оплатить" не появляется
- Причина: backend отвечает 30 секунд, frontend не рендерит кнопку

**Сценарий:**
1. Запускаем тест (UI + Network capture включены)
2. Тест ждёт кнопку, её нет
3. Таймаут — тест падает
4. Frap строит timeline:
   - 10:15:00.100 POST /api/payment-intent START
   - 10:15:30.200 POST /api/payment-intent TIMEOUT
   - 10:15:30.500 UI: элемент "pay-button" not found
5. RCA-репорт: причина — network timeout, не UI-изменение

**Демо-скрипт:**
```bash
# Backend с искусственной задержкой
docker-compose up -d backend-slow

Frap replay --name "payment-flow" --capture-all
# Test: FAILED (timeout waiting for button)

Frap analyze --run-id <id>
# RCA Report:
#   Primary cause: Network timeout (POST /api/payment-intent)
#   UI state: Expected element absent due to API failure
#   Action: Check backend latency, not selectors
```

**Проверка успеха:** RCA корректно идентифицирует сетевую проблему, не пытается "лечить" селектор

---

## C003: Flaky Test Diagnosis

**Название:** Диагностика нестабильного теста

**Фичи:** F001, F002, F003

**Контекст:**
- Тест падает 30% запусков на одном и том же коммите
- Непонятно: race condition, сеть, или UI?

**Сценарий:**
1. Запускаем тест 10 раз подряд с полным capture
2. 7 проходов, 3 падения
3. Сравниваем timelines упавших vs успешных
4. Паттерн: падения когда `GET /api/cart` отвечает >500ms
5. Вывод: race condition — UI рендерится до загрузки данных

**Демо-скрипт:**
```bash
# Массовый запуск
for i in {1..10}; do
  Frap replay --name "cart-flow" --capture-all
done

# Анализ паттерна
Frap analyze --aggregate --name "cart-flow" --runs 10
# Pattern detected:
#   Failure correlation: /api/cart response time > 500ms
#   Success correlation: /api/cart response time < 200ms
#   Root cause: Race condition between API and UI render
```

**Проверка успеха:** Выявлен конкретный порог (500ms) и эндпоинт

---

## C004: Page Object Generation

**Название:** Автогенерация Page Object для магазина

**Фичи:** F004 (Page Object Generator)

**Контекст:**
- Новый проект, нужен Page Object для каталога товаров
- Структура: список карточек, фильтры, пагинация

**Сценарий:**
1. Открываем страницу каталога
2. Запускаем генерацию
3. Frap анализирует структуру:
   - Повторяющиеся элементы (карточки товаров)
   - Интерактивные элементы (кнопки, фильтры)
   - Формы ввода
4. Генерирует Page Object с self-healing селекторами
5. Экспорт в нужный язык (Java/TypeScript/Python)

**Демо-скрипт:**
```bash
Frap generate --url "http://demo-store.local/catalog" --output ./pages

# Output:
# ./pages/CatalogPage.ts
#   - method: getProductCards(): SelfHealingElement[]
#   - method: applyFilter(category: string)
#   - method: goToPage(n: number)
#   - selector: productCard (signature-based)
```

**Проверка успеха:** Сгенерированный код компилируется и работает

---

## C005: LLM Test Generation

**Название:** Генерация теста из текстового описания через MCP

**Фичи:** F005 (MCP Integration)

**Контекст:**
- Пользователь пишет: "Проверь что пользователь может добавить товар в корзину и перейти к оплате"
- LLM-агент должен создать и выполнить тест

**Сценарий:**
1. LLM получает запрос на естественном языке
2. Через MCP вызывает frap: `generate` с описанием
3. Frap запускает браузер, исследует страницу
4. Генерирует шаги: найти кнопку "В корзину", кликнуть, проверить счётчик
5. Возвращает LLM код теста или результат выполнения

**Демо-скрипт:**
```json
// MCP Request
{
  "method": "Frap/generate",
  "params": {
    "description": "Add product to cart and proceed to checkout",
    "url": "http://demo-store.local"
  }
}

// Response
{
  "test": "...generated test code...",
  "steps": [
    "navigate to catalog",
    "click first product",
    "click 'Add to cart'",
    "verify cart count = 1",
    "click 'Checkout'"
  ],
  "status": "generated"
}
```

**Проверка успеха:** Сгенерированный тест успешно проходит

---

## C006: Mobile App Self-Healing

**Название:** Self-healing для Android (v3)

**Фичи:** F006 (Multi-Platform)

**Контекст:**
- Android-приложение, экран настроек
- Рефакторинг ViewTree, изменение ID элементов

**Сценарий:** (аналогичен C001, но для Android)

**Проверка успеха:** Единый формат сценария работает и для веб, и для мобайла

---

## C007: AI-Agent Tool Call Audit

**Название:** Аудит вызовов инструментов AI-агентом

**Фичи:** F011 (AI-Agent Testing), F005 (MCP Integration)

**Контекст:**
- AI-агент помощника для оформления заказов
- Агент использует MCP инструменты Frap для тестирования UI
- Нужно убедиться что агент вызывает инструменты правильно и в нужном порядке

**Сценарий:**
1. Запускаем агента с Frap agent capture
2. Пользователь просит: "Проверь что пользователь может оформить заказ"
3. Агент генерирует план и вызывает MCP инструменты:
   - `frap/replay` — сценарий "add-to-cart"
   - `frap/replay` — сценарий "checkout"  
   - `frap/analyze` — если тест упал
4. Frap записывает все tool calls с аргументами и результатами
5. Проверяем assertions:
   - Агент вызвал `frap/replay` ровно 2 раза
   - Вызовы были в правильном порядке
   - Аргументы валидны (существующие scenario names)
   - Время выполнения < 60 секунд
6. При смене модели (GPT-4 → Claude) воспроизводим тот же сценарий
7. Сравниваем behavior — идентичен или есть деградация?

**Демо-скрипт:**
```bash
# 1. Запускаем agent capture
Frap agent:record --agent-id "shop-assistant" --session-id "audit-001"

# 2. Пользовательский запрос к агенту (через CLI или UI)
echo "Проверь оформление заказа" | Frap agent:prompt --session "audit-001"

# 3. Останавливаем запись
Frap agent:stop --session "audit-001"

# 4. Проверяем assertions
Frap agent:assert --session "audit-001" \
  --rule "Frap/replay called 2 times" \
  --rule "frap/analyze called 0 times" \
  --rule "duration < 60s"

# 5. Регрессия — тестируем с другой моделью
Frap agent:replay --session "audit-001" --model "claude-3-5-sonnet"
```

**Проверка успеха:**
- Assertions проходят на исходной модели
- При смене модели вызовы инструментов идентичны или лучше
- Отчёт показывает timeline tool calls и LLM reasoning steps

---

## C008: Multi-Agent A2A Flow Testing

**Название:** Тестирование A2A взаимодействия между агентами

**Фичи:** F011 (AI-Agent Testing)

**Контекст:**
- Рой из 3 агентов: Coordinator (планирует), UI-Tester (выполняет Frap), Reporter (формирует отчёт)
- Агенты общаются через A2A протокол
- Нужно тестировать что они корректно делегируют и передают контекст

**Сценарий:**
1. Запускаем capture для всего роя
2. Coordinator получает задачу "Протестировать checkout"
3. Coordinator делегирует UI-Tester через A2A
4. UI-Tester выполняет Frap/replay, возвращает результат
5. Coordinator делегирует Reporter — формирует summary
6. Проверяем:
   - Coordinator корректно разбил задачу
   - UI-Tester получил полный контекст
   - Нет потери данных при передаче между агентами
   - При падении UI-Tester, Coordinator обработал ошибку

**Демо-скрипт:**
```bash
# Запускаем 3 агента в режиме capture
Frap agent:swarm --agents "coordinator,ui-tester,reporter" --capture

# Отправляем задачу в swarm
Frap agent:swarm:task --prompt "Test checkout flow" --timeout 120s

# Анализируем A2A коммуникацию
Frap agent:swarm:analyze --run-id <id>
# Отчёт:
#   Messages: 5
#   Delegations: 2
#   Data transfer: 100% (no loss)
#   Error handling: 1 retry (success)
```

**Проверка успеха:**
- Все агенты завершили свои подзадачи
- Нет потери контекста при передаче
- При failure агента — корректная обработка и retry

---

# Legend

| Поле | Описание |
|------|----------|
| ID | Уникальный код кейса (C001, C002... / CP001 для PoC) |
| Фичи | Привязка к features.md (F001, F002...) |
| Сценарий | Пошаговое описание что происходит |
| Демо-скрипт | Конкретные команды для воспроизведения |
| Проверка успеха | Критерий что кейс отработал правильно |
