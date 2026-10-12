package vip.linhs.stock.dao.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.stereotype.Repository;

import vip.linhs.stock.dao.BaseDao;
import vip.linhs.stock.dao.DailyIndexDao;
import vip.linhs.stock.model.po.DailyIndex;
import vip.linhs.stock.model.vo.DailyIndexVo;
import vip.linhs.stock.model.vo.PageParam;
import vip.linhs.stock.model.vo.PageVo;
import vip.linhs.stock.util.SqlCondition;

@Repository
public class DailyIndexDaoImpl extends BaseDao implements DailyIndexDao {

    private static final String INSERT_SQL = "insert into stock_daily(symbol, trade_date, open_price, pre_close_price, high_price, close_price, low_price, volume, amount, turnover, adjust_flag, source) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 3, 'legacy-import') on duplicate key update open_price=values(open_price), pre_close_price=values(pre_close_price), high_price=values(high_price), close_price=values(close_price), low_price=values(low_price), volume=values(volume), amount=values(amount), turnover=values(turnover), updated_at=current_timestamp";

    @Override
    public void save(List<DailyIndex> list) {
        jdbcTemplate.batchUpdate(DailyIndexDaoImpl.INSERT_SQL, list, list.size(),
                DailyIndexDaoImpl::setArgument);
    }

    private static void setArgument(PreparedStatement ps, DailyIndex dailyIndex)
            throws SQLException {
        ps.setString(1, dailyIndex.getCode());
        StatementCreatorUtils.setParameterValue(ps, 2, Types.DATE,
                dailyIndex.getDate());
        ps.setBigDecimal(3, dailyIndex.getOpeningPrice());
        ps.setBigDecimal(4, dailyIndex.getPreClosingPrice());
        ps.setBigDecimal(5, dailyIndex.getHighestPrice());
        ps.setBigDecimal(6, dailyIndex.getClosingPrice());
        ps.setBigDecimal(7, dailyIndex.getLowestPrice());
        ps.setLong(8, dailyIndex.getTradingVolume());
        ps.setBigDecimal(9, dailyIndex.getTradingValue());
        ps.setBigDecimal(10, dailyIndex.getRurnoverRate());
    }

    @Override
    public PageVo<DailyIndexVo> getDailyIndexList(PageParam pageParam) {
        String sql = "select"
            + " s.name, s.abbreviation, d.symbol as code, d.trade_date as date,"
            + " d.pre_close_price as preClosingPrice, d.close_price as closingPrice,"
            + " d.low_price as lowestPrice, d.high_price as highestPrice,"
            + " d.open_price as openingPrice, d.amount as tradingValue,"
            + " d.volume as tradingVolume, d.turnover as rurnoverRate"
            + " from stock_daily d join stock_info s on d.symbol = concat(s.exchange, '.', s.code)"
            + " where d.adjust_flag = 3";

        SqlCondition dataSqlCondition = new SqlCondition(sql, pageParam.getCondition());
        dataSqlCondition.addString("date", "date");

        Integer totalRecords = jdbcTemplate.queryForObject(dataSqlCondition.getCountSql(), Integer.class, dataSqlCondition.toArgs());

        dataSqlCondition.addSort("tradingValue", false, true);
        dataSqlCondition.addSql(" limit ?, ?");
        dataSqlCondition.addPage(pageParam.getStart(), pageParam.getLength());

        List<DailyIndexVo> list = jdbcTemplate.query(dataSqlCondition.toSql(),
                BeanPropertyRowMapper.newInstance(DailyIndexVo.class), dataSqlCondition.toArgs());
        return new PageVo<>(list, totalRecords);
    }

    @Override
    public List<DailyIndex> getDailyIndexListByDate(Date date) {
        String sql = "select"
            + " id, symbol as code, trade_date as date, pre_close_price as preClosingPrice,"
            + " close_price as closingPrice, low_price as lowestPrice,"
            + " high_price as highestPrice, open_price as openingPrice,"
            + " amount as tradingValue, volume as tradingVolume,"
            + " turnover as rurnoverRate"
            + " from stock_daily where adjust_flag = 3 and trade_date = ?";
        List<DailyIndex> list = jdbcTemplate.query(sql, BeanPropertyRowMapper.newInstance(DailyIndex.class),
                new java.sql.Date(date.getTime()));
        return list;
    }

    @Override
    public List<DailyIndex> getDailyHistory(String symbol, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 2000));
        String sql = "select symbol as code, trade_date as date, pre_close_price as preClosingPrice,"
            + " close_price as closingPrice, low_price as lowestPrice, high_price as highestPrice,"
            + " open_price as openingPrice, amount as tradingValue, volume as tradingVolume,"
            + " turnover as rurnoverRate from stock_daily"
            + " where symbol = ? and adjust_flag = 3 order by trade_date desc limit " + safeLimit;
        List<DailyIndex> list = jdbcTemplate.query(sql,
                BeanPropertyRowMapper.newInstance(DailyIndex.class), symbol);
        list.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        return list;
    }

}
