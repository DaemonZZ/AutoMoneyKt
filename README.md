## AutoMoney v7 Kotlin Architecture

Mục tiêu: tách **core thuần** (logic, model, state machine) khỏi **runtime**, **exchange adapter**, và **UI** để:
- chạy được nhiều mode (backtest / paper / live) trên cùng một lõi
- strategy là plugin, tối ưu tham số độc lập
- đổi sàn/đổi UI không làm bẩn core

---

## Modules

### `core/`
**Vai trò:** “bộ não thuần” của hệ thống.  
**Không làm:** IO/network/UI.  
**Chứa:**
- Market Model: `Candle`, `Order`, `Position`, …
- `Strategy` interface + `StrategyState`
- `Signal` (Enter/Exit/Update SLTP…)
- `RiskEngine`
- Trade Lifecycle state machine
- Backtest/Paper engine (chạy offline)

---

### `strategies/`
**Vai trò:** tập hợp strategy plugins (vd: EMA Pullback v7).  
**Phụ thuộc:** `core`  
**Chứa:**
- Params + logic tạo `Signal` dựa trên market data
- Regime detection / filters / entry rules theo từng strategy
- Không đặt lệnh trực tiếp (chỉ phát tín hiệu)

---

### `adapters/`
**Vai trò:** “cánh tay” nói chuyện với bên ngoài (Binance/Testnet/…).
**Phụ thuộc:** `core`  
**Chứa:**
- `ExchangeAdapter` interface + implementations (vd: Binance Futures)
- Fetch market data (klines/stream), place/cancel orders, manage positions
- Mapping model & xử lý quirks của sàn (reduceOnly, SL/TP rules, margin/leverage…)

---

### `runtime/`
**Vai trò:** “người điều phối” chạy bot, nối core + strategy + adapter.  
**Phụ thuộc:** `core`, `strategies`, `adapters`  
**Chứa:**
- `BotRunner` (1 bot / 1 symbol, lock tránh chạy trùng symbol)
- Orchestration cho các mode: sandbox/paper/live
- MarketScanner/analysis trước khi run bot
- Scheduling/event loop, persistence/logging hooks

---

### `app-cli/`
**Vai trò:** entrypoint chạy bằng dòng lệnh (dev/CI/backtest nhanh).  
**Phụ thuộc:** `runtime` (+ gián tiếp core/strategies/adapters)  
**Chứa:**
- `main()` parse args (run backtest / scan / start bot)
- in summary/report/log ra console

---

### `app-ui/`
**Vai trò:** UI JavaFX điều khiển bot và hiển thị trạng thái.  
**Phụ thuộc:** `runtime`  
**Chứa:**
- Controllers/ViewModels/UI state
- start/stop bot, chọn symbol/mode, xem logs/trades/summary
- UI không gọi exchange trực tiếp; mọi thao tác đi qua runtime

---

## Dependency Graph

