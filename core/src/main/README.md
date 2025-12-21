No IO: core không đọc file, không gọi network, không log framework.

No time: không dùng System.currentTimeMillis() trong core (trừ test). Thời gian đi vào qua dữ liệu Candle.t.

No exchange quirks: reduceOnly, leverage… thuộc adapters.

Strategy chỉ tạo Signal, không “đặt lệnh”.