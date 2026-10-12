# stock-master 数据采集

该模块迁移了 `stock_quant/sequoia_x/data` 的 BaoStock 获取逻辑，并将数据写入
`stock-master` 现有 MySQL 的 `daily_index` 表。代码按 `stock_info` 中的股票列表
自动增量同步；`--backfill` 用于指定起始日期全量回填。

## 安装

```bash
cd stock-data
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
```

默认连接 `127.0.0.1:3306/stock_master`，可通过 `STOCK_DB_*` 环境变量覆盖。

## 运行

```bash
STOCK_DB_PASSWORD=123456 .venv/bin/python -m stock_data.sync
STOCK_DB_PASSWORD=123456 .venv/bin/python -m stock_data.sync --backfill --start-date 2024-01-01
```

代码参数统一支持并推荐使用 `sh.600000`、`sz.000001` 这种带点格式。
首次运行建议指定少量标的验证：

```bash
.venv/bin/python -m stock_data.sync --backfill --symbols sh.600000,sz.000001
```

同步器会先读取 BaoStock 交易日历，检查每只股票在区间内缺失的交易日；
发现缺口时从最早缺失日期重新拉取并幂等写入，因此可以重复执行补全。

`stock-service` 系统首页提供“一键下载并补全”按钮。按钮使用 Java 内置的
BaoStock TCP 客户端，任务异步执行，同步日志会写入 `stock-service` 日志；
不会启动本目录下的 Python 脚本。
