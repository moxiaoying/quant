(function () {
  'use strict';
  function show(section) {
    document.querySelectorAll('.nav-item').forEach(function (item) { item.classList.toggle('active', item.dataset.section === section); });
    document.querySelectorAll('.page').forEach(function (page) { page.classList.toggle('active', page.dataset.page === section); });
    if (section === 'paper' || section === 'account') { loadAccount(); loadHoldings(); }
  }
  function json(url) { return fetch(url, { credentials: 'same-origin' }).then(function (r) { if (!r.ok) throw new Error(r.statusText); return r.json(); }); }
  function authToken() {
    try {
      var stored = JSON.parse(localStorage.getItem('auth-token') || '{}');
      return stored.expire > Date.now() ? stored.value : '';
    } catch (e) {
      return '';
    }
  }
  var authUserKey = 'stock-auth-user';
  function authUser() {
    try {
      return JSON.parse(localStorage.getItem(authUserKey) || 'null');
    } catch (e) {
      return null;
    }
  }
  function saveAuth(user) {
    localStorage.setItem(authUserKey, JSON.stringify({
      username: user.username || '',
      name: user.name || ''
    }));
    localStorage.setItem('auth-token', JSON.stringify({
      expire: Date.now() + 24 * 60 * 60 * 1000,
      value: user.token
    }));
  }
  function clearAuth() {
    localStorage.removeItem('auth-token');
    localStorage.removeItem(authUserKey);
  }
  function renderAuth() {
    var status = document.getElementById('auth-status');
    var login = document.getElementById('auth-login-btn');
    var logout = document.getElementById('auth-logout-btn');
    var panel = document.getElementById('auth-panel');
    var user = authUser();
    var token = authToken();
    var loggedIn = !!(user && token);
    if (status) status.textContent = loggedIn ? ('已登录：' + (user.name || user.username)) : '未登录';
    if (login) login.hidden = loggedIn;
    if (logout) logout.hidden = !loggedIn;
    if (panel && loggedIn) panel.hidden = true;
  }
  function initAuth() {
    var panel = document.getElementById('auth-panel');
    var loginButton = document.getElementById('auth-login-btn');
    var logoutButton = document.getElementById('auth-logout-btn');
    var form = document.getElementById('auth-form');
    var message = document.getElementById('auth-message');
    if (!panel || !loginButton || !logoutButton || !form) return;
    renderAuth();
    loginButton.addEventListener('click', function () {
      panel.hidden = !panel.hidden;
      if (!panel.hidden) form.querySelector('[name="username"]').focus();
    });
    logoutButton.addEventListener('click', function () {
      clearAuth();
      if (message) message.textContent = '';
      renderAuth();
      refreshDataSyncStatus();
    });
    form.addEventListener('submit', function (event) {
      event.preventDefault();
      if (message) message.textContent = '登录中...';
      fetch('/api/user/login', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: new URLSearchParams(new FormData(form)).toString()
      }).then(function (response) {
        return response.json().then(function (data) {
          if (!response.ok) throw new Error(data.message || '用户名或密码错误');
          return data;
        });
      }).then(function (user) {
        saveAuth(user);
        form.reset();
        if (message) message.textContent = '登录成功';
        renderAuth();
        refreshDataSyncStatus();
      }).catch(function (error) {
        if (message) message.textContent = error.message || '登录失败';
      });
    });
  }
  var syncStatusLoading = false;
  function refreshDataSyncStatus() {
    if (syncStatusLoading) return;
    syncStatusLoading = true;
    fetch('/api/system/stockDataSyncStatus', {
      credentials: 'same-origin',
      headers: { 'auth-token': authToken() }
    }).then(function (response) {
      if (!response.ok) throw new Error(response.status === 401 ? '登录状态已失效，请重新登录' : '状态查询失败（HTTP ' + response.status + '）');
      return response.json();
    }).then(function (data) {
      var button = document.getElementById('data-sync-start-btn');
      var status = document.getElementById('data-sync-status');
      if (!button || !status) return;
      button.disabled = !!data.running;
      status.textContent = data.running ? '全市场数据同步进行中，请勿重复启动' : (data.message || '空闲');
      status.classList.toggle('status-error', !data.running && Number(data.exitCode) < 0);
      var taskState = document.querySelector('.task-status');
      if (taskState) {
        taskState.textContent = data.running ? '运行中' : (Number(data.exitCode) < 0 ? '失败' : (Number(data.exitCode) === 0 && data.message !== '未执行' ? '已完成' : '待启动'));
        taskState.classList.toggle('status-error', !data.running && Number(data.exitCode) < 0);
      }
    }).catch(function (error) {
      if (/登录状态已失效/.test(error.message || '')) {
        clearAuth();
        renderAuth();
      }
      var status = document.getElementById('data-sync-status');
      if (status) status.textContent = error.message || '无法读取同步状态';
    }).then(function () {
      syncStatusLoading = false;
    }, function () {
      syncStatusLoading = false;
    });
  }
  function startDataSync() {
    var mode = document.getElementById('data-task-mode');
    var type = document.getElementById('data-task-type');
    var button = document.getElementById('data-sync-start-btn');
    var status = document.getElementById('data-sync-status');
    var startDate = document.getElementById('data-sync-start');
    if (!button || !status || !startDate || !startDate.value) {
      if (status) status.textContent = '请先选择起始日期';
      return;
    }
    var selectedEndDate = document.getElementById('data-sync-end');
    if (selectedEndDate && selectedEndDate.value && selectedEndDate.value < startDate.value) {
      status.textContent = '结束日期不能早于开始日期';
      return;
    }
    if (type && type.value !== 'daily') {
      status.textContent = '该数据类型尚未接入';
      return;
    }
    button.disabled = true;
    status.textContent = mode && mode.value === 'backfill' ? '正在提交历史回填任务...' : '正在提交增量同步任务...';
    var endDate = document.getElementById('data-sync-end');
    var body = new URLSearchParams({
      backfill: mode && mode.value === 'backfill' ? 'true' : 'false',
      startDate: startDate.value,
      endDate: endDate && endDate.value ? endDate.value : ''
    });
    fetch('/api/system/stockDataSync', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'auth-token': authToken(), 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
      body: body.toString()
    }).then(function (response) {
      if (!response.ok) throw new Error(response.status === 401 ? '登录状态已失效，请重新登录' : '启动失败（HTTP ' + response.status + '）');
      return response.json();
    }).then(function (data) {
      status.textContent = data.message || (data.started ? '同步任务已启动' : '同步任务未启动');
      refreshDataSyncStatus();
    }).catch(function (error) {
      button.disabled = false;
      status.textContent = error.message || '启动同步失败';
    });
  }
  function openDataTaskEditor() {
    var editor = document.getElementById('data-task-form');
    if (editor) editor.hidden = false;
  }
  function closeDataTaskEditor() {
    var editor = document.getElementById('data-task-form');
    if (editor) editor.hidden = true;
  }
  function saveDataTaskConfig() {
    var start = document.getElementById('data-sync-start');
    var end = document.getElementById('data-sync-end');
    var mode = document.getElementById('data-task-mode');
    var frequency = document.getElementById('data-task-frequency');
    if (!start || !start.value) return;
    localStorage.setItem('stock-data-task', JSON.stringify({
      start: start.value,
      end: end && end.value ? end.value : '',
      mode: mode ? mode.value : 'incremental',
      frequency: frequency ? frequency.value : '每日'
    }));
    var row = document.querySelector('.task-table tbody tr');
    if (row) {
      row.children[1].textContent = frequency ? frequency.value : '每日';
      row.children[2].textContent = start.value;
      row.children[3].textContent = end && end.value ? end.value : '至今';
      row.children[4].textContent = mode && mode.value === 'backfill' ? '历史回填' : '增量同步并补齐';
    }
    closeDataTaskEditor();
    var status = document.getElementById('data-sync-status');
    if (status) status.textContent = '任务配置已保存';
  }
  function loadDataTaskConfig() {
    try {
      var config = JSON.parse(localStorage.getItem('stock-data-task') || 'null');
      if (!config) return;
      var start = document.getElementById('data-sync-start');
      var end = document.getElementById('data-sync-end');
      var mode = document.getElementById('data-task-mode');
      var frequency = document.getElementById('data-task-frequency');
      if (start && config.start) start.value = config.start;
      if (end) end.value = config.end || '';
      if (mode && config.mode) mode.value = config.mode;
      if (frequency && config.frequency) frequency.value = config.frequency;
      saveDataTaskConfig();
    } catch (e) {}
  }
  var dataStartButton = document.getElementById('data-sync-start-btn');
  if (dataStartButton) dataStartButton.addEventListener('click', startDataSync);
  var dataNewButton = document.getElementById('data-task-new-btn');
  if (dataNewButton) dataNewButton.addEventListener('click', openDataTaskEditor);
  var dataEditButton = document.getElementById('data-task-edit-btn');
  if (dataEditButton) dataEditButton.addEventListener('click', openDataTaskEditor);
  var dataCancelButton = document.getElementById('data-task-cancel-btn');
  if (dataCancelButton) dataCancelButton.addEventListener('click', closeDataTaskEditor);
  var dataTaskForm = document.getElementById('data-task-form');
  if (dataTaskForm) {
    dataTaskForm.addEventListener('submit', function (event) {
      event.preventDefault();
      saveDataTaskConfig();
    });
    refreshDataSyncStatus();
    window.setInterval(refreshDataSyncStatus, 5000);
  }
  loadDataTaskConfig();
  initAuth();
  function money(value) { return '¥' + Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  function setService(text, ok) { var el = document.querySelector('.service'); if (el) { el.textContent = '● ' + text; el.style.color = ok ? '#71BB91' : '#D88917'; } }
  function loadHealth() { json('/api/market/health').then(function (r) { setService('行情服务正常（' + (r.source || 'mock') + '）', !!r.success); }).catch(function () { setService('后端未连接', false); }); }
  function loadMarket() {
    json('/api/market/stocks').then(function (r) {
      var rows = r.data || [], table = document.querySelector('[data-page="market"] tbody');
      if (!table || !rows.length) return;
      table.innerHTML = rows.map(function (s) {
        var symbol = stockSymbol(s.code, s.exchange);
        return '<tr data-symbol="' + symbol + '" data-name="' + (s.name || '') + '"><td class="gold">' + (s.code || '') + '</td><td>' + (s.name || '') + '</td><td>' + (s.price || '—') + '</td><td>—</td><td>—</td><td>—</td><td>—</td><td>—</td><td>—</td><td>—</td><td>☆</td></tr>';
      }).join('');
    }).catch(function () {});
  }
  function stockSymbol(code, exchange) {
    var value = String(code || '').toLowerCase().trim();
    if (value.indexOf('.') >= 0 || /^(sh|sz|bj)/.test(value)) return canonicalCode(value);
    var market = exchange;
    if (!market) market = /^(5|6|9)/.test(value) ? 'sh' : (/^(4|8)/.test(value) ? 'bj' : 'sz');
    return canonicalCode(market + '.' + value);
  }
  function canonicalCode(code) {
    var value = String(code || '').toLowerCase().trim();
    if (value.indexOf('.') >= 0) return value;
    if (/^(sh|sz|bj)/.test(value)) return value.slice(0, 2) + '.' + value.slice(2);
    return value;
  }
  var indexCodes = ['sh.000001', 'sz.399001', 'sz.399006', 'sh.000300', 'sh.000688', 'sh.000680'];
  var indexNames = { 'sh.000001': '上证指数', 'sz.399001': '深证指数', 'sz.399006': '创业板指', 'sh.000300': '沪深300', 'sh.000688': '科创50', 'sh.000680': '科创综指' };
  var indexRows = [];
  var activeIndex = 0;
  var indexLoading = false;
  var tickerInitialized = false;
  function tickerItems() {
    return indexCodes.map(function (code) {
      return '<span class="ticker-item" data-code="' + code + '"><b>' + indexNames[code] +
        '</b> <strong class="ticker-price">—</strong> <em class="ticker-change">等待行情</em></span>';
    }).join('');
  }
  function initializeTicker() {
    var ticker = document.querySelector('.ticker');
    if (!ticker || tickerInitialized) return;
    ticker.innerHTML = '<div class="ticker-track"><div class="ticker-group">' + tickerItems() +
      '</div><div class="ticker-group" aria-hidden="true">' + tickerItems() + '</div></div>';
    tickerInitialized = true;
  }
  function renderIndices(rows) {
    var panel = document.querySelector('[data-page="market"] .panel');
    if (!panel || !rows || !rows.length) return;
    indexRows = rows;
    var byCode = {}; rows.forEach(function (x) { byCode[canonicalCode(x.code)] = x; });
    var tabs = panel.querySelectorAll('.tabs button');
    tabs.forEach(function (button, i) { button.dataset.code = indexCodes[i]; });
    var quote = byCode[canonicalCode(indexCodes[activeIndex])];
    var chart = panel.querySelector('.chart');
    if (!quote || !chart) return;
    var close = Number(quote.closingPrice || 0), pre = Number(quote.preClosingPrice || 0);
    var change = pre ? (close - pre) / pre * 100 : 0;
    var color = change >= 0 ? '#DB777E' : '#71BB91';
    chart.innerHTML = '<div class="index-quote"><strong>' + (close ? close.toFixed(2) : '—') + '</strong><span style="color:' + color + '">' + (change >= 0 ? '+' : '') + change.toFixed(2) + '%</span><small>' + (indexNames[quote.code] || quote.code) + ' · 真实行情 · 5 秒刷新</small></div><div class="index-stats">开 ' + (quote.openingPrice || '—') + '　高 ' + (quote.highestPrice || '—') + '　低 ' + (quote.lowestPrice || '—') + '　更新时间 ' + (quote.date || '—') + '</div>';
    tabs.forEach(function (button, i) {
      button.classList.toggle('active', i === activeIndex);
      button.onclick = function () { activeIndex = i; renderIndices(indexRows); };
    });
    renderTicker(byCode);
  }
  function renderTicker(byCode) {
    var ticker = document.querySelector('.ticker');
    if (!ticker) return;
    initializeTicker();
    ticker.querySelectorAll('.ticker-item').forEach(function (item) {
      var quote = byCode[canonicalCode(item.dataset.code)];
      if (!quote) return;
      var close = Number(quote.closingPrice || 0), pre = Number(quote.preClosingPrice || 0);
      var change = pre ? (close - pre) / pre * 100 : 0;
      var price = item.querySelector('.ticker-price');
      var percent = item.querySelector('.ticker-change');
      if (price) price.textContent = close ? close.toFixed(2) : '—';
      if (percent) {
        percent.textContent = (change >= 0 ? '+' : '') + change.toFixed(2) + '%';
        percent.classList.toggle('up', change >= 0);
        percent.classList.toggle('down', change < 0);
      }
    });
  }
  function loadIndices() {
    if (indexLoading) return;
    indexLoading = true;
    json('/api/market/indices?codes=' + indexCodes.join(',')).then(function (r) { if (r.success) renderIndices(r.data); }).catch(function () {
      var chart = document.querySelector('[data-page="market"] .chart'); if (chart) chart.innerHTML = '<div class="empty">后端未连接，无法获取新浪行情</div>';
      document.querySelectorAll('.ticker-change').forEach(function (item) { item.textContent = '后端未连接'; });
    }).then(function () { indexLoading = false; }, function () { indexLoading = false; });
  }
  function renderIntraday(points) {
    var chart = document.querySelector('[data-page="market"] .chart');
    if (!chart || !points || !points.length) return;
    var width = 900, height = 220, values = points.map(function (p) { return Number(p.price); });
    var min = Math.min.apply(null, values), max = Math.max.apply(null, values);
    var range = max - min || 1;
    var path = values.map(function (value, i) {
      var x = values.length === 1 ? 0 : i * width / (values.length - 1);
      var y = height - ((value - min) / range) * (height - 20) - 10;
      return (i ? 'L' : 'M') + x.toFixed(1) + ' ' + y.toFixed(1);
    }).join(' ');
    var quote = indexRows[activeIndex];
    var close = quote ? Number(quote.closingPrice || 0) : values[values.length - 1];
    var pre = quote ? Number(quote.preClosingPrice || 0) : 0;
    var change = pre ? (close - pre) / pre * 100 : 0;
    var color = change >= 0 ? '#DB777E' : '#71BB91';
    chart.innerHTML = '<div class="index-quote"><strong>' + close.toFixed(2) + '</strong><span style="color:' + color + '">' + (change >= 0 ? '+' : '') + change.toFixed(2) + '%</span><small>' + indexNames[indexCodes[activeIndex]] + ' · 新浪分时 · 5 秒刷新</small></div><svg class="intraday-chart" viewBox="0 0 900 220" preserveAspectRatio="none"><path d="' + path + '" fill="none" stroke="' + color + '" stroke-width="3"/></svg><div class="index-stats">分时点数 ' + points.length + '　最新价 ' + values[values.length - 1].toFixed(2) + '　更新时间 ' + (points[points.length - 1].time || '—') + '</div>';
  }
  function loadIntraday() {
    var code = indexCodes[activeIndex];
    json('/api/market/intraday?code=' + code).then(function (r) {
      if (r.success) renderIntraday(r.data);
    }).catch(function () {});
  }
  var detailState = { symbol: '', name: '', period: 'day', indicator: 'macd', rows: [] };
  function ensureDetailView() {
    var market = document.querySelector('[data-page="market"]');
    if (!market || document.getElementById('stock-detail-view')) return;
    var listPanel = market.querySelectorAll('.panel')[1];
    if (listPanel) {
      var title = listPanel.querySelector('.panel-title');
      if (title && title.firstChild) title.firstChild.textContent = '个股明细 ';
      var tbody = listPanel.querySelector('tbody');
      if (tbody && !tbody.dataset.detailHandlerAttached) {
        tbody.dataset.detailHandlerAttached = 'true';
        tbody.addEventListener('dblclick', function (event) {
          var row = event.target.closest('tr');
          if (!row || !tbody.contains(row)) return;
          var code = row.dataset.symbol;
          var codeCell = row.cells[0];
          var nameCell = row.cells[1];
          if (!code && codeCell) {
            code = stockSymbol(codeCell.textContent, row.dataset.exchange);
            row.dataset.symbol = code;
          }
          if (!code) return;
          openStockDetail(code, row.dataset.name || (nameCell && nameCell.textContent.trim()));
        });
      }
    }
    var view = document.createElement('div');
    view.id = 'stock-detail-view'; view.className = 'stock-detail-view'; view.hidden = true;
    view.innerHTML = '<div class="panel"><div class="panel-title"><span><button id="stock-detail-back" class="table-action" type="button">← 返回个股明细</button> <strong id="stock-detail-title">个股详情</strong></span><small id="stock-detail-status">stock_daily</small></div><div class="detail-toolbar"><span id="stock-detail-code">--</span><button class="period-btn active" data-period="day">日K</button><button class="period-btn" data-period="week">周K</button><button class="period-btn" data-period="month">月K</button><button class="period-btn" data-period="year">年K</button><span class="indicator-label">主图</span><button class="indicator-btn active" data-indicator="ma">MA</button><button class="indicator-btn" data-indicator="boll">BOLL</button><span class="indicator-label">副图</span><button class="indicator-btn active" data-indicator="macd">MACD</button><button class="indicator-btn" data-indicator="rsi">RSI</button><button class="indicator-btn" data-indicator="kdj">KDJ</button></div><div class="detail-layout"><div class="detail-chart-area"><div id="stock-kline-chart" class="stock-kline-chart"></div><div id="stock-volume-chart" class="stock-subchart"></div><div id="stock-indicator-chart" class="stock-subchart"></div></div><aside class="detail-side"><div class="detail-quote"><h3 id="detail-name">个股行情</h3><div class="detail-price" id="detail-price">--</div><div id="detail-change">--</div><dl><dt>最新日期</dt><dd id="detail-date">--</dd><dt>开 / 收</dt><dd id="detail-open-close">--</dd><dt>最高 / 最低</dt><dd id="detail-high-low">--</dd><dt>成交量</dt><dd id="detail-volume">--</dd></dl></div><div class="detail-order"><h3>快速下单（模拟）</h3><label>方向<select><option>买入</option><option>卖出</option></select></label><label>委托价<input id="detail-order-price" readonly></label><label>数量<input value="100" readonly></label><button class="gold-btn" type="button" disabled>只读模式</button></div></aside></div></div>';
    market.appendChild(view);
    view.querySelector('#stock-detail-back').onclick = closeStockDetail;
    view.querySelectorAll('.period-btn').forEach(function (b) { b.onclick = function () { detailState.period = b.dataset.period; view.querySelectorAll('.period-btn').forEach(function (x) { x.classList.toggle('active', x === b); }); loadStockDetail(); }; });
    view.querySelectorAll('.indicator-btn').forEach(function (b) { b.onclick = function () { detailState.indicator = b.dataset.indicator; view.querySelectorAll('.indicator-btn').forEach(function (x) { x.classList.toggle('active', x === b); }); renderStockDetail(); }; });
  }
  function openStockDetail(symbol, name) {
    ensureDetailView(); detailState.symbol = canonicalCode(symbol); detailState.name = name || detailState.symbol;
    document.getElementById('market-list-view') && (document.getElementById('market-list-view').hidden = true);
    var panels = document.querySelectorAll('[data-page="market"] > .panel'); if (panels[1]) panels[1].hidden = true;
    document.getElementById('stock-detail-view').hidden = false; loadStockDetail();
  }
  function closeStockDetail() {
    var view = document.getElementById('stock-detail-view'); if (view) view.hidden = true;
    var panels = document.querySelectorAll('[data-page="market"] > .panel'); if (panels[1]) panels[1].hidden = false;
  }
  function lineSvg(rows, field, color, height) {
    var values = rows.map(function (r) { return Number(r[field] || 0); }), width = 900;
    if (!values.length) return ''; var min = Math.min.apply(null, values), max = Math.max.apply(null, values), range = max - min || 1;
    var path = values.map(function (v, i) { var x = i * width / Math.max(1, values.length - 1), y = height - ((v - min) / range) * (height - 18) - 9; return (i ? 'L' : 'M') + x.toFixed(1) + ' ' + y.toFixed(1); }).join(' ');
    return '<path d="' + path + '" fill="none" stroke="' + color + '" stroke-width="2"/>';
  }
  function candleSvg(rows, height) {
    if (!rows.length) return '';
    var width = 900, pad = 10, min = Infinity, max = -Infinity;
    rows.forEach(function (r) {
      min = Math.min(min, Number(r.lowestPrice), Number(r.openingPrice), Number(r.closingPrice));
      max = Math.max(max, Number(r.highestPrice), Number(r.openingPrice), Number(r.closingPrice));
    });
    var range = max - min || 1, step = width / rows.length, bodyWidth = Math.max(2, Math.min(10, step * 0.62));
    function y(value) { return height - pad - ((value - min) / range) * (height - pad * 2); }
    return rows.map(function (r, i) {
      var open = Number(r.openingPrice), close = Number(r.closingPrice);
      var high = Number(r.highestPrice), low = Number(r.lowestPrice), x = i * step + step / 2;
      var rising = close >= open, color = rising ? '#db777e' : '#71bb91';
      var top = y(Math.max(open, close)), bodyHeight = Math.max(1, Math.abs(y(open) - y(close)));
      return '<line x1="' + x.toFixed(2) + '" y1="' + y(high).toFixed(2) + '" x2="' + x.toFixed(2) + '" y2="' + y(low).toFixed(2) + '" stroke="' + color + '" stroke-width="1"/>' +
        '<rect x="' + (x - bodyWidth / 2).toFixed(2) + '" y="' + top.toFixed(2) + '" width="' + bodyWidth.toFixed(2) + '" height="' + bodyHeight.toFixed(2) + '" fill="' + (rising ? 'transparent' : color) + '" stroke="' + color + '" stroke-width="1"/>';
    }).join('');
  }
  function renderStockDetail() {
    var rows = detailState.rows, last = rows[rows.length - 1]; if (!last) return;
    document.getElementById('stock-detail-title').textContent = detailState.name;
    document.getElementById('stock-detail-code').textContent = detailState.symbol + ' · ' + detailState.period + 'K';
    document.getElementById('detail-price').textContent = Number(last.closingPrice).toFixed(2);
    document.getElementById('detail-date').textContent = (last.date || '').slice(0, 10);
    document.getElementById('detail-open-close').textContent = Number(last.openingPrice).toFixed(2) + ' / ' + Number(last.closingPrice).toFixed(2);
    document.getElementById('detail-high-low').textContent = Number(last.highestPrice).toFixed(2) + ' / ' + Number(last.lowestPrice).toFixed(2);
    document.getElementById('detail-volume').textContent = Number(last.tradingVolume || 0).toLocaleString();
    document.getElementById('detail-order-price').value = Number(last.closingPrice).toFixed(2);
    var main = detailState.indicator === 'boll' ? lineSvg(rows, 'bollUpper', '#d5bd62', 250) + lineSvg(rows, 'bollMiddle', '#9fa3ab', 250) + lineSvg(rows, 'bollLower', '#d5bd62', 250) : lineSvg(rows, 'ma5', '#db777e', 250) + lineSvg(rows, 'ma10', '#71bb91', 250) + lineSvg(rows, 'ma20', '#d5bd62', 250);
    document.getElementById('stock-kline-chart').innerHTML = '<div class="chart-caption">' + detailState.period.toUpperCase() + 'K蜡烛图 / ' + detailState.indicator.toUpperCase() + '</div><svg viewBox="0 0 900 250" preserveAspectRatio="none">' + candleSvg(rows, 250) + main + '</svg>';
    document.getElementById('stock-volume-chart').innerHTML = '<div class="chart-caption">成交量</div><svg viewBox="0 0 900 100" preserveAspectRatio="none">' + lineSvg(rows, 'tradingVolume', '#71bb91', 100) + '</svg>';
    var sub = detailState.indicator === 'rsi' ? lineSvg(rows, 'rsi', '#d5bd62', 100) : detailState.indicator === 'kdj' ? lineSvg(rows, 'k', '#db777e', 100) + lineSvg(rows, 'd', '#71bb91', 100) + lineSvg(rows, 'j', '#d5bd62', 100) : lineSvg(rows, 'macd', '#db777e', 100) + lineSvg(rows, 'macdSignal', '#71bb91', 100);
    document.getElementById('stock-indicator-chart').innerHTML = '<div class="chart-caption">' + detailState.indicator.toUpperCase() + '</div><svg viewBox="0 0 900 100" preserveAspectRatio="none">' + sub + '</svg>';
  }
  function loadStockDetail() {
    json('/api/market/kline?code=' + encodeURIComponent(detailState.symbol) + '&period=' + detailState.period + '&limit=240').then(function (r) { detailState.rows = r.data || []; renderStockDetail(); }).catch(function () { document.getElementById('stock-detail-status').textContent = '历史数据不可用'; });
  }
  function loadAccount() {
    json('/api/trade/queryAccount').then(function (a) {
      document.querySelectorAll('[data-page="paper"] .metrics b,[data-page="account"] .metrics b').forEach(function (el, i) { var vals = [a.totalAmount, a.availableAmount, 0, 0]; el.textContent = money(vals[i % 4]); });
      var header = document.querySelector('.actions strong'); if (header) header.textContent = '账户资产 ' + money(a.totalAmount);
    }).catch(function () {});
  }
  function loadHoldings() {
    json('/api/trade/holdings?start=0&length=100').then(function (r) {
      var rows = r.data || [];
      ['paper', 'account'].forEach(function (name) { var page = document.querySelector('[data-page="' + name + '"]'), empty = page && page.querySelector('.empty'); if (!empty || !rows.length) return; var table = document.createElement('table'); table.innerHTML = '<thead><tr><th>代码</th><th>名称</th><th>持仓</th><th>可用</th><th>成本价</th><th>最新价</th><th>盈亏</th><th>收益率</th></tr></thead><tbody>' + rows.map(function (x) { return '<tr><td class="gold">' + (x.stockCode || '') + '</td><td>' + (x.name || '') + '</td><td>' + (x.totalVolume || 0) + '</td><td>' + (x.availableVolume || 0) + '</td><td>' + (x.costPrice || '—') + '</td><td>' + (x.price || '—') + '</td><td>' + (x.profit || '—') + '</td><td>' + (x.rate || '—') + '</td></tr>'; }).join('') + '</tbody>'; empty.replaceWith(table); });
    }).catch(function () {});
  }
  document.querySelectorAll('[data-section]').forEach(function (item) { item.addEventListener('click', function () { show(item.dataset.section); }); });
  initializeTicker();
  ensureDetailView();
  loadHealth(); loadMarket(); loadIndices(); loadIntraday();
  window.setInterval(function () { loadIndices(); loadIntraday(); }, 5000);
  loadAccount();
}());
