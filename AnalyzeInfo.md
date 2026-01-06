# Market Analysis / Backtest UI Specification

## 1) Header / Strategy Bar (hàng trên cùng)

**Mục đích:**  
Cấu hình **bối cảnh phân tích** trước khi chạy backtest.

### Strategy

- Chọn chiến lược phân tích (vd: **EMA Pullback v7**).
- Ảnh hưởng trực tiếp đến **logic entry / exit, SL / TP**.

### Interval

- Khung thời gian candle (**5m, 15m, 1h…**).
- Toàn bộ **indicator, EMA, ATR, win rate** đều tính trên TF này.

### History

- Số lượng candle dùng cho backtest (vd: **1500**).
- Quyết định **độ tin cậy thống kê**  
  *(trade count, profit factor, drawdown)*.

### Risk %

- % vốn rủi ro mỗi lệnh.
- Dùng cho **mô phỏng position sizing** trong backtest.

### Equity

- Vốn khởi đầu cho backtest.
- **Không phải số dư thật**.

### Fees / Slippage

- Bật / tắt **phí giao dịch** và **trượt giá**.
- Giúp kết quả **sát thực tế** hơn.

### Mode

- **SANDBOX / LIVE**
- Phân biệt:
    - nguồn data
    - ý định sử dụng (*Analyze vs Trade*)

---

## 2) Symbol Selection (cột trái)

**Mục đích:**  
Chọn **coin đầu vào** cho phân tích.

### a) Manual / Auto-pick

- **Manual:** người dùng tự chọn coin.
- **Auto-pick:** hệ thống tự chọn theo logic  
  *(volume, random, gainers…)*.

### b) Search Symbol

- Tìm nhanh coin trong danh sách **USDT-M futures**.

### c) Symbol List

- Danh sách toàn bộ coin futures khả dụng.
- Cho phép **tick nhiều coin**.
- % bên cạnh mỗi coin: **biến động 24h** (tham khảo nhanh).

### d) Selected Count

- Hiển thị số lượng coin đang được chọn để analyze.

---

## 3) Backtest Results (khu trung tâm)

**Mục đích:**  
Hiển thị **kết quả phân tích cốt lõi** – cơ sở để ra quyết định trade.

- Mỗi dòng = **1 symbol** đã backtest.

### Các cột & ý nghĩa

| Cột                    | Ý nghĩa                                      |
|------------------------|----------------------------------------------|
| **Symbol**             | Coin được phân tích                          |
| **Verdict**            | Kết luận hệ thống: **TRADE / WATCH / AVOID** |
| **Score**              | Điểm tổng hợp (**0–100**) dựa trên rule      |
| **Trades**             | Tổng số lệnh trong backtest                  |
| **Win Rate**           | % lệnh thắng                                 |
| **PF (Profit Factor)** | Tổng lãi / tổng lỗ *(>1 là tốt)*             |
| **Max DD**             | Drawdown lớn nhất (%)                        |

👉 **Bảng này trả lời câu hỏi:**
> *“Coin này có đáng trade với strategy hiện tại không?”*

---

## 4) Run Control (cột phải – trên)

**Mục đích:**  
Điều khiển **quá trình phân tích**.

### Analyze Selected

- Bắt đầu chạy backtest cho các symbol đã chọn.

### Cancel

- Dừng analyze đang chạy  
  *(coroutine cancel)*.

### Export CSV

- Xuất kết quả để:
    - lưu lịch sử
    - so sánh nhiều strategy
    - phân tích ngoài app *(Excel, Python)*

---

## 5) Status Panel

**Mục đích:**  
Cung cấp **feedback realtime** khi chạy analyze.

### Status Text

- `Idle` / `Analyzing` / `Done` / `Error`

### Progress

- Ví dụ: **5 / 12**
- Số symbol đã xử lý / tổng số.

→ Giúp user biết app **đang làm gì**, tránh cảm giác bị treo.

---

## 6) Filters (Reactive)

**Mục đích:**  
Lọc kết quả **sau khi backtest**  
*(không chạy lại engine)*.

### Các bộ lọc

- **Min Trades**  
  → Loại coin có sample quá ít.

- **Min Score**  
  → Chỉ giữ coin đạt điểm tối thiểu.

- **Min Volatility (ATR)**  
  → Loại coin quá “lì”, không đủ biên độ.

- **Hide low sample (<10 trades)**  
  → Giảm noise thống kê.

👉 Dùng để tạo **shortlist coin chất lượng cao**.

---

## 7) Auto-pick Logic

**Mục đích:**  
Giúp app **chủ động đề xuất coin** khi user không biết chọn gì.

- **Top by Volume**  
  → Thanh khoản cao, dễ vào lệnh.

- **Random 5 (Diversity)**  
  → Tránh bias, khám phá coin mới.

- **Top Gainers (24h)**  
  → Coin đang có momentum mạnh.

- **Pick 5 Now**  
  → Tự động chọn symbol → đưa sang analyze.

---

## 8) Exclusions

**Mục đích:**  
Loại trừ những coin **không phù hợp strategy**.

- **Stablecoins**  
  → Gần như không biến động.

- **Leveraged Tokens**  
  → Dễ méo dữ liệu, rủi ro cao.


# Auto Money v7 – Market Analysis (Full Charts)

Tài liệu này mô tả **toàn bộ kiến trúc và luồng phân tích thị trường**
theo mô hình **3 tầng**:

1. Market Eligibility
2. Strategy Compatibility
3. Backtest Confirmation

Thiết kế này **scale tốt khi có nhiều strategy plugin** và giữ core sạch.

---

## 1. Overall Scan Flow (Multi-Symbol, Multi-Strategy)

```mermaid
flowchart TD
    A[User selects symbols + timeframe + window] --> B[Fetch OHLCV Market Data]
    B --> C[Compute MarketStats\ncached]
    C --> D{Market Eligibility Pass?}
    D -- No --> X[Reject Symbol\nlow vol / chop / illiquid]
    D -- Yes --> E[Iterate Strategies]
    E --> F[Strategy Compatibility Score]
    F --> G{Score >= Threshold?}
    G -- No --> Y[Skip Strategy]
    G -- Yes --> H[Optional Backtest Confirmation]
    H --> I[Backtest Score\nExpectancy / DD / Trades]
    I --> J[Rank Symbol..Strategy Pairs]
    J --> K[UI shows Top N Candidates]
```

---

## 2. Three-Layer Analysis Detail

```mermaid
flowchart LR
    subgraph L1[Layer 1 Market Eligibility]
        A1[OHLCV] --> B1[MarketStats\nATR% / Trend / Chop / Liquidity]
        B1 --> C1{Eligibility Rules}
        C1 -->|Pass| D1[Eligible Market\n+ Regime + Confidence]
        C1 -->|Fail| E1[Rejected Market\n+ Reasons]
    end

    subgraph L2[Layer 2 Strategy Compatibility]
        D1 --> B2[Strategy Feature View\npullback depth,\ntrend persistence,\ncompression]
        B2 --> C2[Compatibility Score\n0.0 .. 1.0]
    end

    subgraph L3[Layer 3 Backtest Confirmation]
        C2 --> D2{Need Backtest?}
        D2 -->|No| E2[Use Compatibility Only]
        D2 -->|Yes| F2[Short Backtest / Walk-Forward Mini]
        F2 --> G2[Backtest Score\nExpectancy / PF / DD]
    end
```

---

## 3. Component Diagram (Core Clean Architecture)

```mermaid
flowchart TB
    UI[Desktop UI / Scanner Page] --> Runner[Scanner Runner]
    Runner --> Data[Market Data Provider\nExchange Adapter]
    Runner --> Cache[Cache Store\nOHLCV / MarketStats]
    Runner --> Analyzer[Market Analyzer Core]

    subgraph Core[Auto Money Core]
        Analyzer --> Stats[MarketStatsComputer]
        Analyzer --> Rules[Eligibility Rules Engine]
        Analyzer --> Registry[Strategy Registry]
        Registry --> S1[Strategy Plugin A]
        Registry --> S2[Strategy Plugin B]
        Registry --> S3[Strategy Plugin C]
        Analyzer --> Backtest[Backtest Engine]
        Analyzer --> Ranker[Strategy Ranking Engine]
    end

    Data --> Cache
    Cache --> Analyzer
    Ranker --> Runner
    Runner --> UI
```

---

## 4. Sequence Diagram (One Symbol – Many Strategies)

```mermaid
sequenceDiagram
    participant UI as UI
    participant R as ScannerRunner
    participant MD as MarketDataProvider
    participant C as Cache
    participant A as MarketAnalyzer
    participant SR as StrategyRegistry
    participant BT as BacktestEngine
    participant RK as Ranker
    UI ->> R: ScanRequest(symbols, timeframe, window)

    loop each symbol
        R ->> C: getOHLCV(symbol)
        alt Cache Miss
            R ->> MD: fetchOHLCV(symbol)
            MD -->> R: OHLCV
            R ->> C: storeOHLCV
        end

        R ->> A: analyze(symbol)
        A ->> C: getMarketStats
        alt Stats Miss
            A ->> A: computeMarketStats
            A ->> C: storeMarketStats
        end

        A ->> A: checkEligibility
        alt Not Eligible
            A -->> R: reject(symbol)
        else Eligible
            A ->> SR: listStrategies
            loop each strategy
                A ->> SR: compatibility(strategy, marketStats)
                SR -->> A: CompatibilityScore
                alt Score < threshold
                    A -->> R: skip(strategy)
                else Score OK
                    opt Need Backtest
                        A ->> BT: backtest(strategy)
                        BT -->> A: BacktestScore
                    end
                    A ->> RK: submitResult
                end
            end
            RK -->> R: rankedResults
        end
    end

    R -->> UI: Top Candidates
```

---

## 5. Data Objects Flow (Class Diagram)

```mermaid
classDiagram
    class ScanRequest {
        symbols: List<String>
        timeframe: Timeframe
        windowBars: Int
        strategies: List<String>
    }

    class MarketStats {
        atrPct: Double
        trendStrength: Double
        chopScore: Double
        liquidityScore: Double
        regime: Regime
        confidence: Double
    }

    class CompatibilityScore {
        score: Double
        reasons: List<String>
        needBacktest: Boolean
    }

    class BacktestScore {
        expectancy: Double
        profitFactor: Double
        maxDrawdown: Double
        tradeCount: Int
    }

    class RankedResult {
        symbol: String
        strategyId: String
        finalScore: Double
    }

    ScanRequest --> MarketStats
    MarketStats --> CompatibilityScore
    CompatibilityScore --> BacktestScore
    BacktestScore --> RankedResult
```

---

## Key Principles

- Market analysis **không phụ thuộc strategy**
- Strategy chỉ tự đánh giá **độ phù hợp**
- Backtest là bước **xác nhận**, không phải gate đầu
- Kiến trúc **scale tuyến tính** theo số strategy

---
