-- 将 daily_index 中历史的 sh600000/sz000001 格式迁移为 sh.600000/sz.000001。
-- 执行前请备份 stock_master 数据库。
START TRANSACTION;

UPDATE daily_index d
JOIN stock_info s
  ON d.code = CONCAT(s.exchange, s.code)
SET d.code = CONCAT(s.exchange, '.', s.code)
WHERE d.code NOT LIKE '%.%';

COMMIT;
