-- 统一行情表迁移脚本
-- 说明：
-- 1. stock_info 保存证券基础资料；
-- 2. stock_daily 保存股票、ETF、指数的日线行情；
-- 3. daily_index 暂时保留为迁移来源和回滚兼容表，运行时代码不再写入；
-- 4. 本脚本可重复执行，不删除任何旧表和旧数据。

USE `stock_master`;

CREATE TABLE IF NOT EXISTS `stock_daily` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `symbol` VARCHAR(16) NOT NULL COMMENT '标准代码，例如 sh.600000',
  `trade_date` DATE NOT NULL,
  `open_price` DECIMAL(20,4) NULL,
  `high_price` DECIMAL(20,4) NULL,
  `low_price` DECIMAL(20,4) NULL,
  `close_price` DECIMAL(20,4) NULL,
  `pre_close_price` DECIMAL(20,4) NULL,
  `volume` DECIMAL(24,4) NOT NULL DEFAULT 0,
  `amount` DECIMAL(24,4) NULL,
  `turnover` DECIMAL(20,8) NULL,
  `adjust_flag` TINYINT NOT NULL DEFAULT 3 COMMENT '1前复权 2后复权 3不复权',
  `source` VARCHAR(32) NOT NULL DEFAULT 'baostock',
  `batch_id` VARCHAR(64) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_daily_symbol_date_adjust` (`symbol`, `trade_date`, `adjust_flag`),
  KEY `idx_stock_daily_symbol_date` (`symbol`, `trade_date`),
  KEY `idx_stock_daily_trade_date` (`trade_date`),
  KEY `idx_stock_daily_source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `market_sync_job` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `job_type` VARCHAR(32) NOT NULL,
  `trade_date` DATE NULL,
  `status` VARCHAR(16) NOT NULL,
  `total_count` INT NOT NULL DEFAULT 0,
  `success_count` INT NOT NULL DEFAULT 0,
  `failed_count` INT NOT NULL DEFAULT 0,
  `retry_count` INT NOT NULL DEFAULT 0,
  `error_message` TEXT NULL,
  `started_at` DATETIME NULL,
  `finished_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_market_sync_job_type_date` (`job_type`, `trade_date`),
  KEY `idx_market_sync_job_status` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 迁移现有 daily_index 数据。旧代码中的 code 采用 sh.600000 形式，
-- 因此直接作为 stock_daily.symbol 保存，避免丢失交易所信息。
INSERT INTO `stock_daily` (
  `symbol`, `trade_date`, `open_price`, `high_price`, `low_price`,
  `close_price`, `pre_close_price`, `volume`, `amount`, `turnover`,
  `adjust_flag`, `source`
)
SELECT
  d.`code`, d.`date`, d.`opening_price`, d.`highest_price`, d.`lowest_price`,
  d.`closing_price`, d.`pre_closing_price`, d.`trading_volume`,
  d.`trading_value`, d.`rurnover_rate`, 3, 'legacy_daily_index'
FROM `daily_index` d
ON DUPLICATE KEY UPDATE
  `open_price` = VALUES(`open_price`),
  `high_price` = VALUES(`high_price`),
  `low_price` = VALUES(`low_price`),
  `close_price` = VALUES(`close_price`),
  `pre_close_price` = VALUES(`pre_close_price`),
  `volume` = VALUES(`volume`),
  `amount` = VALUES(`amount`),
  `turnover` = VALUES(`turnover`),
  `updated_at` = CURRENT_TIMESTAMP;

CREATE OR REPLACE VIEW `v_stock_daily` AS
SELECT
  d.`id`,
  d.`symbol`,
  d.`trade_date`,
  s.`code`,
  s.`name`,
  s.`exchange`,
  s.`type`,
  s.`state`,
  d.`open_price`,
  d.`high_price`,
  d.`low_price`,
  d.`close_price`,
  d.`pre_close_price`,
  d.`volume`,
  d.`amount`,
  d.`turnover`,
  d.`adjust_flag`,
  d.`source`,
  d.`batch_id`,
  d.`created_at`,
  d.`updated_at`
FROM `stock_daily` d
LEFT JOIN `stock_info` s
  ON d.`symbol` = CONCAT(s.`exchange`, '.', s.`code`);
