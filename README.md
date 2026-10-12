# stock
github: [https://github.com/bosspen1/stock](https://github.com/bosspen1/stock)

码云: [https://gitee.com/bosspen1_admin/stock](https://gitee.com/bosspen1_admin/stock)

## 升级日志
### 升级只针对以前已经部署的朋友, 新来的朋友直接按最新步骤来就行
2022-12-04 解决升级后控件登录问题

------------


## 项目介绍
本项目偏向交易, 分析和策略这块相对不足. python版本还没达到开源的要求，需要的可以扫最后的二维码进群索取. 
- 自动交易
 1. 股票交易接口(封装东方财富网交易, 支持a股, etf, 可转债交易)
 2. 交易策略
- 股票数据抓取
 1. 股票名字及新增自动更新
 2. 股票每日数据抓取, 需要历史每日行情的朋友可以从单元测试类爬取
 3. 股票实时价格提示
- 注意事项
 1. 不要把调用接口频率调太大, 以免被东方财富监控. 有朋友循环不停的请求, 导致账号被东方财富警告
 2. 自带一个网格交易策略, 如需要使用, 可以先配置mock和小成交量测试 请务必弄清楚后再配置启用, 以免自动挂错误的单造成不必要损失
 3. 习惯用maven的朋友可以搜索gradle转maven, 进行项目转换
 4. 由于某种原因前端做的比较简陋
 5. 熟悉项目建议先wiki clone到本地, 看里面的deployment.docx. 代码可以从任务(ScheduledTasks)和前端页面调用的接口开始
 6. 本项目只供个人测试，请在法律允许范围内的使用, 造成损失和违法概不负责, 祝大家早日找到适合自己的方法

------------

## 软件架构
- springboot
- js

------------

## 所需环境
- java
- mysql
- nodejs

<font color="red" size=8>文档和数据库表见[wiki](https://github.com/bosspen1/stock/wiki)</font>

<font color="red" size=10>**注意: 把wiki clone到本地就可以看到数据库表结构和部署和升级文档!!!**</font>

<font color="red" size=10>**注意: 把wiki clone到本地就可以看到数据库表结构和部署和升级文档!!!**</font>

<font color="red" size=10>**注意: 把wiki clone到本地就可以看到数据库表结构和部署和升级文档!!!**</font>

<font color="red" size=8>如需和github集成自动构建可以引入[deployment](https://github.com/bosspen1/deployment)</font>

------------


## 使用说明

### 行情数据同步

`stock-data` 目录提供从 BaoStock 获取日线数据并写入本项目 MySQL
`daily_index` 表的独立采集程序。它不会改变原有 Spring Boot 服务的交易逻辑。
`daily_index.code` 统一保存为 `sh.600000` / `sz.000001`；已有数据库可先执行
`stock_master_code_migration.sql` 迁移历史数据。

```shell
cd stock-data
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt

# 首次验证少量标的
STOCK_DB_PASSWORD=123456 .venv/bin/python -m stock_data.sync \
  --backfill --symbols sh.600000,sz.000001 --start-date 2024-01-01

# 按 stock_info 增量同步
STOCK_DB_PASSWORD=123456 .venv/bin/python -m stock_data.sync
```

也可以从工程根目录执行：

```shell
./bin/stock-data.sh
```

启动 `stock-service` 后，登录系统首页即可使用“行情数据”区域的一键下载并补全按钮。
按钮会启动全市场 BaoStock 回填任务，并自动补齐数据库中缺失的交易日。

按照wiki里面的deployment文档准备好环境和配置文件后, 可以常规运行或者容器运行, stock-service第一次构建可能需要几分钟

### 常规运行

#### service
```shell
gradlew build -x test
# 或者用自己的gradle构建, 版本 7.0+
# gradle build -x test
java -jar stock-service.jar
```

#### web
```shell
npm install
npm start
```

想制作启动脚本可以参考bin下的脚本, 也可以直接部署到 nginx tomcat 之类的服务器启动

### docker方式部署

准备数据库后, 修改 stock-service\src\main\resources\application.yml 配置数据库

```shell
docker build --no-cache -t stock:1.0.0 .
docker run -p 8088:8088 -p 3000:3000 --privileged=true -itd --name stock-test stock:1.0.0
```

启动后进入容器执行脚本

```shell
cd /data/deploy/bin
./stock-service.sh
./stock-web.sh
```

------------

## 有什么问题和建议或者好的交易策略欢迎进群交流
[点击查看微信群二维码](http://image.wlgccl.top/wechat.jpg)
二维码过期可以在issue里通知我更新, 或者晚点再来
