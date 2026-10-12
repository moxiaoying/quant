-- stock_master inferred schema
--
-- This file is inferred from the Java entities and JDBC statements in this
-- repository. It is not the original upstream schema. Column lengths,
-- defaults, indexes and seed data should be reviewed before production use.

CREATE DATABASE IF NOT EXISTS `stock_master`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `stock_master`;

CREATE TABLE IF NOT EXISTS `user` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(128) NOT NULL,
  `name` VARCHAR(64) NULL,
  `mobile` VARCHAR(32) NULL,
  `email` VARCHAR(128) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `stock_info` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(16) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `exchange` VARCHAR(8) NOT NULL,
  `abbreviation` VARCHAR(64) NULL,
  `state` TINYINT NOT NULL DEFAULT 0,
  `type` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_exchange_code` (`exchange`, `code`),
  KEY `idx_stock_state_type` (`state`, `type`),
  KEY `idx_stock_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `stock_log` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `stock_info_id` INT UNSIGNED NOT NULL DEFAULT 0,
  `date` DATE NOT NULL,
  `type` TINYINT NOT NULL,
  `old_value` VARCHAR(255) NULL,
  `new_value` VARCHAR(255) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_stock_log_stock_date` (`stock_info_id`, `date`),
  KEY `idx_stock_log_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `daily_index` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(16) NOT NULL,
  `date` DATE NOT NULL,
  `opening_price` DECIMAL(20,4) NULL,
  `pre_closing_price` DECIMAL(20,4) NULL,
  `highest_price` DECIMAL(20,4) NULL,
  `closing_price` DECIMAL(20,4) NULL,
  `lowest_price` DECIMAL(20,4) NULL,
  `trading_volume` BIGINT NOT NULL DEFAULT 0,
  `trading_value` DECIMAL(24,4) NULL,
  `rurnover_rate` DECIMAL(20,8) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_daily_index_code_date` (`code`, `date`),
  KEY `idx_daily_index_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- New unified daily market table. `daily_index` remains for legacy task
-- compatibility and can be retired after all readers are migrated.
CREATE TABLE IF NOT EXISTS `stock_daily` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `symbol` VARCHAR(16) NOT NULL,
  `trade_date` DATE NOT NULL,
  `open_price` DECIMAL(20,4) NULL,
  `high_price` DECIMAL(20,4) NULL,
  `low_price` DECIMAL(20,4) NULL,
  `close_price` DECIMAL(20,4) NULL,
  `pre_close_price` DECIMAL(20,4) NULL,
  `volume` DECIMAL(24,4) NOT NULL DEFAULT 0,
  `amount` DECIMAL(24,4) NULL,
  `turnover` DECIMAL(20,8) NULL,
  `adjust_flag` TINYINT NOT NULL DEFAULT 3,
  `source` VARCHAR(32) NOT NULL DEFAULT 'baostock',
  `batch_id` VARCHAR(64) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_daily_symbol_date_adjust` (`symbol`, `trade_date`, `adjust_flag`),
  KEY `idx_stock_daily_symbol_date` (`symbol`, `trade_date`),
  KEY `idx_stock_daily_trade_date` (`trade_date`)
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

CREATE TABLE IF NOT EXISTS `holiday_calendar` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `date` DATE NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_holiday_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `task` (
  `id` INT UNSIGNED NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `description` VARCHAR(255) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `execute_info` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `task_id` INT UNSIGNED NOT NULL,
  `params_str` TEXT NULL,
  `is_manual` TINYINT(1) NOT NULL DEFAULT 0,
  `state` TINYINT NOT NULL DEFAULT 2,
  `start_time` DATETIME NULL,
  `complete_time` DATETIME NULL,
  `message` TEXT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_execute_task_state` (`task_id`, `state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `system_config` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `value1` VARCHAR(1024) NULL,
  `value2` VARCHAR(1024) NULL,
  `value3` VARCHAR(1024) NULL,
  `state` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_system_config_name_state` (`name`, `state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `robot` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `type` TINYINT NOT NULL,
  `webhook` VARCHAR(1024) NOT NULL,
  `state` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_robot_type_state` (`type`, `state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `stock_selected` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(16) NOT NULL,
  `rate` DECIMAL(20,8) NULL,
  `description` VARCHAR(255) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_selected_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `trade_method` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `url` VARCHAR(1024) NOT NULL,
  `state` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_method_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `trade_user` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `account_id` VARCHAR(128) NOT NULL,
  `password` VARCHAR(128) NOT NULL,
  `cookie` TEXT NULL,
  `validate_key` VARCHAR(255) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_user_account_id` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `trade_strategy` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `bean_name` VARCHAR(255) NOT NULL,
  `state` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_strategy_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `trade_rule` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `stock_code` VARCHAR(16) NOT NULL,
  `strategy_id` INT UNSIGNED NOT NULL,
  `user_id` INT UNSIGNED NOT NULL,
  `type` TINYINT NOT NULL DEFAULT 0,
  `value` DECIMAL(20,8) NULL,
  `volume` INT NOT NULL DEFAULT 0,
  `open_price` DECIMAL(20,4) NULL,
  `highest_price` DECIMAL(20,4) NULL,
  `lowest_price` DECIMAL(20,4) NULL,
  `highest_volume` DECIMAL(20,8) NULL,
  `lowest_volume` DECIMAL(20,8) NULL,
  `state` TINYINT NOT NULL DEFAULT 1,
  `description` VARCHAR(1024) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_trade_rule_user_state` (`user_id`, `state`),
  KEY `idx_trade_rule_stock_code` (`stock_code`),
  KEY `idx_trade_rule_strategy` (`strategy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `trade_order` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `rule_id` INT UNSIGNED NOT NULL,
  `stock_code` VARCHAR(16) NOT NULL,
  `entrust_code` VARCHAR(64) NULL,
  `deal_code` VARCHAR(64) NULL,
  `related_deal_code` VARCHAR(64) NULL,
  `price` DECIMAL(20,4) NULL,
  `volume` INT NOT NULL DEFAULT 0,
  `trade_type` VARCHAR(32) NULL,
  `trade_state` VARCHAR(32) NULL,
  `trade_time` DATETIME NULL,
  `state` TINYINT NOT NULL DEFAULT 1,
  `user_id` INT UNSIGNED NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_trade_order_rule_user_state` (`rule_id`, `user_id`, `state`),
  KEY `idx_trade_order_trade_time` (`trade_time`),
  KEY `idx_trade_order_entrust_code` (`entrust_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `trade_deal` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `stock_code` VARCHAR(16) NOT NULL,
  `deal_code` VARCHAR(64) NULL,
  `price` DECIMAL(20,4) NULL,
  `volume` INT NOT NULL DEFAULT 0,
  `trade_type` VARCHAR(32) NULL,
  `cr_trade_type` VARCHAR(32) NULL,
  `trade_time` DATETIME NULL,
  `user_id` INT UNSIGNED NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_trade_deal_user_time` (`user_id`, `trade_time`),
  KEY `idx_trade_deal_code` (`deal_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Task IDs and names are explicitly defined by Task.java.
INSERT INTO `task` (`id`, `name`, `description`) VALUES
  (1, 'begin_of_year', 'begin of year'),
  (2, 'begin_of_day', 'begin of day'),
  (3, 'update_of_stock', 'update stock list'),
  (4, 'update_of_daily_index', 'update daily index'),
  (5, 'ticker', 'stock ticker'),
  (6, 'trade_ticker', 'trade ticker'),
  (7, 'apply_new_stock', 'apply new stock'),
  (8, 'auto_login', 'auto login')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `description` = VALUES(`description`);

-- One pending execution record per built-in task makes the task page usable.
INSERT INTO `execute_info` (`task_id`, `params_str`, `is_manual`, `state`)
SELECT t.`id`, '', 0, 2
FROM `task` t
WHERE NOT EXISTS (
  SELECT 1 FROM `execute_info` e WHERE e.`task_id` = t.`id`
);
