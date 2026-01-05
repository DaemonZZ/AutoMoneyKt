1) Header / Strategy Bar (hàng trên cùng)

Mục đích: cấu hình bối cảnh phân tích trước khi chạy backtest.

Strategy
Chọn chiến lược phân tích (vd: EMA Pullback v7).
→ Ảnh hưởng trực tiếp logic entry/exit, SL/TP.

Interval
Khung thời gian candle (5m, 15m, 1h…).
→ Toàn bộ indicator, EMA, ATR, winrate đều tính trên TF này.

History
Số lượng candle dùng cho backtest (vd: 1500).
→ Quyết định độ tin cậy thống kê (trade count, PF, DD).

Risk %
% vốn rủi ro mỗi lệnh (cho backtest mô phỏng position sizing).

Equity
Vốn khởi đầu cho backtest (không phải số dư thật).

Fees / Slippage
Bật/tắt phí và trượt giá để kết quả sát thực tế hơn.

Mode
SANDBOX / LIVE
→ Hiện tại dùng để phân biệt nguồn data & ý định (Analyze vs Trade).

2) Symbol Selection (cột trái)

Mục đích: chọn coin đầu vào cho phân tích.

a) Manual / Auto-pick

Manual: người dùng tự chọn coin.

Auto-pick: hệ thống tự chọn coin theo logic (volume, random, gainers…).

b) Search symbol

Tìm nhanh coin trong danh sách futures (USDT-M).

c) Symbol list

Danh sách toàn bộ coin futures khả dụng.

Tick để chọn nhiều coin.

Dòng % bên cạnh: biến động 24h (tham khảo nhanh).

d) Selected count

Cho biết đang chọn bao nhiêu coin để analyze.

3) Backtest Results (khu trung tâm)

Mục đích: kết quả phân tích cốt lõi – thứ để ra quyết định trade.

Mỗi dòng = 1 symbol đã backtest.

Cột Ý nghĩa
Symbol Coin được phân tích
Verdict Kết luận hệ thống: TRADE / WATCH / AVOID
Score Điểm tổng hợp (0–100) dựa trên rule
Trades Tổng số lệnh trong backtest
Win Rate % lệnh thắng
PF (Profit Factor)    Tổng lãi / tổng lỗ (>1 là tốt)
Max DD Drawdown lớn nhất (%)

👉 Đây là bảng bạn nhìn để trả lời câu hỏi:
“Coin này có đáng trade theo strategy hiện tại không?”

4) Run Control (cột phải – trên)

Mục đích: điều khiển quá trình phân tích.

Analyze Selected
Bắt đầu chạy backtest cho các symbol đã chọn.

Cancel
Dừng analyze đang chạy (coroutine cancel).

Export CSV
Xuất kết quả để:

lưu lịch sử

so sánh nhiều strategy

phân tích ngoài app (Excel, Python).

5) Status Panel

Mục đích: feedback realtime khi chạy analyze.

Status text

Idle / Analyzing / Done / Error

Progress (5/5)
Bao nhiêu symbol đã xử lý / tổng số.

→ Giúp user biết app đang làm gì, tránh cảm giác treo.

6) Filters (Reactive)

Mục đích: lọc kết quả sau khi backtest (không chạy lại engine).

Min Trades
Loại coin có sample quá ít.

Min Score
Chỉ giữ coin đạt điểm tối thiểu.

Min Volatility (ATR)
Loại coin quá “lì” (không đủ biên độ).

Hide low sample (<10 trades)
Lọc noise thống kê.

👉 Dùng để ra shortlist coin chất lượng cao.

7) Auto-pick Logic

Mục đích: để app chủ động đề xuất coin khi user không biết chọn gì.

Top by Volume
Coin thanh khoản cao, dễ vào lệnh.

Random 5 (diversity)
Tránh bias, khám phá coin mới.

Top Gainers (24h)
Coin đang có momentum mạnh.

Pick 5 now
Tự động chọn symbol → đưa sang analyze.

8) Exclusions

Mục đích: tránh những loại coin không phù hợp strategy.

Stablecoins
Loại coin gần như không biến động.

Leveraged tokens
Tránh coin đòn bẩy dễ méo dữ liệu.