# Quant Platform Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将现有量化系统改造成可验证的模块化基础：统一前后端契约、修复登录态、引入行情数据源适配与 Mock 模式，并先完成白底量化工作台的第一期可运行界面。

**Architecture:** 保留当前 Spring Boot 模块化单体和 Node 静态 Web。后端新增 `market` 与 `trade` 的边界接口，所有外部行情/交易调用经过适配器；默认使用 Mock 数据和 Mock Broker，只有明确配置后才允许真实外部调用。前端保留现有 Node 代理，先以原生 HTML/CSS/JS 实现白底工作台，逐步替换旧页面。

**Tech Stack:** Java 8、Spring Boot 2.5.4、Gradle 7.1.1、Spring JDBC、Caffeine、JUnit 5、Node.js、原生 HTML/CSS/JavaScript、MySQL。

**Spec:** `docs/quant-platform-requirements.md`、`docs/frontend-ui-design.md`

## Global Constraints

- 第一阶段不执行真实交易，只允许 Mock Broker。
- 外部数据源必须经过适配器，数据失败时返回可识别的健康状态，不能静默写入业务表。
- 保持现有 Java 8 和 Spring Boot 2.5.4，避免无关升级。
- 现有前端 API 路径和 MySQL 表名保持兼容。
- 新增行为先写测试，先看到测试失败，再实现最小代码。
- 密码存储暂时保持现有 MD5 兼容格式；更换为强哈希作为后续迁移任务，不在本计划中混改。

### Task 1: Lock the current contracts and test baseline

**Files:**
- Create: `stock-service/src/test/java/vip/linhs/stock/web/AuthContractTest.java`
- Create: `stock-service/src/test/java/vip/linhs/stock/market/MarketProviderContractTest.java`
- Modify: `stock-service/src/test/resources/application-test.yml`

**Interfaces:**
- Consumes: existing `UserController`, `AuthInterceptor`, `StockCrawlerService` contracts.
- Produces: executable expectations for login, protected requests and provider failure behavior.

- [ ] **Step 1: Write the failing auth contract test**

  Assert that a successful login returns a token and that the token can authorize a protected endpoint in the same application context.

- [ ] **Step 2: Run the focused test and verify the failure**

  Run: `./gradlew test --tests '*AuthContractTest'`

  Expected: failure because the current test fixture has no deterministic user/session setup.

- [ ] **Step 3: Write the failing provider contract test**

  Assert that a provider returns either normalized stock data or an explicit unavailable result; an empty response must not be treated as successful synchronization.

- [ ] **Step 4: Run the provider test and verify the failure**

  Run: `./gradlew test --tests '*MarketProviderContractTest'`

- [ ] **Step 5: Commit the baseline tests**

  Run: `git add stock-service/src/test && git commit -m "test: define quant platform foundation contracts"`

### Task 2: Repair authentication/session behavior without breaking compatibility

**Files:**
- Modify: `stock-service/src/main/java/vip/linhs/stock/service/impl/UserServiceImpl.java`
- Modify: `stock-service/src/main/java/vip/linhs/stock/web/interceptor/AuthInterceptor.java`
- Modify: `stock-service/src/main/java/vip/linhs/stock/config/AppConfig.java`
- Modify: `stock-service/src/main/java/vip/linhs/stock/util/StockConsts.java`
- Test: `stock-service/src/test/java/vip/linhs/stock/web/AuthContractTest.java`

**Interfaces:**
- Consumes: `auth-token` header and existing `user` table.
- Produces: deterministic in-process token lookup, explicit login/logout behavior, and protected route authorization.

- [ ] **Step 1: Add a failing cache/session test**

  Test `putToSession(user, token)` followed by `getByToken(token)` returns the same user, and an unknown token returns `null`.

- [ ] **Step 2: Run the test and verify it fails**

  Run: `./gradlew test --tests '*AuthContractTest'`

- [ ] **Step 3: Implement the minimal session behavior**

  Keep the existing Caffeine cache name, make token creation and lookup explicit, and add a logout cache eviction method. Do not introduce Redis or a password migration yet.

- [ ] **Step 4: Verify login, protected request and logout behavior**

  Run: `./gradlew test --tests '*AuthContractTest'`

- [ ] **Step 5: Commit**

  Run: `git add stock-service/src/main/java stock-service/src/test && git commit -m "fix: make auth token sessions deterministic"`

### Task 3: Introduce market data provider boundaries and Mock data

**Files:**
- Create: `stock-service/src/main/java/vip/linhs/stock/market/MarketDataProvider.java`
- Create: `stock-service/src/main/java/vip/linhs/stock/market/MarketDataResult.java`
- Create: `stock-service/src/main/java/vip/linhs/stock/market/MockMarketDataProvider.java`
- Create: `stock-service/src/main/java/vip/linhs/stock/market/ExternalMarketDataProvider.java`
- Create: `stock-service/src/main/java/vip/linhs/stock/web/controller/MarketController.java`
- Modify: `stock-service/src/main/resources/application.yml`
- Test: `stock-service/src/test/java/vip/linhs/stock/market/MarketProviderContractTest.java`

**Interfaces:**
- `MarketDataProvider#getStockList()` returns `MarketDataResult<List<StockInfo>>`.
- `MarketDataProvider#getDailyIndex(List<String> codes)` returns `MarketDataResult<List<DailyIndex>>`.
- `MarketDataResult` contains `success`, `source`, `timestamp`, `data`, and `message`.

- [ ] **Step 1: Write tests for Mock provider and unavailable external provider**

  Assert Mock mode returns deterministic records and external provider failure returns `success=false` with a message and source name.

- [ ] **Step 2: Run focused tests and verify red**

  Run: `./gradlew test --tests '*MarketProviderContractTest'`

- [ ] **Step 3: Implement the interfaces and Mock provider**

  Use two or three fixed records such as `sh600000` and `sz000001`; do not use random prices because deterministic fixtures are required for later tests.

- [ ] **Step 4: Wrap existing `StockCrawlerService` behind the external provider**

  Preserve the current Eastmoney/Sina/163 clients, but convert null/empty/parse exceptions into an unavailable result. Add `market.provider: mock` to the default local profile.

- [ ] **Step 5: Add read-only endpoints**

  Add `GET /market/health`, `GET /market/stocks`, and `GET /market/daily-index` with the same JSON envelope for Mock and external providers.

- [ ] **Step 6: Run all market tests and commit**

  Run: `./gradlew test --tests '*Market*'`

  Commit: `git add stock-service/src/main && git commit -m "feat: add market provider boundary and mock data"`

### Task 4: Introduce Mock Broker and safe order boundary

**Files:**
- Create: `stock-service/src/main/java/vip/linhs/stock/trade/BrokerAdapter.java`
- Create: `stock-service/src/main/java/vip/linhs/stock/trade/MockBrokerAdapter.java`
- Create: `stock-service/src/main/java/vip/linhs/stock/trade/OrderCommand.java`
- Create: `stock-service/src/main/java/vip/linhs/stock/trade/OrderResult.java`
- Create: `stock-service/src/main/java/vip/linhs/stock/web/controller/MockTradeController.java`
- Modify: `stock-service/src/main/resources/application.yml`
- Test: `stock-service/src/test/java/vip/linhs/stock/trade/MockBrokerAdapterTest.java`

**Interfaces:**
- `BrokerAdapter#submit(OrderCommand)` returns `OrderResult`.
- `BrokerAdapter#cancel(String orderId)` returns `OrderResult`.
- `BrokerAdapter#queryAccount()` returns normalized account data.

- [ ] **Step 1: Write failing tests for buy, sell, cancel and invalid quantity**
- [ ] **Step 2: Run `./gradlew test --tests '*MockBrokerAdapterTest'` and verify red**
- [ ] **Step 3: Implement an in-memory broker with deterministic fills**
- [ ] **Step 4: Reject orders when `trade.mode` is not `mock` for this first phase**
- [ ] **Step 5: Verify order state transitions and commit**

### Task 5: Build the white dashboard shell in the existing web project

**Files:**
- Create: `stock-web/dashboard.html`
- Create: `stock-web/static/css/dashboard.css`
- Create: `stock-web/static/js/modules/dashboard.js`
- Modify: `stock-web/index.html`
- Modify: `stock-web/static/js/util/config.js`

**Interfaces:**
- Consumes: `/api/market/health`, `/api/market/stocks`, `/api/market/daily-index`, `/api/mock-trade/account`.
- Produces: white-background workspace with navigation, account cards, chart placeholder, positions, alerts and task status.

- [ ] **Step 1: Add a browser-level fixture test**

  Verify the page contains navigation labels, the mock environment badge, four account cards, and a visible empty/error state when the API is unavailable.

- [ ] **Step 2: Run the fixture test and verify red**

  Run: `npm test` after adding the minimal test runner configuration, or use a deterministic Node DOM fixture if the project remains dependency-light.

- [ ] **Step 3: Implement the white UI shell**

  Reuse the approved layout from the UI prototype: white surfaces, black text, blue primary action, red/green status colors, left navigation and responsive stacking.

- [ ] **Step 4: Connect only read-only Mock APIs**

  Never place a real buy/sell action on the dashboard. Link to the future trade page with a Mock-only label.

- [ ] **Step 5: Verify the page manually at 1440px and 375px widths**

- [ ] **Step 6: Commit**

  Run: `git add stock-web && git commit -m "feat: add white quant dashboard shell"`

### Task 6: Data-source health, persistence and operator documentation

**Files:**
- Create: `stock-service/src/main/java/vip/linhs/stock/market/MarketHealth.java`
- Create: `stock-service/src/main/java/vip/linhs/stock/web/controller/HealthController.java`
- Modify: `stock-service/src/main/resources/application.yml`
- Modify: `README.md`
- Modify: `docs/quant-platform-requirements.md`
- Test: `stock-service/src/test/java/vip/linhs/stock/market/MarketHealthTest.java`

**Interfaces:**
- `GET /health` returns service/database/provider status.
- Provider health includes last success time, last failure time, source, record count and message.

- [ ] **Step 1: Write health aggregation tests**
- [ ] **Step 2: Verify red**
- [ ] **Step 3: Implement health aggregation without hiding provider failures**
- [ ] **Step 4: Document local Mock mode and external-provider requirements**
- [ ] **Step 5: Run full backend tests and commit**

## Verification Gates

After each task:

```bash
cd /Users/dongtao/codex/stock-master/stock-service
./gradlew test
```

Before enabling any real data source or real trading adapter:

1. Mock market tests pass.
2. Mock Broker tests pass.
3. Dashboard displays provider health and an explicit environment badge.
4. MySQL schema has been imported and a test user exists with MD5-compatible password.
5. Real broker configuration is absent or disabled by default.

## Known Data Acquisition Risks

- Eastmoney, Sina, 163 and holiday endpoints are external and may change response formats or rate-limit clients.
- Current code uses both HTTP and HTTPS endpoints and includes GBK decoding for Sina; each provider must have parser tests with saved fixtures.
- The system currently has no durable raw-response archive; before production, add `data_sync_log` and optionally object storage for raw payloads.
- Real-time prices and historical daily data have different freshness and correction semantics; they must not overwrite each other without source and timestamp fields.
- A missing or empty provider response must not be interpreted as zero price, zero volume or an empty successful synchronization.

