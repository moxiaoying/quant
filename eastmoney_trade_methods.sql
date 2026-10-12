-- EastMoney trade method URLs inferred from:
-- /Users/dongtao/codex/stock_quant/eastmoney-trade-v1.0/east_money.py
--
-- This migration only stores endpoint URLs. It does not store account
-- credentials, cookies, validate keys or OCR secrets.

USE `stock_master`;

INSERT INTO `trade_method` (`name`, `url`, `state`) VALUES
  ('authentication', 'https://jywg.18.cn/Login/Authentication?validatekey=${validatekey}', 1),
  ('authentication_check', 'https://jywg.18.cn/Trade/Buy', 1),
  ('yzm', 'https://jywg.18.cn/Login/YZM', 1),
  ('get_asserts', 'https://jywg.18.cn/Com/queryAssetAndPositionV1?validatekey=${validatekey}', 1),
  ('submit', 'https://jywg.18.cn/Trade/SubmitTradeV2?validatekey=${validatekey}', 1),
  ('revoke', 'https://jywg.18.cn/Trade/RevokeOrders?validatekey=${validatekey}', 1),
  ('get_stock_list', 'https://jywg.18.cn/Search/GetStockList?validatekey=${validatekey}', 1),
  ('get_orders_data', 'https://jywg.18.cn/Search/GetOrdersData?validatekey=${validatekey}', 1),
  ('get_deal_data', 'https://jywg.18.cn/Search/GetDealData?validatekey=${validatekey}', 1),
  ('get_his_deal_data', 'https://jywg.18.cn/Search/GetHisDealData?validatekey=${validatekey}', 1),
  ('get_his_orders_data', 'https://jywg.18.cn/Search/GetHisOrdersData?validatekey=${validatekey}', 1),
  ('get_can_buy_new_stock_list_v3', 'https://jywg.18.cn/Trade/GetCanBuyNewStockListV3?validatekey=${validatekey}', 1),
  ('get_convertible_bond_list_v2', 'https://jywg.18.cn/Trade/GetConvertibleBondListV2?validatekey=${validatekey}', 1),
  ('submit_bat_trade_v2', 'https://jywg.18.cn/Trade/SubmitBatTradeV2?validatekey=${validatekey}', 1),
  ('cr_get_rzrq_asserts', 'https://jywg.18.cn/Com/queryAssetAndPositionV1?validatekey=${validatekey}', 1),
  ('cr_query_collateral', 'https://jywg.18.cn/Search/GetStockList?validatekey=${validatekey}', 1),
  ('cr_submit', 'https://jywg.18.cn/Trade/SubmitTradeV2?validatekey=${validatekey}', 1),
  ('cr_get_orders_data', 'https://jywg.18.cn/Search/GetOrdersData?validatekey=${validatekey}', 1),
  ('cr_get_deal_data', 'https://jywg.18.cn/Search/GetDealData?validatekey=${validatekey}', 1),
  ('cr_get_his_deal_data', 'https://jywg.18.cn/Search/GetHisDealData?validatekey=${validatekey}', 1),
  ('cr_revoke', 'https://jywg.18.cn/Trade/RevokeOrders?validatekey=${validatekey}', 1),
  ('cr_get_can_buy_new_stock_list_v3', 'https://jywg.18.cn/Trade/GetCanBuyNewStockListV3?validatekey=${validatekey}', 1),
  ('cr_get_convertible_bond_list_v2', 'https://jywg.18.cn/Trade/GetConvertibleBondListV2?validatekey=${validatekey}', 1),
  ('cr_submit_bat_trade_v2', 'https://jywg.18.cn/Trade/SubmitBatTradeV2?validatekey=${validatekey}', 1)
ON DUPLICATE KEY UPDATE
  `url` = VALUES(`url`),
  `state` = VALUES(`state`),
  `update_time` = CURRENT_TIMESTAMP;

