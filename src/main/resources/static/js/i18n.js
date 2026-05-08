/* ── AMR Control Tower — i18n ───────────────────────────────── */
const TRANSLATIONS = {
  en: {
    'nav.dashboard':       'Dashboard',
    'nav.fleet':           'Fleet',
    'nav.tasks':           'Tasks',
    'nav.admin':           'Admin',
    'page.title':          'Dashboard',
    'label.state':         'State',
    'label.telemetry':     'Telemetry',
    'label.battery':       'Battery',
    'label.session':       'Session',
    'label.robotId':       'Robot ID',
    'label.posX':          'Pos X',
    'label.posY':          'Pos Y',
    'label.yaw':           'Yaw',
    'label.linearVel':     'Linear vel',
    'label.angularVel':    'Angular vel',
    'label.distance':      'Distance (m)',
    'label.uptime':        'Uptime (s)',
    'map.title':           'SLAM Map',
    'map.lidar':           'LiDAR',
    'map.clearPath':       'Clear path',
    'map.hint':            'Click map to send nav goal',
    'chart.linearVel':     'Linear Velocity',
    'chart.ms':            'm/s',
    'chart.mission':       'Mission History',
    'ctrl.command':        'Command',
    'ctrl.estop':          'E-Stop',
    'ctrl.resume':         'Resume',
    'ctrl.sendGoal':       'Send goal',
    'ctrl.labelX':         'X (m)',
    'ctrl.labelY':         'Y (m)',
    'ctrl.labelTheta':     'Theta',
    'ctrl.velLimits':      'Velocity Limits',
    'ctrl.linear':         'Linear',
    'ctrl.angular':        'Angular',
    'ctrl.manualDrive':    'Manual Drive',
    'ctrl.kbHint':         'WASD / Arrows / Space=Stop',
    'event.title':         'Event Log',
    'conn.connected':      'Connected',
    'conn.disconnected':   'Disconnected',
    'label.units':         'Units',
    'state.IDLE':          'Idle',
    'state.MOVING':        'Moving',
    'state.EMERGENCY_STOP':'E-Stop',
    'state.CHARGING':      'Charging',
    'state.ERROR':         'Error',
  },
  ko: {
    'nav.dashboard':       '대시보드',
    'nav.fleet':           '플리트',
    'nav.tasks':           '태스크',
    'nav.admin':           '관리자',
    'page.title':          '대시보드',
    'label.state':         '상태',
    'label.telemetry':     '텔레메트리',
    'label.battery':       '배터리',
    'label.session':       '세션',
    'label.robotId':       '로봇 ID',
    'label.posX':          '위치 X',
    'label.posY':          '위치 Y',
    'label.yaw':           '방향각',
    'label.linearVel':     '선속도',
    'label.angularVel':    '각속도',
    'label.distance':      '이동거리 (m)',
    'label.uptime':        '가동시간 (s)',
    'map.title':           'SLAM 맵',
    'map.lidar':           '라이다',
    'map.clearPath':       '경로 초기화',
    'map.hint':            '맵 클릭 → 목표 전송',
    'chart.linearVel':     '선속도',
    'chart.ms':            'm/s',
    'chart.mission':       '미션 히스토리',
    'ctrl.command':        '명령',
    'ctrl.estop':          '긴급 정지',
    'ctrl.resume':         '재개',
    'ctrl.sendGoal':       '목표 전송',
    'ctrl.labelX':         'X (m)',
    'ctrl.labelY':         'Y (m)',
    'ctrl.labelTheta':     '방향 (rad)',
    'ctrl.velLimits':      '속도 제한',
    'ctrl.linear':         '선속도',
    'ctrl.angular':        '각속도',
    'ctrl.manualDrive':    '수동 조종',
    'ctrl.kbHint':         'WASD / 방향키 / Space=정지',
    'event.title':         '이벤트 로그',
    'conn.connected':      '연결됨',
    'conn.disconnected':   '연결 끊김',
    'label.units':         '대수',
    'state.IDLE':          '대기',
    'state.MOVING':        '이동 중',
    'state.EMERGENCY_STOP':'긴급 정지',
    'state.CHARGING':      '충전 중',
    'state.ERROR':         '오류',
  },
  ja: {
    'nav.dashboard':       'ダッシュボード',
    'nav.fleet':           'フリート',
    'nav.tasks':           'タスク',
    'nav.admin':           '管理',
    'page.title':          'ダッシュボード',
    'label.state':         '状態',
    'label.telemetry':     'テレメトリ',
    'label.battery':       'バッテリー',
    'label.session':       'セッション',
    'label.robotId':       'ロボットID',
    'label.posX':          '位置 X',
    'label.posY':          '位置 Y',
    'label.yaw':           'ヨー角',
    'label.linearVel':     '線速度',
    'label.angularVel':    '角速度',
    'label.distance':      '走行距離 (m)',
    'label.uptime':        '稼働時間 (s)',
    'map.title':           'SLAMマップ',
    'map.lidar':           'LiDAR',
    'map.clearPath':       '経路クリア',
    'map.hint':            'マップクリックで目標送信',
    'chart.linearVel':     '線速度',
    'chart.ms':            'm/s',
    'chart.mission':       'ミッション履歴',
    'ctrl.command':        'コマンド',
    'ctrl.estop':          '緊急停止',
    'ctrl.resume':         '再開',
    'ctrl.sendGoal':       '目標送信',
    'ctrl.labelX':         'X (m)',
    'ctrl.labelY':         'Y (m)',
    'ctrl.labelTheta':     '向き (rad)',
    'ctrl.velLimits':      '速度制限',
    'ctrl.linear':         '線速度',
    'ctrl.angular':        '角速度',
    'ctrl.manualDrive':    '手動操縦',
    'ctrl.kbHint':         'WASD / 矢印キー / Space=停止',
    'event.title':         'イベントログ',
    'conn.connected':      '接続済み',
    'conn.disconnected':   '切断',
    'label.units':         '台数',
    'state.IDLE':          'アイドル',
    'state.MOVING':        '移動中',
    'state.EMERGENCY_STOP':'緊急停止',
    'state.CHARGING':      '充電中',
    'state.ERROR':         'エラー',
  }
};

function getLang() {
  return localStorage.getItem('amr-lang') || 'en';
}

function t(key) {
  const lang = getLang();
  return TRANSLATIONS[lang]?.[key] ?? TRANSLATIONS['en'][key] ?? key;
}

function applyLang(lang) {
  if (!TRANSLATIONS[lang]) lang = 'en';
  localStorage.setItem('amr-lang', lang);
  document.documentElement.lang = lang;

  // 텍스트 노드 교체
  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.dataset.i18n;
    el.textContent = TRANSLATIONS[lang][key] ?? TRANSLATIONS['en'][key] ?? key;
  });

  // 랭 버튼 active 상태 업데이트
  document.querySelectorAll('.lang-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.lang === lang);
  });
}

function setLang(lang) {
  applyLang(lang);
}

// DOM 로드 후 저장된 언어 적용
document.addEventListener('DOMContentLoaded', () => applyLang(getLang()));
