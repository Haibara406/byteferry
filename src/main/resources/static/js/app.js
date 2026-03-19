/* ==================== State ==================== */
let qType = 'text', qFiles = [];
let sessCode = null, sessAttach = [], sessTimerId = null;
let recvCode = null, recvPollId = null;
let spAttach = [], spPollId = null, spWs = null;
let friendWs = null, activeFSession = null, fSessionAttach = [], fSessionTimerId = null, fChatFriendId = null;
let notifications = [];
let closedSessions = new Set(); // Track closed sessions to prevent re-entry
let activeHistoryId = null;
let xhsImages = [];
let xhsVideos = [];
let currentMainTab = 'quick', currentSubTab = null;
const LOGIN_MEMORY_KEY = 'bf_login_memory';

/* ==================== DOM Ready ==================== */
document.addEventListener('DOMContentLoaded', () => {
    // Mode nav
    document.querySelectorAll('#mode-nav .pill').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#mode-nav .pill').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            show('view-quick', btn.dataset.mode === 'quick');
            show('view-session', btn.dataset.mode === 'session');
            show('view-xhs', btn.dataset.mode === 'xhs');
            show('view-space', btn.dataset.mode === 'space');
            show('view-friend', btn.dataset.mode === 'friend');
            if (btn.dataset.mode !== 'session') stopRecvPoll();
            if (btn.dataset.mode === 'space') { refreshSpace(); connectSpaceWS(); }
            else { stopSpacePoll(); disconnectSpaceWS(); }
            if (btn.dataset.mode === 'friend') { refreshFriendList(); refreshRequests(); }
            currentMainTab = btn.dataset.mode;
            currentSubTab = null;
            saveHash();
        });
    });

    // Quick sub-tabs
    bindTabs('quick-nav', { 'q-send': 'q-send', 'q-recv': 'q-recv' });

    // Session sub-tabs
    bindTabs('sess-nav', { 's-send': 's-send', 's-recv': 's-recv' });

    // Type chips
    document.querySelectorAll('#type-bar .type-chip').forEach(btn => {
        btn.addEventListener('click', () => {
            qType = btn.dataset.type; qFiles = [];
            document.querySelectorAll('#type-bar .type-chip').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            show('ia-text', qType === 'text');
            show('ia-image', qType === 'image');
            show('ia-file', qType === 'file');
            resetQuickUpload();
        });
    });

    // Dropzones
    setupDZ('dz-image', 'img-input', files => addQFiles(files, true));
    setupDZ('dz-file', 'file-input', files => addQFiles(files, false));

    // Quick send
    $('q-send-btn').addEventListener('click', quickSend);
    $('q-copy-code').addEventListener('click', () => copyText($('q-code').textContent));

    // Quick receive
    $('qr-get-btn').addEventListener('click', quickRecv);
    $('qr-code').addEventListener('keydown', e => { if (e.key === 'Enter') quickRecv(); });
    $('xhs-extract-btn').addEventListener('click', xhsExtractImages);
    $('xhs-url').addEventListener('keydown', e => {
        if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
            e.preventDefault();
            xhsExtractImages();
        }
    });
    $('xhs-copy-links').addEventListener('click', xhsCopyLinks);
    $('xhs-select-all').addEventListener('click', xhsSelectAll);
    $('xhs-download-selected').addEventListener('click', xhsDownloadSelected);

    // Session expire chips
    document.querySelectorAll('#expire-bar .type-chip').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#expire-bar .type-chip').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
        });
    });

    // Session create
    $('s-create-btn').addEventListener('click', sessCreate);
    $('s-copy-code').addEventListener('click', () => copyText($('s-code').textContent));
    $('s-code').addEventListener('click', () => copyText($('s-code').textContent));
    $('s-close-btn').addEventListener('click', sessClose);
    $('s-send-btn').addEventListener('click', sessSend);

    // Session attach buttons
    $('s-attach-file-btn').addEventListener('click', () => $('s-file-in').click());
    $('s-attach-img-btn').addEventListener('click', () => $('s-img-in').click());
    $('s-file-in').addEventListener('change', e => { sessAttach = sessAttach.concat(Array.from(e.target.files)); e.target.value = ''; renderSessAttach(); });
    $('s-img-in').addEventListener('change', e => { sessAttach = sessAttach.concat(Array.from(e.target.files)); e.target.value = ''; renderSessAttach(); });
    bindClipboardFiles('s-text', files => { sessAttach = sessAttach.concat(files); renderSessAttach(); });

    // Session join
    $('sr-join-btn').addEventListener('click', sessJoin);
    $('sr-code').addEventListener('keydown', e => { if (e.key === 'Enter') sessJoin(); });
    $('sr-leave-btn').addEventListener('click', sessLeave);
    $('sr-closed-leave').addEventListener('click', sessLeave);

    // Auth
    $('login-btn').addEventListener('click', () => { show('auth-modal', true); renderLoginMemory(); });
    $('auth-close').addEventListener('click', () => show('auth-modal', false));
    $('auth-modal').addEventListener('click', e => { if (e.target === $('auth-modal')) show('auth-modal', false); });
    bindTabs('auth-nav', { 'auth-login-form': 'auth-login-form', 'auth-reg-form': 'auth-reg-form' });
    $('auth-login-form').addEventListener('submit', loginSubmit);
    $('auth-reg-form').addEventListener('submit', registerSubmit);
    $('logout-btn').addEventListener('click', logout);
    const memoryList = $('login-memory-list');
    if (memoryList) {
        memoryList.addEventListener('click', e => {
            const btn = e.target.closest('[data-memory-user]');
            if (!btn) return;
            quickLoginFromMemory(btn.dataset.memoryUser);
        });
    }
    renderLoginMemory();

    // Space
    document.querySelectorAll('#sp-expire-bar .type-chip').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#sp-expire-bar .type-chip').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
        });
    });
    $('sp-clear-btn').addEventListener('click', spaceClearAll);
    $('sp-attach-file-btn').addEventListener('click', () => $('sp-file-in').click());
    $('sp-attach-img-btn').addEventListener('click', () => $('sp-img-in').click());
    $('sp-file-in').addEventListener('change', e => { spAttach = spAttach.concat(Array.from(e.target.files)); e.target.value = ''; renderSpAttach(); });
    $('sp-img-in').addEventListener('change', e => { spAttach = spAttach.concat(Array.from(e.target.files)); e.target.value = ''; renderSpAttach(); });
    $('sp-send-btn').addEventListener('click', spaceAdd);
    bindClipboardFiles('sp-text', files => { spAttach = spAttach.concat(files); renderSpAttach(); });

    // Friends
    // Friends — new 3-tab structure
    bindTabs('friend-nav', { 'f-myfriends': 'f-myfriends', 'f-sessions': 'f-sessions', 'f-history': 'f-history' });
    document.querySelectorAll('#friend-nav .pill').forEach(btn => {
        btn.addEventListener('click', () => {
            if (btn.dataset.tab === 'f-sessions') { renderCreateSessionFriends(); refreshPendingInvitations(); refreshActiveSessions(); }
            if (btn.dataset.tab === 'f-history') refreshFriendSessionHistory();
        });
    });
    $('f-add-btn').addEventListener('click', addFriend);
    $('f-unified-input').addEventListener('keydown', e => { if (e.key === 'Enter') addFriend(); });
    $('f-search-btn').addEventListener('click', renderFriendList);
    // Pending requests toggle
    $('f-pending-toggle').addEventListener('click', () => {
        const content = $('f-pending-content');
        const arrow = $('f-pending-arrow');
        content.classList.toggle('hidden');
        arrow.textContent = content.classList.contains('hidden') ? '\u25B6' : '\u25BC';
    });
    // Friend expire modal
    document.querySelectorAll('#f-expire-bar .type-chip').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#f-expire-bar .type-chip').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
        });
    });
    $('f-expire-close').addEventListener('click', () => show('f-expire-modal', false));
    $('f-expire-confirm').addEventListener('click', confirmFriendSession);
    // Friend session config modal (from My Friends Chat)
    document.querySelectorAll('#f-config-expire-bar .type-chip').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#f-config-expire-bar .type-chip').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
        });
    });
    $('f-config-search').addEventListener('input', renderConfigFriendsList);
    $('f-config-confirm').addEventListener('click', confirmSessionConfig);
    // Create session from Sessions tab
    document.querySelectorAll('#f-create-expire-bar .type-chip').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#f-create-expire-bar .type-chip').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
        });
    });
    $('f-create-session-btn').addEventListener('click', createSessionFromTab);
    $('f-create-search').addEventListener('input', renderCreateSessionFriends);
    // Friend session
    $('f-session-back').addEventListener('click', leaveFriendSessionView);
    $('f-history-back').addEventListener('click', leaveHistoryDetailView);
    $('f-session-close-btn').addEventListener('click', closeFriendSession);
    $('f-session-leave-btn').addEventListener('click', leaveCurrentSession);
    $('f-session-members-toggle').addEventListener('click', () => {
        $('f-session-members').classList.toggle('hidden');
        if (!$('f-session-members').classList.contains('hidden')) refreshSessionMembers();
    });
    $('f-session-invite-btn').addEventListener('click', openSessionInviteModal);
    $('f-session-invite-send').addEventListener('click', sendSessionInviteFromModal);
    $('f-session-invite-cancel').addEventListener('click', () => show('f-session-invite-modal', false));
    $('f-session-send-btn').addEventListener('click', sendFriendSessionItem);
    $('f-admin-leave-transfer').addEventListener('click', adminLeaveTransfer);
    $('f-admin-leave-dissolve').addEventListener('click', adminLeaveDissolve);
    $('f-session-start-btn').addEventListener('click', activateCurrentSession);
    $('f-session-attach-file').addEventListener('click', () => $('f-session-file-in').click());
    $('f-session-attach-img').addEventListener('click', () => $('f-session-img-in').click());
    $('f-session-file-in').addEventListener('change', e => { fSessionAttach = fSessionAttach.concat(Array.from(e.target.files)); e.target.value = ''; renderFSessionAttach(); });
    $('f-session-img-in').addEventListener('change', e => { fSessionAttach = fSessionAttach.concat(Array.from(e.target.files)); e.target.value = ''; renderFSessionAttach(); });
    bindClipboardFiles('f-session-text', files => { fSessionAttach = fSessionAttach.concat(files); renderFSessionAttach(); });
    // Enter key sends message (Shift+Enter for newline)
    $('f-session-text').addEventListener('keydown', e => {
        if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendFriendSessionItem(); }
    });

    // Notification drawer
    $('notif-bell').addEventListener('click', () => {
        $('notif-backdrop').classList.remove('hidden');
        $('notif-drawer').classList.remove('hidden');
        setTimeout(() => $('notif-drawer').classList.add('open'), 10);
    });
    $('notif-backdrop').addEventListener('click', closeNotifDrawer);
    $('notif-clear').addEventListener('click', clearAllNotifications);

    // Restore session
    restoreAuth();
    restoreFromHash();
});

/* ==================== Helpers ==================== */
function $(id) { return document.getElementById(id); }
function show(id, visible) { $(id).classList.toggle('hidden', !visible); }
function esc(s) { if (!s) return ''; const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
function fmtSize(b) { if (!b) return '0 B'; const k = 1024, u = ['B','KB','MB','GB'], i = Math.floor(Math.log(b)/Math.log(k)); return (b/Math.pow(k,i)).toFixed(1)+' '+u[i]; }
function fmtTime(iso) { if (!iso) return ''; const d = new Date(iso); return [d.getHours(),d.getMinutes(),d.getSeconds()].map(n=>String(n).padStart(2,'0')).join(':'); }
function fmtTimer(sec) { return Math.floor(sec/60) + ':' + String(sec%60).padStart(2,'0'); }

function saveHash() {
    let hash = currentMainTab || 'quick';
    if (currentSubTab) hash += '/' + currentSubTab;
    location.hash = hash;
}

function restoreFromHash() {
    const hash = location.hash.replace(/^#/, '');
    if (!hash) return;
    const parts = hash.split('/');
    const mainTab = parts[0];
    const subTab = parts[1] || null;
    const validMain = ['quick', 'session', 'xhs', 'space', 'friend'];
    if (!validMain.includes(mainTab)) return;
    // For space/friend, only restore if logged in
    if ((mainTab === 'space' || mainTab === 'friend') && !getToken()) return;
    // Click the main tab pill
    const mainPill = document.querySelector('#mode-nav .pill[data-mode="' + mainTab + '"]');
    if (mainPill) mainPill.click();
    // Click the sub-tab pill if specified
    if (subTab) {
        const navMap = { quick: 'quick-nav', session: 'sess-nav', friend: 'friend-nav' };
        const navId = navMap[mainTab];
        if (navId) {
            const subPill = document.querySelector('#' + navId + ' .pill[data-tab="' + subTab + '"]');
            if (subPill) subPill.click();
        }
    }
}

function showToast(msg, type = 'normal') {
    const t = $('toast');
    t.textContent = msg;
    t.classList.remove('error', 'success', 'normal');
    if (type === 'error') t.classList.add('error');
    else if (type === 'success') t.classList.add('success');
    else t.classList.add('normal');
    t.classList.add('show');
    setTimeout(() => t.classList.remove('show'), 3000);
}

function copyText(text) { navigator.clipboard.writeText(text).then(() => showToast('Copied!')); }

function dl(url, name) {
    const a = document.createElement('a'); a.href = url; a.download = name || '';
    document.body.appendChild(a); a.click(); document.body.removeChild(a);
}

function bindTabs(navId, map) {
    const keys = Object.keys(map);
    document.querySelectorAll('#' + navId + ' .pill').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#' + navId + ' .pill').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            keys.forEach(k => show(map[k], btn.dataset.tab === k));
            currentSubTab = btn.dataset.tab;
            saveHash();
        });
    });
}

function setupDZ(zoneId, inputId, handler) {
    const z = $(zoneId); if (!z) return;
    z.addEventListener('click', e => { if (!e.target.closest('button')) $(inputId).click(); });
    z.addEventListener('dragover', e => { e.preventDefault(); z.classList.add('drag-over'); });
    z.addEventListener('dragleave', () => z.classList.remove('drag-over'));
    z.addEventListener('drop', e => { e.preventDefault(); z.classList.remove('drag-over'); if (e.dataTransfer.files.length) handler(Array.from(e.dataTransfer.files)); });
}

function bindClipboardFiles(inputId, onFiles) {
    const input = $(inputId);
    if (!input) return;
    input.addEventListener('paste', e => {
        const items = Array.from((e.clipboardData && e.clipboardData.items) || []);
        const files = items
            .filter(item => item.kind === 'file')
            .map(item => item.getAsFile())
            .filter(Boolean);
        if (!files.length) return;
        e.preventDefault();
        onFiles(files);
        showToast(files.length > 1 ? files.length + ' files pasted' : 'File pasted', 'success');
    });
}

function startTimer(elId, total, onExpire) {
    let rem = total;
    const tick = () => {
        $(elId).textContent = fmtTimer(Math.max(rem, 0));
        if (rem <= 0) { clearInterval(sessTimerId); sessTimerId = null; if (onExpire) onExpire(); }
        rem--;
    };
    tick();
    if (sessTimerId) clearInterval(sessTimerId);
    sessTimerId = setInterval(tick, 1000);
}

/* ==================== Quick Send ==================== */
function resetQuickUpload() {
    $('img-preview').innerHTML = ''; show('img-preview', false); show('img-ph', true);
    $('file-list').innerHTML = ''; show('file-list', false); show('file-ph', true);
    show('q-result', false);
}

function addQFiles(files, isImage) {
    qFiles = qFiles.concat(files);
    if (isImage) renderImgPreview();
    else renderFileList();
}

function renderImgPreview() {
    const c = $('img-preview'); c.innerHTML = ''; show('img-preview', true); show('img-ph', false);
    qFiles.forEach((f, i) => {
        const w = document.createElement('div'); w.className = 'thumb-wrap';
        const img = document.createElement('img');
        const btn = document.createElement('button'); btn.className = 'remove-btn'; btn.textContent = '\u00d7';
        btn.addEventListener('click', e => { e.stopPropagation(); qFiles.splice(i, 1); renderImgPreview(); if (!qFiles.length) { show('img-preview', false); show('img-ph', true); } });
        const r = new FileReader(); r.onload = e => img.src = e.target.result; r.readAsDataURL(f);
        w.append(img, btn); c.appendChild(w);
    });
}

function renderFileList() {
    const c = $('file-list'); c.innerHTML = ''; show('file-list', qFiles.length > 0); show('file-ph', !qFiles.length);
    qFiles.forEach((f, i) => {
        const d = document.createElement('div'); d.className = 'file-item';
        d.innerHTML = `<span class="name">${esc(f.name)}</span><span class="size">${fmtSize(f.size)}</span>`;
        const btn = document.createElement('button'); btn.className = 'remove-btn'; btn.textContent = '\u00d7';
        btn.addEventListener('click', () => { qFiles.splice(i, 1); renderFileList(); });
        d.appendChild(btn); c.appendChild(d);
    });
}

async function quickSend() {
    const btn = $('q-send-btn'); btn.disabled = true; btn.textContent = 'Sending...';
    try {
        let resp;
        const del = $('del-after').checked;
        if (qType === 'text') {
            const c = $('q-text').value.trim();
            if (!c) { showToast('Enter text'); return; }
            resp = await fetch('/api/share/text', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({content:c, deleteAfterDownload:del}) });
        } else {
            if (!qFiles.length) { showToast('Select files'); return; }
            const fd = new FormData(); qFiles.forEach(f => fd.append('file', f));
            fd.append('type', qType.toUpperCase()); fd.append('deleteAfterDownload', del);
            resp = await fetch('/api/share/file', { method:'POST', body:fd });
        }
        if (!resp.ok) throw new Error((await resp.json().catch(()=>({}))).message || 'Failed');
        const data = await resp.json();
        $('q-code').textContent = data.code;
        show('q-result', true);
        showToast('Sent!', 'success');
    } catch(e) { showToast(e.message); }
    finally { btn.disabled = false; btn.textContent = 'Send'; }
}

/* ==================== Quick Receive ==================== */
async function quickRecv() {
    const code = $('qr-code').value.trim().toUpperCase();
    if (!code) { showToast('Enter a code'); return; }
    show('qr-result', false); show('qr-error', false);
    $('qr-result').innerHTML = '';
    try {
        const resp = await fetch('/api/share/' + code);
        if (!resp.ok) throw new Error((await resp.json().catch(()=>({}))).message || 'Not found');
        const data = await resp.json();
        show('qr-result', true);
        const el = $('qr-result');

        if (data.type === 'TEXT') {
            el.innerHTML = `<div class="card"><div class="timeline-header"><span class="badge text-type">TEXT</span><button class="btn-link" id="qr-copy">Copy</button></div><pre class="text-bubble">${esc(data.content)}</pre></div>`;
            $('qr-copy').addEventListener('click', () => copyText(data.content));
        } else if (data.type === 'IMAGE') {
            let h = '<div class="card"><span class="badge image-type">IMAGES</span><div class="img-grid" style="margin-top:12px">';
            data.files.forEach(f => { h += `<div class="img-card"><img src="/api/share/${code}/preview/${f.index}"><div class="img-footer"><span>${esc(f.fileName)}</span><button class="btn-link sm" data-url="/api/share/${code}/download/${f.index}" data-name="${esc(f.fileName)}">Download</button></div></div>`; });
            h += '</div></div>'; el.innerHTML = h;
            el.querySelectorAll('[data-url]').forEach(b => b.addEventListener('click', () => dl(b.dataset.url, b.dataset.name)));
        } else {
            let h = '<div class="card"><span class="badge file-type">FILES</span><div style="margin-top:12px">';
            data.files.forEach(f => { h += `<div class="dl-row"><div class="dl-info"><div class="dl-name">${esc(f.fileName)}</div><div class="dl-size">${fmtSize(f.fileSize)}</div></div><button class="btn btn-primary btn-sm" data-url="/api/share/${code}/download/${f.index}" data-name="${esc(f.fileName)}">Download</button></div>`; });
            h += '</div></div>'; el.innerHTML = h;
            el.querySelectorAll('[data-url]').forEach(b => b.addEventListener('click', () => dl(b.dataset.url, b.dataset.name)));
        }
    } catch(e) {
        show('qr-error', true);
        $('qr-error').innerHTML = `<p>${esc(e.message)}</p>`;
    }
}

/* ==================== Session Send ==================== */
async function sessCreate() {
    try {
        const activeChip = document.querySelector('#expire-bar .type-chip.active');
        const expireSeconds = activeChip ? parseInt(activeChip.dataset.expire) : 1800;
        const resp = await fetch('/api/session', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ expireSeconds }) });
        if (!resp.ok) throw new Error('Failed');
        const data = await resp.json();
        sessCode = data.code;
        $('s-code').textContent = data.code;
        show('s-create', false); show('s-active', true);
        startTimer('s-timer', data.expireSeconds, sessClose);
        showToast('Session: ' + data.code);
    } catch(e) { showToast(e.message); }
}

function renderSessAttach() {
    const bar = $('s-attachments');
    bar.innerHTML = '';
    show('s-attachments', sessAttach.length > 0);
    sessAttach.forEach((f, i) => {
        const tag = document.createElement('span'); tag.className = 'attach-tag';
        tag.innerHTML = `<span class="tag-name">${esc(f.name)}</span>`;
        const btn = document.createElement('button'); btn.className = 'tag-remove'; btn.textContent = '\u00d7';
        btn.addEventListener('click', () => { sessAttach.splice(i, 1); renderSessAttach(); });
        tag.appendChild(btn); bar.appendChild(tag);
    });
}

async function sessSend() {
    const text = $('s-text').value.trim();
    const hasFiles = sessAttach.length > 0;
    if (!text && !hasFiles) { showToast('Enter text or attach files'); return; }
    const btn = $('s-send-btn'); btn.disabled = true;
    try {
        if (text) {
            const r = await fetch('/api/session/'+sessCode+'/items/text', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({content:text}) });
            if (!r.ok) throw new Error((await r.json().catch(()=>({}))).message || 'Failed');
            $('s-text').value = '';
        }
        if (hasFiles) {
            const fd = new FormData(); sessAttach.forEach(f => fd.append('file', f));
            const r = await fetch('/api/session/'+sessCode+'/items/file', { method:'POST', body:fd });
            if (!r.ok) throw new Error((await r.json().catch(()=>({}))).message || 'Failed');
            sessAttach = []; renderSessAttach();
        }
        showToast('Sent');
        refreshSessItems();
    } catch(e) { showToast(e.message); }
    finally { btn.disabled = false; }
}

async function refreshSessItems() {
    if (!sessCode) return;
    try {
        const r = await fetch('/api/session/'+sessCode);
        if (!r.ok) return;
        const d = await r.json();
        renderTimeline(d.items, sessCode, 's-items');
    } catch(e) {}
}

async function sessClose() {
    if (!sessCode) return;
    try { await fetch('/api/session/'+sessCode, {method:'DELETE'}); } catch(e) {}
    if (sessTimerId) { clearInterval(sessTimerId); sessTimerId = null; }
    sessCode = null;
    show('s-active', false); show('s-create', true);
    $('s-items').innerHTML = '';
    showToast('Session closed');
}

/* ==================== Session Receive ==================== */
async function sessJoin() {
    const code = $('sr-code').value.trim().toUpperCase();
    if (!code) { showToast('Enter a code'); return; }
    show('sr-error', false);
    try {
        const r = await fetch('/api/session/'+code);
        if (!r.ok) throw new Error('Session not found');
        const d = await r.json();
        if (d.status === 'CLOSED') throw new Error('Session already closed');
        recvCode = code;
        show('sr-join', false); show('sr-active', true); show('sr-closed', false);
        updateRecv(d);
        recvPollId = setInterval(async () => {
            try {
                const r2 = await fetch('/api/session/'+recvCode);
                if (!r2.ok) { onRecvClosed(); return; }
                const d2 = await r2.json();
                if (d2.status === 'CLOSED') { onRecvClosed(); return; }
                updateRecv(d2);
            } catch(e) { onRecvClosed(); }
        }, 2000);
    } catch(e) {
        show('sr-error', true);
        $('sr-error').innerHTML = `<p>${esc(e.message)}</p>`;
    }
}

function updateRecv(data) {
    $('sr-status').textContent = data.status;
    $('sr-count').textContent = data.itemCount + ' items';
    $('sr-timer').textContent = fmtTimer(data.remainingSeconds);
    renderTimeline(data.items, data.code, 'sr-items');
}

function onRecvClosed() {
    stopRecvPoll();
    show('sr-closed', true);
    $('sr-status').textContent = 'CLOSED';
    $('sr-status').className = 'badge red';
    $('sr-items').innerHTML = '<p class="muted" style="text-align:center;padding:20px">Content cleared.</p>';
}

function sessLeave() {
    stopRecvPoll();
    recvCode = null;
    show('sr-active', false); show('sr-join', true); show('sr-closed', false);
    $('sr-items').innerHTML = '';
    $('sr-status').className = 'badge green';
}

function stopRecvPoll() {
    if (recvPollId) { clearInterval(recvPollId); recvPollId = null; }
}

async function xhsExtractImages() {
    const input = $('xhs-url').value.trim();
    if (!input) {
        showToast('Please paste the rednote link first', 'error');
        return;
    }
    const btn = $('xhs-extract-btn');
    btn.disabled = true;
    btn.textContent = 'Extracting...';
    show('xhs-error', false);
    show('xhs-result', false);
    show('xhs-actions', false);
    try {
        const resp = await fetch('/api/xhs/extract-images', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ url: input })
        });
        const data = await resp.json().catch(() => ({}));
        if (!resp.ok) throw new Error(data.message || 'Extraction failed');

        // 支持图片和视频
        xhsImages = Array.isArray(data.images) ? data.images : [];
        xhsVideos = Array.isArray(data.videos) ? data.videos : [];

        if (!xhsImages.length && !xhsVideos.length) {
            throw new Error('No media extracted');
        }

        renderXhsResult(data.title || 'rednote post', data.sourceUrl || input, xhsImages, xhsVideos);
        show('xhs-actions', true);
        show('xhs-result', true);

        // 动态生成提示信息
        const parts = [];
        if (xhsImages.length) parts.push(`${xhsImages.length} image${xhsImages.length > 1 ? 's' : ''}`);
        if (xhsVideos.length) parts.push(`${xhsVideos.length} Live Photo${xhsVideos.length > 1 ? 's' : ''}`);
        showToast(parts.join(' and ') + ' extracted', 'success');
    } catch (e) {
        show('xhs-error', true);
        $('xhs-error').innerHTML = `<p>${esc(e.message || 'Extraction failed')}</p>`;
    } finally {
        btn.disabled = false;
        btn.textContent = 'Extract';
    }
}

function renderXhsResult(title, sourceUrl, images, videos) {
    const result = $('xhs-result');
    let html = '<div class="xhs-result-head">';
    html += `<p><strong>${esc(title)}</strong></p>`;
    html += `<p class="muted" style="margin-top:4px;word-break:break-all">${esc(sourceUrl)}</p>`;
    html += '</div>';
    html += '<div class="xhs-grid">';

    // 渲染图片
    if (images && images.length) {
        images.forEach((url, idx) => {
            html += '<div class="xhs-item">';
            html += `<label class="xhs-check-row"><input type="checkbox" class="xhs-item-check" data-url="${esc(url)}" data-type="image" checked><span class="xhs-index">Image #${idx + 1}</span></label>`;
            html += `<a class="xhs-img-wrap" href="${esc(url)}" target="_blank" rel="noopener noreferrer">`;
            html += `<img src="${esc(url)}" alt="xhs-${idx + 1}" onerror="this.style.display='none';this.nextElementSibling.style.display='flex'" style="width:100%;height:100%;object-fit:cover">`;
            html += `<div style="display:none;width:100%;height:100%;align-items:center;justify-content:center;background:linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);flex-direction:column;padding:30px;box-sizing:border-box">`;
            html += `<div style="font-size:48px;margin-bottom:16px;opacity:0.6">📷</div>`;
            html += `<div style="color:#5a6c7d;font-size:14px;font-weight:500;text-align:center;line-height:1.6">The image format does not support preview for the time being</div>`;
            html += `<div style="color:#8b95a1;font-size:12px;margin-top:12px;padding:8px 16px;background:rgba(255,255,255,0.7);border-radius:4px">Click Download to save</div>`;
            html += `</div>`;
            html += `</a>`;
            html += `<button class="btn-link sm" data-xhs-dl="${esc(url)}" data-name="xhs-image-${idx + 1}.jpg" data-type="image">Download</button>`;
            html += '</div>';
        });
    }

    // 渲染视频（Live Photo）
    if (videos && videos.length) {
        videos.forEach((url, idx) => {
            html += '<div class="xhs-item">';
            html += `<label class="xhs-check-row"><input type="checkbox" class="xhs-item-check" data-url="${esc(url)}" data-type="video" checked><span class="xhs-index">Live Photo #${idx + 1}</span></label>`;
            html += `<div class="xhs-img-wrap" style="position:relative">`;
            // 先尝试直接加载视频
            html += `<video controls preload="metadata" style="width:100%;height:100%;object-fit:cover" onerror="this.style.display='none';this.nextElementSibling.style.display='flex'">`;
            html += `<source src="${esc(url)}" type="video/mp4">`;
            html += `</video>`;
            // 如果视频加载失败，显示占位符
            html += `<div style="display:none;width:100%;height:100%;align-items:center;justify-content:center;background:linear-gradient(135deg, #667eea 0%, #764ba2 100%);flex-direction:column;padding:30px;box-sizing:border-box;position:absolute;top:0;left:0">`;
            html += `<div style="font-size:48px;margin-bottom:16px;opacity:0.9">🎬</div>`;
            html += `<div style="color:#fff;font-size:14px;font-weight:500;text-align:center;line-height:1.6">Video preview unavailable</div>`;
            html += `<div style="color:rgba(255,255,255,0.8);font-size:12px;margin-top:12px;padding:8px 16px;background:rgba(255,255,255,0.2);border-radius:4px">Click Download to save</div>`;
            html += `</div>`;
            html += `</div>`;
            html += `<button class="btn-link sm" data-xhs-dl="${esc(url)}" data-name="xhs-livephoto-${idx + 1}.mp4" data-type="video">Download</button>`;
            html += '</div>';
        });
    }

    html += '</div>';
    result.innerHTML = html;
    result.querySelectorAll('[data-xhs-dl]').forEach(btn => {
        btn.addEventListener('click', () => xhsDownloadFile(btn.dataset.xhsDl, btn.dataset.name));
    });
}

function xhsCopyLinks() {
    const allUrls = [...xhsImages, ...(xhsVideos || [])];
    if (!allUrls.length) {
        showToast('There are currently no replicable links', 'error');
        return;
    }
    copyText(allUrls.join('\n'));
}

function xhsSelectAll() {
    const checks = document.querySelectorAll('.xhs-item-check');
    if (!checks.length) return;
    checks.forEach(c => { c.checked = true; });
    showToast('All selected');
}

function xhsDownloadSelected() {
    const selected = Array.from(document.querySelectorAll('.xhs-item-check:checked')).map(c => ({
        url: c.dataset.url,
        type: c.dataset.type
    }));
    if (!selected.length) {
        showToast('Please select media first', 'error');
        return;
    }
    selected.forEach((item, idx) => {
        const ext = item.type === 'video' ? 'mp4' : 'jpg';
        const name = `xhs-${item.type}-${idx + 1}.${ext}`;
        setTimeout(() => xhsDownloadFile(item.url, name), idx * 200);
    });
}

// 通过后端代理下载文件（解决跨域问题）
async function xhsDownloadFile(url, filename) {
    try {
        showToast('Downloading...', 'normal');
        const resp = await fetch('/api/xhs/proxy-download?url=' + encodeURIComponent(url));
        if (!resp.ok) throw new Error('Download failed');

        const blob = await resp.blob();
        const blobUrl = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = blobUrl;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(blobUrl);

        showToast('Downloaded: ' + filename, 'success');
    } catch (e) {
        showToast('Download failed: ' + e.message, 'error');
    }
}

/* ==================== Timeline Renderer ==================== */
function renderTimeline(items, code, containerId) {
    const el = $(containerId);
    if (!items || !items.length) { el.innerHTML = '<p class="muted" style="text-align:center;padding:24px">No items yet</p>'; el.dataset.n = '0'; return; }
    if (el.dataset.n === String(items.length)) return; // skip re-render if unchanged
    el.dataset.n = String(items.length);
    el.innerHTML = '';

    items.forEach(item => {
        const card = document.createElement('div'); card.className = 'timeline-card';
        const typeClass = item.type === 'TEXT' ? 'text-type' : item.type === 'IMAGE' ? 'image-type' : 'file-type';
        let html = `<div class="timeline-header"><span class="badge ${typeClass}">${item.type}</span><span class="time-label">${fmtTime(item.addedAt)}</span></div>`;

        if (item.type === 'TEXT') {
            html += `<pre class="text-bubble">${esc(item.content)}</pre><div style="margin-top:8px"><button class="btn-link" data-copy="${esc(item.content)}">Copy</button></div>`;
        } else if (item.type === 'IMAGE') {
            html += '<div class="img-grid">';
            (item.files||[]).forEach(f => {
                html += `<div class="img-card"><img src="/api/session/${code}/items/${item.id}/preview/${f.index}"><div class="img-footer"><span>${esc(f.fileName)}</span><button class="btn-link sm" data-url="/api/session/${code}/items/${item.id}/download/${f.index}" data-name="${esc(f.fileName)}">Download</button></div></div>`;
            });
            html += '</div>';
        } else {
            (item.files||[]).forEach(f => {
                html += `<div class="dl-row"><div class="dl-info"><div class="dl-name">${esc(f.fileName)}</div><div class="dl-size">${fmtSize(f.fileSize)}</div></div><button class="btn btn-primary btn-sm" data-url="/api/session/${code}/items/${item.id}/download/${f.index}" data-name="${esc(f.fileName)}">Download</button></div>`;
            });
        }
        card.innerHTML = html;
        // Bind events
        card.querySelectorAll('[data-copy]').forEach(b => b.addEventListener('click', () => copyText(b.dataset.copy)));
        card.querySelectorAll('[data-url]').forEach(b => b.addEventListener('click', () => dl(b.dataset.url, b.dataset.name)));
        el.appendChild(card);
    });
    el.scrollTop = el.scrollHeight;
}

/* ==================== Auth ==================== */
function getToken() { return localStorage.getItem('bf_token'); }
function getUser() { return localStorage.getItem('bf_user'); }
function getLoginMemory() {
    try {
        const raw = localStorage.getItem(LOGIN_MEMORY_KEY);
        const parsed = raw ? JSON.parse(raw) : [];
        if (!Array.isArray(parsed)) return [];
        return parsed.filter(it => it && typeof it.username === 'string' && typeof it.token === 'string');
    } catch (e) {
        return [];
    }
}

function saveLoginMemory(list) {
    localStorage.setItem(LOGIN_MEMORY_KEY, JSON.stringify(list.slice(0, 8)));
}

function rememberLogin(username, token) {
    if (!username || !token) return;
    const list = getLoginMemory().filter(it => it.username !== username);
    list.unshift({ username, token, updatedAt: Date.now() });
    saveLoginMemory(list);
}

function removeRememberedLogin(username) {
    const list = getLoginMemory().filter(it => it.username !== username);
    saveLoginMemory(list);
}

function renderLoginMemory() {
    const wrap = $('login-memory-wrap');
    const listEl = $('login-memory-list');
    if (!wrap || !listEl) return;
    const list = getLoginMemory();
    if (!list.length) {
        show('login-memory-wrap', false);
        listEl.innerHTML = '';
        return;
    }
    show('login-memory-wrap', true);
    listEl.innerHTML = '';
    list.forEach(it => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'login-memory-btn';
        btn.dataset.memoryUser = it.username;
        btn.textContent = it.username;
        listEl.appendChild(btn);
    });
}

function authHeader() {
    const t = getToken();
    return t ? { 'Authorization': 'Bearer ' + t } : {};
}

async function restoreAuth() {
    if (getToken() && getUser()) {
        try {
            const r = await fetch('/api/auth/me', { headers: authHeader() });
            if (!r.ok) throw new Error('expired');
            onLogin(getUser());
        } catch (e) {
            localStorage.removeItem('bf_token');
            localStorage.removeItem('bf_user');
            localStorage.removeItem('bf_uid');
        }
    }
    renderLoginMemory();
}

function onLogin(username) {
    show('user-area', false);
    show('user-info', true);
    $('user-display').textContent = username;
    show('nav-space', true);
    show('nav-friend', true);
    show('notif-bell', true);
    connectFriendWS();
    // Fetch userId for friend session sender identification
    fetch('/api/auth/me', { headers: authHeader() }).then(r => r.ok ? r.json() : null).then(d => {
        if (d && d.id) localStorage.setItem('bf_uid', d.id);
    }).catch(() => {});
    rememberLogin(username, getToken());
    renderLoginMemory();
}

function logout() {
    localStorage.removeItem('bf_token');
    localStorage.removeItem('bf_user');
    localStorage.removeItem('bf_uid');
    show('user-area', true);
    show('user-info', false);
    show('nav-space', false);
    show('nav-friend', false);
    show('notif-bell', false);
    stopSpacePoll();
    disconnectSpaceWS();
    disconnectFriendWS();
    notifications = [];
    renderNotifications();
    // If on space/friend tab, switch to quick
    if (!$('view-space').classList.contains('hidden') || !$('view-friend').classList.contains('hidden')) {
        document.querySelector('#mode-nav .pill[data-mode="quick"]').click();
    }
    showToast('Logged out');
}

async function quickLoginFromMemory(username) {
    const item = getLoginMemory().find(it => it.username === username);
    if (!item) return;
    show('login-error', false);
    localStorage.setItem('bf_token', item.token);
    localStorage.setItem('bf_user', item.username);
    try {
        const r = await fetch('/api/auth/me', { headers: authHeader() });
        if (!r.ok) throw new Error('Session expired');
        onLogin(item.username);
        show('auth-modal', false);
        showToast('Welcome back, ' + item.username, 'success');
    } catch (e) {
        localStorage.removeItem('bf_token');
        localStorage.removeItem('bf_user');
        localStorage.removeItem('bf_uid');
        removeRememberedLogin(item.username);
        renderLoginMemory();
        const loginPill = document.querySelector('#auth-nav .pill[data-tab="auth-login-form"]');
        if (loginPill) loginPill.click();
        show('auth-modal', true);
        $('login-user').value = item.username;
        $('login-pass').value = '';
        show('login-error', true);
        $('login-error').innerHTML = '<p>Saved login expired, please enter password again.</p>';
        showToast('Saved login expired, please sign in again', 'error');
    }
}

async function loginSubmit(e) {
    e.preventDefault();
    show('login-error', false);
    const user = $('login-user').value.trim();
    const pass = $('login-pass').value;
    try {
        const r = await fetch('/api/auth/login', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: user, password: pass })
        });
        if (!r.ok) throw new Error((await r.json().catch(() => ({}))).message || 'Login failed');
        const d = await r.json();
        localStorage.setItem('bf_token', d.token);
        localStorage.setItem('bf_user', d.username);
        rememberLogin(d.username, d.token);
        onLogin(d.username);
        show('auth-modal', false);
        showToast('Welcome, ' + d.username);
    } catch (err) {
        show('login-error', true);
        $('login-error').innerHTML = '<p>' + esc(err.message) + '</p>';
    }
}

async function registerSubmit(e) {
    e.preventDefault();
    show('reg-error', false); show('reg-ok', false);
    const user = $('reg-user').value.trim();
    const pass = $('reg-pass').value;
    try {
        const r = await fetch('/api/auth/register', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: user, password: pass })
        });
        if (!r.ok) throw new Error((await r.json().catch(() => ({}))).message || 'Registration failed');
        show('reg-ok', true);
        $('reg-ok').innerHTML = '<p>Registered! You can now login.</p>';
        const loginPill = document.querySelector('#auth-nav .pill[data-tab="auth-login-form"]');
        if (loginPill) loginPill.click();
        $('login-user').value = user;
        $('login-pass').value = '';
        $('reg-user').value = '';
        $('reg-pass').value = '';
    } catch (err) {
        show('reg-error', true);
        $('reg-error').innerHTML = '<p>' + esc(err.message) + '</p>';
    }
}

/* ==================== Space ==================== */
function renderSpAttach() {
    const bar = $('sp-attachments');
    bar.innerHTML = '';
    show('sp-attachments', spAttach.length > 0);
    spAttach.forEach((f, i) => {
        const tag = document.createElement('span'); tag.className = 'attach-tag';
        tag.innerHTML = `<span class="tag-name">${esc(f.name)}</span>`;
        const btn = document.createElement('button'); btn.className = 'tag-remove'; btn.textContent = '\u00d7';
        btn.addEventListener('click', () => { spAttach.splice(i, 1); renderSpAttach(); });
        tag.appendChild(btn); bar.appendChild(tag);
    });
}

async function spaceAdd() {
    const text = $('sp-text').value.trim();
    const hasFiles = spAttach.length > 0;
    if (!text && !hasFiles) { showToast('Enter text or attach files'); return; }
    const activeChip = document.querySelector('#sp-expire-bar .type-chip.active');
    const expireSeconds = activeChip ? parseInt(activeChip.dataset.expire) : 1800;
    const btn = $('sp-send-btn'); btn.disabled = true;
    try {
        if (text) {
            const r = await fetch('/api/space/items/text', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', ...authHeader() },
                body: JSON.stringify({ content: text, expireSeconds })
            });
            if (r.status === 401) { onAuthFail(); return; }
            if (!r.ok) throw new Error('Failed');
            $('sp-text').value = '';
        }
        if (hasFiles) {
            const fd = new FormData();
            spAttach.forEach(f => fd.append('file', f));
            fd.append('expireSeconds', expireSeconds);
            const r = await fetch('/api/space/items/file', {
                method: 'POST', headers: authHeader(), body: fd
            });
            if (r.status === 401) { onAuthFail(); return; }
            if (!r.ok) throw new Error('Failed');
            spAttach = []; renderSpAttach();
        }
        showToast('Added');
        refreshSpace();
        connectSpaceWS();
    } catch (e) { showToast(e.message); }
    finally { btn.disabled = false; }
}

async function refreshSpace() {
    if (!getToken()) return;
    try {
        const r = await fetch('/api/space/items', { headers: authHeader() });
        if (r.status === 401) { onAuthFail(); return; }
        if (!r.ok) return;
        const items = await r.json();
        renderSpaceItems(items);
        // Connect WebSocket if there are items, disconnect if empty
        if (items.length > 0) connectSpaceWS();
    } catch (e) { /* ignore */ }
}

async function spaceClearAll() {
    if (!getToken()) return;
    try {
        const r = await fetch('/api/space/items/clear', { method: 'DELETE', headers: authHeader() });
        if (r.status === 401) { onAuthFail(); return; }
        $('sp-items').dataset.n = '0';
        $('sp-items').innerHTML = '<p class="muted" style="text-align:center;padding:24px">Your space is empty</p>';
        disconnectSpaceWS();
        showToast('Cleared');
    } catch (e) { showToast(e.message); }
}

function stopSpacePoll() {
    if (spPollId) { clearInterval(spPollId); spPollId = null; }
}

function renderSpaceItems(items) {
    const el = $('sp-items');
    if (!items || !items.length) {
        el.innerHTML = '<p class="muted" style="text-align:center;padding:24px">Your space is empty</p>';
        el.dataset.n = '0';
        return;
    }
    // Always re-render to update countdown timers
    el.dataset.n = String(items.length);
    el.innerHTML = '';

    items.forEach(item => {
        const card = document.createElement('div'); card.className = 'timeline-card';
        const typeClass = item.type === 'TEXT' ? 'text-type' : item.type === 'IMAGE' ? 'image-type' : 'file-type';

        // Build timer label
        let timerHtml = '';
        if (item.remainingSeconds != null) {
            timerHtml = `<span class="time-label sp-countdown" data-remaining="${item.remainingSeconds}">${fmtTimer(item.remainingSeconds)}</span>`;
        }

        let html = `<div class="timeline-header"><span class="badge ${typeClass}">${item.type}</span><div class="row gap-sm">${timerHtml}<span class="time-label">${fmtTime(item.createdAt)}</span><button class="timeline-delete" data-del="${item.id}">Delete</button></div></div>`;

        if (item.type === 'TEXT') {
            html += `<pre class="text-bubble">${esc(item.content)}</pre><div style="margin-top:8px"><button class="btn-link" data-copy="${esc(item.content)}">Copy</button></div>`;
        } else if (item.type === 'IMAGE') {
            html += '<div class="img-grid">';
            (item.files || []).forEach(f => {
                html += `<div class="img-card"><img src="/api/space/items/${item.id}/files/${f.id}/preview"><div class="img-footer"><span>${esc(f.fileName)}</span><button class="btn-link sm" data-url="/api/space/items/${item.id}/files/${f.id}/download" data-name="${esc(f.fileName)}">Download</button></div></div>`;
            });
            html += '</div>';
        } else {
            (item.files || []).forEach(f => {
                html += `<div class="dl-row"><div class="dl-info"><div class="dl-name">${esc(f.fileName)}</div><div class="dl-size">${fmtSize(f.fileSize)}</div></div><button class="btn btn-primary btn-sm" data-url="/api/space/items/${item.id}/files/${f.id}/download" data-name="${esc(f.fileName)}">Download</button></div>`;
            });
        }
        card.innerHTML = html;
        card.querySelectorAll('[data-copy]').forEach(b => b.addEventListener('click', () => copyText(b.dataset.copy)));
        card.querySelectorAll('[data-url]').forEach(b => b.addEventListener('click', () => {
            const a = document.createElement('a');
            a.href = b.dataset.url;
            a.href += (a.href.includes('?') ? '&' : '?') + 'token=' + getToken();
            a.download = b.dataset.name || '';
            document.body.appendChild(a); a.click(); document.body.removeChild(a);
        }));
        card.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', async () => {
            await fetch('/api/space/items/' + b.dataset.del, { method: 'DELETE', headers: authHeader() });
            el.dataset.n = '0';
            refreshSpace();
        }));
        el.appendChild(card);
    });

    startSpaceCountdown();
    el.scrollTop = el.scrollHeight;
}

let spCountdownId = null;
function startSpaceCountdown() {
    if (spCountdownId) clearInterval(spCountdownId);
    spCountdownId = setInterval(() => {
        const els = document.querySelectorAll('.sp-countdown');
        if (!els.length) { clearInterval(spCountdownId); spCountdownId = null; return; }
        let anyExpired = false;
        els.forEach(el => {
            let rem = parseInt(el.dataset.remaining) - 1;
            el.dataset.remaining = rem;
            if (rem <= 0) { anyExpired = true; el.textContent = '0:00'; }
            else el.textContent = fmtTimer(rem);
        });
        if (anyExpired) refreshSpace();
    }, 1000);
}

function onAuthFail() {
    logout();
    show('auth-modal', true);
    showToast('Session expired, please login again');
}

/* ==================== Space WebSocket ==================== */
function connectSpaceWS() {
    if (spWs && spWs.readyState <= 1) return; // already connected or connecting
    const token = getToken();
    if (!token) return;
    const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
    spWs = new WebSocket(proto + '//' + location.host + '/ws/space?token=' + token);
    spWs.onopen = () => {
        stopSpacePoll(); // WebSocket connected, stop polling
    };
    spWs.onmessage = (e) => {
        if (e.data === 'refresh') {
            $('sp-items').dataset.n = '0';
            refreshSpace();
        } else if (e.data === 'empty') {
            $('sp-items').dataset.n = '0';
            $('sp-items').innerHTML = '<p class="muted" style="text-align:center;padding:24px">Your space is empty</p>';
            disconnectSpaceWS();
        }
    };
    spWs.onclose = () => {
        spWs = null;
        // Fallback to polling if still on Space tab
        if (!$('view-space').classList.contains('hidden') && getToken()) {
            if (!spPollId) spPollId = setInterval(refreshSpace, 10000);
        }
    };
}

function disconnectSpaceWS() {
    if (spWs) { spWs.close(); spWs = null; }
}

/* ==================== Friend WebSocket ==================== */
function connectFriendWS() {
    if (friendWs && friendWs.readyState <= 1) return;
    const token = getToken();
    if (!token) return;
    const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
    friendWs = new WebSocket(proto + '//' + location.host + '/ws/friend?token=' + token);
    friendWs.onmessage = (e) => {
        try {
            const msg = JSON.parse(e.data);
            if (msg.type === 'friend_request') {
                showToast(msg.fromUsername + ' sent you a friend request');
                addNotification(msg.fromUsername + ' sent you a friend request', 'friend_request', { requestId: msg.requestId });
                refreshRequests();
            }
            else if (msg.type === 'friend_accepted') {
                showToast(msg.username + ' accepted your request', 'success');
                addNotification(msg.username + ' accepted your friend request', 'friend');
                refreshFriendList(); refreshRequests();
            }
            else if (msg.type === 'friend_removed') { refreshFriendList(); }
            else if (msg.type === 'session_invitation') {
                showToast(msg.fromUsername + ' invited you to a session');
                addNotification(msg.fromUsername + ' invited you to a session', 'session_invitation', { invitationId: msg.invitationId });
                refreshPendingInvitations();
            }
            else if (msg.type === 'invitation_accepted') {
                showToast(msg.username + ' accepted your invitation');
                addNotification(msg.username + ' joined your session', 'session');
                if (activeFSession === msg.sessionId) refreshFriendSessionItems();
            }
            else if (msg.type === 'invitation_declined') {
                showToast(msg.byUsername + ' declined your invitation');
                addNotification(msg.byUsername + ' declined your session invitation', 'session');
            }
            else if (msg.type === 'session_member_joined') {
                if (activeFSession === msg.sessionId) {
                    showToast(msg.username + ' joined the session');
                    refreshFriendSessionItems();
                }
            }
            else if (msg.type === 'session_member_left') {
                if (activeFSession === msg.sessionId) {
                    showToast(msg.username + ' left the session');
                    refreshFriendSessionItems();
                }
            }
            else if (msg.type === 'session_member_kicked') {
                if (activeFSession === msg.sessionId) {
                    showToast('You were kicked from the session by ' + msg.kickedBy);
                    leaveFriendSessionView();
                }
            }
            else if (msg.type === 'session_update') { if (activeFSession === msg.sessionId) refreshFriendSessionItems(); }
            else if (msg.type === 'session_closed') {
                // Mark session as closed
                closedSessions.add(msg.sessionId);
                if (activeFSession === msg.sessionId) {
                    showToast('Session has been closed by admin');
                    leaveFriendSessionView();
                }
            }
            else if (msg.type === 'admin_transferred') {
                showToast('You are now the admin (transferred by ' + msg.oldAdminUsername + ')');
                addNotification(msg.oldAdminUsername + ' transferred admin role to you', 'session');
                if (activeFSession === msg.sessionId) refreshFriendSessionItems();
            }
            else if (msg.type === 'online_status') { updateOnlineDot(msg.userId, msg.online); }
            else if (msg.type === 'session_replaced') {
                showToast('Connected from another device. This session is disconnected.');
                if (activeFSession) leaveFriendSessionView();
            }
        } catch(err) {}
    };
    friendWs.onclose = () => { friendWs = null; };
}

function disconnectFriendWS() {
    if (friendWs) { friendWs.close(); friendWs = null; }
}

function updateOnlineDot(userId, online) {
    document.querySelectorAll('.online-dot[data-uid="' + userId + '"]').forEach(dot => {
        dot.classList.toggle('online', online);
    });
}

/* ==================== Friend Management ==================== */
async function addFriend() {
    const input = $('f-unified-input');
    const username = input.value.trim();
    if (!username) return;
    try {
        const r = await fetch('/api/friend/request', { method: 'POST', headers: { 'Content-Type': 'application/json', ...authHeader() }, body: JSON.stringify({ username }) });
        if (r.status === 401) { onAuthFail(); return; }
        if (!r.ok) {
            const d = await r.json().catch(() => ({}));
            const errorMsg = d.message || d.error || 'Failed to send friend request';
            showToast(errorMsg, 'error');
            return;
        }
        input.value = '';
        showToast('Friend request sent', 'success');
        refreshRequests();
    } catch (e) { showToast(e.message, 'error'); }
}

let cachedFriends = [];

async function refreshFriendList() {
    if (!getToken()) return;
    try {
        const r = await fetch('/api/friend/list', { headers: authHeader() });
        if (r.status === 401) { onAuthFail(); return; }
        if (!r.ok) return;
        cachedFriends = await r.json();
        renderFriendList();
    } catch (e) {}
}

function renderFriendList() {
    const el = $('f-friends-list');
    const query = ($('f-unified-input') ? $('f-unified-input').value : '').toLowerCase().trim();
    const filtered = query ? cachedFriends.filter(f => f.username.toLowerCase().includes(query)) : cachedFriends;
    if (!filtered.length) {
        if (query) {
            el.innerHTML = '<div class="card" style="padding:20px;text-align:center;border:2px dashed var(--border)"><p style="color:var(--text);margin-bottom:8px">No friends found matching "<strong>' + esc(query) + '</strong>"</p><p class="muted" style="font-size:0.875rem">Try a different search term</p></div>';
        } else {
            el.innerHTML = '<p class="muted" style="text-align:center;padding:24px">No friends yet. Add one above!</p>';
        }
        return;
    }
    el.innerHTML = '';
    // Sort: online first
    filtered.sort((a, b) => (b.online ? 1 : 0) - (a.online ? 1 : 0));
    filtered.forEach(f => {
        const card = document.createElement('div'); card.className = 'friend-card';
        card.innerHTML = `<div class="friend-info"><span class="online-dot ${f.online ? 'online' : ''}" data-uid="${f.friendId}"></span><span class="friend-name">${esc(f.username)}</span></div><div class="friend-actions"><button class="btn btn-primary btn-sm" data-chat="${f.friendId}" data-name="${esc(f.username)}">Chat</button><button class="btn-link danger sm" data-remove="${f.friendId}">Remove</button></div>`;
        card.querySelector('[data-chat]').addEventListener('click', () => openFriendSession(f.friendId, f.username));
        card.querySelector('[data-remove]').addEventListener('click', () => removeFriend(f.friendId));
        el.appendChild(card);
    });
}

async function refreshRequests() {
    if (!getToken()) return;
    try {
        const r = await fetch('/api/friend/requests', { headers: authHeader() });
        if (r.status === 401) return;
        if (!r.ok) return;
        const data = await r.json();
        // Received
        const recvEl = $('f-req-received');
        if (!data.received.length) { recvEl.innerHTML = '<p class="muted">None</p>'; }
        else {
            recvEl.innerHTML = '';
            data.received.forEach(req => {
                const card = document.createElement('div'); card.className = 'request-card';
                card.innerHTML = `<span class="req-name">${esc(req.username)}</span><div class="req-actions"><button class="btn btn-primary btn-sm" data-accept="${req.id}">Accept</button><button class="btn-link danger sm" data-reject="${req.id}">Reject</button></div>`;
                card.querySelector('[data-accept]').addEventListener('click', () => acceptRequest(req.id));
                card.querySelector('[data-reject]').addEventListener('click', () => rejectRequest(req.id));
                recvEl.appendChild(card);
            });
        }
        // Sent
        const sentEl = $('f-req-sent');
        if (!data.sent.length) { sentEl.innerHTML = '<p class="muted">None</p>'; }
        else {
            sentEl.innerHTML = '';
            data.sent.forEach(req => {
                const card = document.createElement('div'); card.className = 'request-card';
                card.innerHTML = `<span class="req-name">${esc(req.username)}</span><span class="muted">Pending</span>`;
                sentEl.appendChild(card);
            });
        }
        // Badge
        const badge = $('f-req-badge');
        if (data.received.length > 0) { badge.textContent = data.received.length; show('f-req-badge', true); }
        else { show('f-req-badge', false); }
    } catch (e) {}
}

async function acceptRequest(id) {
    try {
        const r = await fetch('/api/friend/request/' + id + '/accept', { method: 'POST', headers: authHeader() });
        if (!r.ok) {
            const d = await r.json().catch(() => ({}));
            const msg = d.message || 'Failed';
            showToast(msg, 'error');
            if (/not pending|not found/i.test(msg)) markFriendRequestNotification(id, 'accepted');
            refreshRequests();
            return false;
        }
        showToast('Accepted', 'success');
        markFriendRequestNotification(id, 'accepted');
        refreshRequests();
        refreshFriendList();
        return true;
    } catch (e) { showToast(e.message); }
    return false;
}

async function rejectRequest(id) {
    try {
        const r = await fetch('/api/friend/request/' + id + '/reject', { method: 'POST', headers: authHeader() });
        if (!r.ok) {
            const d = await r.json().catch(() => ({}));
            const msg = d.message || 'Failed';
            showToast(msg, 'error');
            if (/not found/i.test(msg)) markFriendRequestNotification(id, 'rejected');
            refreshRequests();
            return false;
        }
        showToast('Rejected', 'success');
        markFriendRequestNotification(id, 'rejected');
        refreshRequests();
        return true;
    } catch (e) { showToast(e.message); }
    return false;
}

async function removeFriend(friendId) {
    try {
        await fetch('/api/friend/' + friendId, { method: 'DELETE', headers: authHeader() });
        showToast('Removed');
        refreshFriendList();
    } catch (e) {}
}

/* ==================== Friend Session ==================== */
function openFriendSession(friendId, friendUsername) {
    fChatFriendId = friendId;
    $('f-session-friend-name').textContent = friendUsername;
    renderConfigFriendsList();
    show('f-session-config-modal', true);
}

function renderConfigFriendsList() {
    const el = $('f-config-friends-list');
    if (!el) return;
    const query = ($('f-config-search') ? $('f-config-search').value : '').toLowerCase().trim();
    // Filter out the primary friend being chatted with
    const filtered = cachedFriends.filter(f => {
        if (f.friendId === fChatFriendId) return false;
        if (query && !f.username.toLowerCase().includes(query)) return false;
        return true;
    });
    if (!filtered.length) {
        el.innerHTML = query
            ? '<p class="muted" style="padding:8px">No matching friends</p>'
            : '<p class="muted" style="padding:8px">No other friends to invite</p>';
        return;
    }
    el.innerHTML = '';
    filtered.forEach(f => {
        const label = document.createElement('label');
        label.className = 'friend-check-card';
        label.innerHTML = `<input type="checkbox" value="${f.friendId}" class="f-config-check"> <span class="online-dot ${f.online ? 'online' : ''}" data-uid="${f.friendId}"></span> <span>${esc(f.username)}</span>`;
        el.appendChild(label);
    });
}

async function confirmSessionConfig() {
    show('f-session-config-modal', false);
    const chip = document.querySelector('#f-config-expire-bar .type-chip.active');
    const expire = chip ? parseInt(chip.dataset.expire) : 1800;
    const sessionName = $('f-config-name').value.trim() || null;
    const additionalFriends = Array.from(document.querySelectorAll('.f-config-check:checked')).map(cb => parseInt(cb.value));

    try {
        // Create session with primary friend
        const r = await fetch('/api/friend/session/invite', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', ...authHeader() },
            body: JSON.stringify({ friendId: fChatFriendId, expireSeconds: expire, sessionName })
        });
        if (!r.ok) { const d = await r.json().catch(() => ({})); showToast(d.message || 'Failed', 'error'); return; }
        const data = await r.json();

        // Invite additional friends if any
        for (const friendId of additionalFriends) {
            await fetch('/api/friend/session/invite', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', ...authHeader() },
                body: JSON.stringify({ friendId, sessionId: data.sessionId })
            });
        }

        showToast('Session created, invitations sent', 'success');
        $('f-config-name').value = '';
        $('f-config-search').value = '';
        enterFriendSession(data.sessionId, null, expire);
    } catch (e) { showToast(e.message, 'error'); }
}

async function confirmFriendSession() {
    show('f-expire-modal', false);
    const chip = document.querySelector('#f-expire-bar .type-chip.active');
    const expire = chip ? parseInt(chip.dataset.expire) : 1800;
    try {
        const r = await fetch('/api/friend/session/invite', { method: 'POST', headers: { 'Content-Type': 'application/json', ...authHeader() }, body: JSON.stringify({ friendId: fChatFriendId, expireSeconds: expire }) });
        if (!r.ok) { const d = await r.json().catch(() => ({})); showToast(d.message || 'Failed'); return; }
        const data = await r.json();
        showToast('Invitation sent, waiting for response...');
        // Enter session immediately as admin (can send messages while waiting)
        enterFriendSession(data.sessionId, null, expire);
    } catch (e) { showToast(e.message); }
}

async function acceptSessionInvitation(invitationId) {
    try {
        const r = await fetch('/api/friend/session/invite/' + invitationId + '/accept', { method: 'POST', headers: authHeader() });
        if (!r.ok) {
            const d = await r.json().catch(() => ({}));
            const msg = d.message || 'Failed';
            showToast(msg, 'error');
            // Mark notification as handled on failure too (session gone/expired)
            markInvitationNotification(invitationId, 'accepted');
            refreshPendingInvitations();
            return;
        }
        const data = await r.json();
        showToast('Invitation accepted');
        // Mark notification as handled
        markInvitationNotification(invitationId, 'accepted');
        refreshPendingInvitations();
        // Switch to Friends tab before entering session
        switchToFriendsTab();
        enterFriendSession(data.sessionId, null, data.remainingSeconds);
    } catch (e) { showToast(e.message); }
}

async function declineSessionInvitation(invitationId) {
    try {
        const r = await fetch('/api/friend/session/invite/' + invitationId + '/decline', { method: 'POST', headers: authHeader() });
        if (!r.ok) {
            const d = await r.json().catch(() => ({}));
            const msg = d.message || 'Failed';
            showToast(msg, 'error');
            // Mark notification as handled on failure too
            markInvitationNotification(invitationId, 'declined');
            refreshPendingInvitations();
            return;
        }
        showToast('Invitation declined');
        // Mark notification as handled
        markInvitationNotification(invitationId, 'declined');
        refreshPendingInvitations();
    } catch (e) { showToast(e.message); }
}

function switchToFriendsTab() {
    document.querySelectorAll('#mode-nav .pill').forEach(b => b.classList.remove('active'));
    const friendPill = document.querySelector('#mode-nav .pill[data-mode="friend"]');
    if (friendPill) friendPill.classList.add('active');
    show('view-quick', false); show('view-session', false); show('view-space', false); show('view-friend', true);
    stopRecvPoll(); stopSpacePoll(); disconnectSpaceWS();
    currentMainTab = 'friend';
    saveHash();
}

async function refreshPendingInvitations() {
    if (!getToken()) return;
    try {
        const r = await fetch('/api/friend/session/invitations', { headers: authHeader() });
        if (!r.ok) return;
        const invitations = await r.json();
        const el = $('f-invitations');
        if (!el) return;
        // Update badge on Sessions tab
        const badge = $('f-inv-badge');
        if (badge) {
            if (invitations.length > 0) { badge.textContent = invitations.length; show('f-inv-badge', true); }
            else { show('f-inv-badge', false); }
        }
        if (!invitations.length) { el.innerHTML = ''; updateSessionsEmptyState(); return; }
        el.innerHTML = '';
        invitations.forEach(inv => {
            const card = document.createElement('div'); card.className = 'invitation-card';
            const durMin = Math.round(inv.expireSeconds / 60);
            card.innerHTML = `<div class="inv-info"><strong>${esc(inv.fromUsername)}</strong> invited you to a session <span class="muted">(${durMin} min)</span></div><div class="inv-actions"><button class="btn btn-primary btn-sm" data-accept="${inv.invitationId}">Accept</button><button class="btn-link danger sm" data-decline="${inv.invitationId}">Decline</button></div>`;
            card.querySelector('[data-accept]').addEventListener('click', () => acceptSessionInvitation(inv.invitationId));
            card.querySelector('[data-decline]').addEventListener('click', () => declineSessionInvitation(inv.invitationId));
            el.appendChild(card);
        });
        updateSessionsEmptyState();
    } catch (e) {}
}

function enterFriendSession(sessionId, friendName, expireSec) {
    activeFSession = sessionId;
    if (friendName) $('f-session-friend-name').textContent = friendName;
    else $('f-session-friend-name').textContent = 'Session';
    $('f-session-status').textContent = 'ACTIVE';
    $('f-session-status').className = 'badge green';
    show('f-myfriends', false); show('f-sessions', false); show('f-history', false);
    document.querySelector('#friend-nav').style.display = 'none';
    show('f-session-members', false);
    show('f-session-invite-modal', false);
    $('f-session-close-btn').style.display = 'none';
    show('f-session-overlay', true);
    fSessionAttach = [];
    $('f-session-items').innerHTML = '';
    $('f-session-attachments').innerHTML = '';
    show('f-session-attachments', false);
    if (fSessionTimerId) clearInterval(fSessionTimerId);
    fSessionTimerId = null;
    // Timer will be set by refreshFriendSessionItems based on activatedAt
    $('f-session-timer').textContent = 'Loading...';
    show('f-session-start-btn', false);
    refreshFriendSessionItems();
}

function leaveFriendSessionView() {
    show('f-session-overlay', false);
    document.querySelector('#friend-nav').style.display = '';
    // Switch to Sessions tab (most natural after leaving a session)
    document.querySelectorAll('#friend-nav .pill').forEach(b => b.classList.remove('active'));
    const sessTab = document.querySelector('#friend-nav .pill[data-tab="f-sessions"]');
    if (sessTab) sessTab.classList.add('active');
    show('f-myfriends', false); show('f-sessions', true); show('f-history', false);
    if (fSessionTimerId) { clearInterval(fSessionTimerId); fSessionTimerId = null; }
    activeFSession = null;

    // Clear the active sessions list immediately to prevent stale UI
    const activeEl = $('f-active-sessions');
    if (activeEl) activeEl.innerHTML = '<p class="muted" style="padding:12px;text-align:center">Refreshing...</p>';

    // Delay refresh to ensure backend operations complete
    setTimeout(() => {
        refreshPendingInvitations();
        refreshActiveSessions();
        refreshFriendSessionHistory();
    }, 500);
}

async function closeFriendSession() {
    if (!activeFSession) return;
    const sessionToClose = activeFSession;
    try {
        const r = await fetch('/api/friend/session/' + sessionToClose, { method: 'DELETE', headers: authHeader() });
        if (!r.ok) {
            const errorData = await r.json().catch(() => ({}));
            throw new Error(errorData.message || `HTTP ${r.status}`);
        }
        showToast('Session closed', 'success');
        // Mark this session as closed to prevent re-entry
        closedSessions.add(sessionToClose);
    } catch (e) {
        console.error('Close session error:', e);
        showToast('Failed to close session: ' + e.message, 'error');
        return; // Don't leave view if close failed
    }
    leaveFriendSessionView();
}

async function leaveCurrentSession() {
    if (!activeFSession) return;
    const myId = parseInt(localStorage.getItem('bf_uid') || '0');
    // Check if current user is admin
    try {
        const r = await fetch('/api/friend/session/' + activeFSession, { headers: authHeader() });
        if (!r.ok) { leaveFriendSessionView(); return; }
        const data = await r.json();
        const isAdmin = data.adminId === myId;
        if (isAdmin && data.participants && data.participants.length > 1) {
            // Show transfer/dissolve modal
            showAdminLeaveModal(data.participants.filter(p => p.userId !== myId));
            return;
        }
        // Non-admin or solo admin: just leave/close
        await fetch('/api/friend/session/' + activeFSession + '/leave', { method: 'POST', headers: authHeader() });
        showToast('Left session');
        leaveFriendSessionView();
    } catch (e) { showToast(e.message); }
}

function showAdminLeaveModal(otherParticipants) {
    const modal = $('f-admin-leave-modal');
    const list = $('f-admin-leave-list');
    list.innerHTML = '';
    otherParticipants.forEach(p => {
        const opt = document.createElement('label');
        opt.className = 'friend-check-card';
        opt.innerHTML = `<input type="radio" name="transfer-target" value="${p.userId}"> <span>${esc(p.username)}</span>`;
        list.appendChild(opt);
    });
    show('f-admin-leave-modal', true);
}

async function adminLeaveTransfer() {
    const selected = document.querySelector('input[name="transfer-target"]:checked');
    if (!selected) { showToast('Select a member to transfer admin'); return; }
    const newAdminId = parseInt(selected.value);
    try {
        const r = await fetch('/api/friend/session/' + activeFSession + '/transfer', {
            method: 'POST', headers: { 'Content-Type': 'application/json', ...authHeader() },
            body: JSON.stringify({ newAdminId })
        });
        if (!r.ok) { const d = await r.json().catch(() => ({})); showToast(d.message || 'Failed'); return; }
        showToast('Admin transferred, you left the session');
        show('f-admin-leave-modal', false);
        leaveFriendSessionView();
    } catch (e) { showToast(e.message); }
}

async function adminLeaveDissolve() {
    show('f-admin-leave-modal', false);
    await closeFriendSession();
}

async function activateCurrentSession() {
    if (!activeFSession) return;
    try {
        const r = await fetch('/api/friend/session/' + activeFSession + '/activate', { method: 'POST', headers: authHeader() });
        if (!r.ok) { const d = await r.json().catch(() => ({})); showToast(d.message || 'Failed'); return; }
        showToast('Session started');
        show('f-session-start-btn', false);
        refreshFriendSessionItems();
    } catch (e) { showToast(e.message); }
}

async function refreshSessionMembers() {
    if (!activeFSession) return;
    try {
        const r = await fetch('/api/friend/session/' + activeFSession, { headers: authHeader() });
        if (!r.ok) return;
        const data = await r.json();
        const el = $('f-session-members-list');
        const myId = parseInt(localStorage.getItem('bf_uid') || '0');
        const isAdmin = data.adminId === myId;
        el.innerHTML = '';
        (data.participants || []).forEach(p => {
            const row = document.createElement('div'); row.className = 'member-row';
            const isMe = p.userId === myId;
            let html = `<span class="member-name">${esc(p.username)}${p.userId === data.adminId ? ' <span class="badge blue-sm">Admin</span>' : ''}${isMe ? ' <span class="muted">(you)</span>' : ''}</span>`;
            if (isAdmin && !isMe) {
                html += `<button class="btn-link danger sm" data-kick="${p.userId}">Kick</button>`;
            }
            row.innerHTML = html;
            const kickBtn = row.querySelector('[data-kick]');
            if (kickBtn) kickBtn.addEventListener('click', () => kickSessionMember(p.userId));
            el.appendChild(row);
        });
        // Show/hide close button (admin only)
        $('f-session-close-btn').style.display = isAdmin ? '' : 'none';
    } catch (e) {}
}

async function kickSessionMember(targetId) {
    if (!activeFSession) return;
    try {
        const r = await fetch('/api/friend/session/' + activeFSession + '/kick/' + targetId, { method: 'POST', headers: authHeader() });
        if (!r.ok) { const d = await r.json().catch(() => ({})); showToast(d.message || 'Failed'); return; }
        showToast('Member kicked');
        refreshSessionMembers();
        refreshFriendSessionItems();
    } catch (e) { showToast(e.message); }
}

async function openSessionInviteModal() {
    if (!activeFSession) return;
    try {
        // Fetch current session data to get participant list
        const r = await fetch('/api/friend/session/' + activeFSession, { headers: authHeader() });
        if (!r.ok) return;
        const sessionData = await r.json();
        const participantIds = new Set((sessionData.participants || []).map(p => p.userId));

        const list = $('f-session-invite-list');
        list.innerHTML = '';

        // Filter out friends who are already in the session
        const availableFriends = cachedFriends.filter(f => !participantIds.has(f.friendId));

        if (!availableFriends.length) {
            list.innerHTML = '<p class="muted" style="padding:8px">All friends are already in this session</p>';
        } else {
            availableFriends.forEach(f => {
                const label = document.createElement('label');
                label.className = 'friend-check-card';
                label.innerHTML = `<input type="checkbox" value="${f.friendId}" class="f-invite-check"> <span class="online-dot ${f.online ? 'online' : ''}" data-uid="${f.friendId}"></span> <span>${esc(f.username)}</span>`;
                list.appendChild(label);
            });
        }
        show('f-session-invite-modal', true);
    } catch (e) {
        showToast('Failed to load session data', 'error');
    }
}

async function sendSessionInviteFromModal() {
    const checked = document.querySelectorAll('.f-invite-check:checked');
    if (!checked.length) { showToast('Select at least one friend'); return; }
    const btn = $('f-session-invite-send'); btn.disabled = true;
    try {
        for (const cb of checked) {
            const friendId = parseInt(cb.value);
            const r = await fetch('/api/friend/session/invite', {
                method: 'POST', headers: { 'Content-Type': 'application/json', ...authHeader() },
                body: JSON.stringify({ friendId, sessionId: activeFSession })
            });
            if (!r.ok) { const d = await r.json().catch(() => ({})); showToast(d.message || 'Failed to invite'); }
        }
        showToast('Invitations sent');
        show('f-session-invite-modal', false);
    } catch (e) { showToast(e.message); }
    finally { btn.disabled = false; }
}

async function refreshActiveSessions() {
    if (!getToken()) return;
    try {
        const r = await fetch('/api/friend/session/active', { headers: authHeader() });
        if (!r.ok) return;
        const sessions = await r.json();
        const el = $('f-active-sessions');
        if (!el) return;

        // Filter out closed sessions
        const validSessions = sessions.filter(s => !closedSessions.has(s.sessionId));

        if (!validSessions.length) { el.innerHTML = ''; updateSessionsEmptyState(); return; }
        el.innerHTML = '';
        const myId = parseInt(localStorage.getItem('bf_uid') || '0');
        validSessions.forEach(s => {
            // Use sessionName if available, otherwise use participant names
            let displayName;
            if (s.sessionName) {
                displayName = s.sessionName;
            } else {
                const otherNames = (s.participants || [])
                    .filter(p => p.userId !== myId)
                    .map(p => p.username).join(', ');
                displayName = otherNames || 'Session';
            }
            const remainMin = Math.ceil(s.remainingSeconds / 60);
            const card = document.createElement('div'); card.className = 'active-session-card';
            card.innerHTML = `<div class="as-info"><strong>${esc(displayName)}</strong> <span class="muted">(${remainMin} min left)</span></div><div class="as-actions"><button class="btn btn-primary btn-sm" data-reenter="${s.sessionId}">Re-enter</button></div>`;
            card.querySelector('[data-reenter]').addEventListener('click', () => {
                enterFriendSession(s.sessionId, null, s.remainingSeconds);
            });
            el.appendChild(card);
        });
        updateSessionsEmptyState();
    } catch (e) {}
}

function updateSessionsEmptyState() {
    const empty = $('f-sessions-empty');
    if (!empty) return;
    const hasInv = $('f-invitations') && $('f-invitations').children.length > 0;
    const hasSess = $('f-active-sessions') && $('f-active-sessions').children.length > 0;
    empty.classList.toggle('hidden', hasInv || hasSess);
}

function renderCreateSessionFriends() {
    const el = $('f-create-friends-list');
    if (!el) return;
    const query = ($('f-create-search') ? $('f-create-search').value : '').toLowerCase().trim();
    const filtered = query ? cachedFriends.filter(f => f.username.toLowerCase().includes(query)) : cachedFriends;
    if (!filtered.length) {
        el.innerHTML = query
            ? '<p class="muted" style="padding:8px">No matching friends</p>'
            : '<p class="muted" style="padding:8px">No friends yet</p>';
        return;
    }
    el.innerHTML = '';
    filtered.forEach(f => {
        const label = document.createElement('label');
        label.className = 'friend-check-card';
        label.innerHTML = `<input type="checkbox" value="${f.friendId}" class="f-create-check"> <span class="online-dot ${f.online ? 'online' : ''}" data-uid="${f.friendId}"></span> <span>${esc(f.username)}</span>`;
        el.appendChild(label);
    });
}

async function createSessionFromTab() {
    const checked = document.querySelectorAll('.f-create-check:checked');
    if (!checked.length) { showToast('Select at least one friend'); return; }
    const chip = document.querySelector('#f-create-expire-bar .type-chip.active');
    const expire = chip ? parseInt(chip.dataset.expire) : 1800;
    const sessionName = $('f-create-name').value.trim() || null;
    const friendIds = Array.from(checked).map(c => parseInt(c.value));
    const btn = $('f-create-session-btn'); btn.disabled = true;
    try {
        // Send invitation to first friend (creates the session)
        const r = await fetch('/api/friend/session/invite', {
            method: 'POST', headers: { 'Content-Type': 'application/json', ...authHeader() },
            body: JSON.stringify({ friendId: friendIds[0], expireSeconds: expire, sessionName })
        });
        if (!r.ok) { const d = await r.json().catch(() => ({})); showToast(d.message || 'Failed'); return; }
        const data = await r.json();
        // Invite remaining friends to the same session
        for (let i = 1; i < friendIds.length; i++) {
            await fetch('/api/friend/session/invite', {
                method: 'POST', headers: { 'Content-Type': 'application/json', ...authHeader() },
                body: JSON.stringify({ friendId: friendIds[i], sessionId: data.sessionId })
            });
        }
        showToast('Session created, invitations sent', 'success');
        $('f-create-name').value = '';
        $('f-create-search').value = '';
        enterFriendSession(data.sessionId, null, expire);
    } catch (e) { showToast(e.message, 'error'); }
    finally { btn.disabled = false; }
}

async function refreshFriendSessionItems() {
    if (!activeFSession) return;
    try {
        const r = await fetch('/api/friend/session/' + activeFSession, { headers: authHeader() });
        if (!r.ok) return;
        const data = await r.json();
        if (data.status === 'CLOSED') {
            $('f-session-status').textContent = 'CLOSED';
            $('f-session-status').className = 'badge red';
            showToast('Session has been closed');
            return;
        }
        const myId = parseInt(localStorage.getItem('bf_uid') || '0');
        const isAdmin = data.adminId === myId;

        // Use sessionName if available, otherwise fall back to participant names
        if (data.sessionName) {
            $('f-session-friend-name').textContent = data.sessionName;
        } else {
            const otherNames = (data.participants || [])
                .filter(p => p.userId !== myId)
                .map(p => p.username);
            if (otherNames.length > 0) {
                $('f-session-friend-name').textContent = otherNames.join(', ');
            }
        }

        $('f-session-close-btn').style.display = isAdmin ? '' : 'none';

        // Timer logic based on activatedAt
        if (data.activatedAt) {
            // Session is activated — run countdown
            show('f-session-start-btn', false);
            if (!fSessionTimerId) {
                let rem = data.remainingSeconds;
                $('f-session-timer').textContent = fmtTimer(Math.max(rem, 0));
                fSessionTimerId = setInterval(() => {
                    rem--;
                    $('f-session-timer').textContent = fmtTimer(Math.max(rem, 0));
                    if (rem <= 0) { clearInterval(fSessionTimerId); fSessionTimerId = null; showToast('Session expired'); leaveFriendSessionView(); }
                }, 1000);
            }
        } else {
            // Not activated yet
            $('f-session-timer').textContent = 'Not started';
            show('f-session-start-btn', isAdmin);
        }

        renderFriendSessionTimeline(data.items, data.sessionId);
    } catch (e) {}
}

function renderFriendSessionTimeline(items, sessionId) {
    const el = $('f-session-items');
    const myId = parseInt(localStorage.getItem('bf_uid') || '0');
    if (!items || !items.length) { el.innerHTML = '<p class="muted" style="text-align:center;padding:24px">No messages yet</p>'; return; }
    el.innerHTML = '';
    // Build sender color map — current user always gets color-0 (blue)
    const senderIds = [...new Set(items.map(i => i.senderId))];
    const colorMap = {};
    colorMap[myId] = 0; // current user = blue
    let nextColor = 1;
    senderIds.forEach(id => {
        if (id !== myId) { colorMap[id] = nextColor % 8; nextColor++; }
    });
    items.forEach(item => {
        const isMine = item.senderId === myId;
        const colorIdx = colorMap[item.senderId] || 0;
        const card = document.createElement('div');
        card.className = 'timeline-card f-session-item ' + (isMine ? 'sent' : 'received');
        card.style.borderLeftColor = 'var(--user-color-' + colorIdx + ')';
        const typeClass = item.type === 'TEXT' ? 'text-type' : item.type === 'IMAGE' ? 'image-type' : 'file-type';
        let html = `<div class="f-sender-label" style="color:var(--user-color-${colorIdx})">${esc(item.senderUsername)}</div>`;
        html += `<div class="timeline-header"><span class="badge ${typeClass}">${item.type}</span><span class="time-label">${fmtTime(item.addedAt)}</span></div>`;
        if (item.type === 'TEXT') {
            html += `<pre class="text-bubble">${esc(item.content)}</pre><div style="margin-top:6px"><button class="btn-link" data-copy="${esc(item.content)}">Copy</button></div>`;
        } else if (item.type === 'IMAGE') {
            html += '<div class="img-grid">';
            (item.files || []).forEach(f => {
                html += `<div class="img-card"><img src="/api/friend/session/${sessionId}/items/${item.id}/preview/${f.index}?token=${getToken()}"><div class="img-footer"><span>${esc(f.fileName)}</span><button class="btn-link sm" data-url="/api/friend/session/${sessionId}/items/${item.id}/download/${f.index}" data-name="${esc(f.fileName)}">Download</button></div></div>`;
            });
            html += '</div>';
        } else {
            (item.files || []).forEach(f => {
                html += `<div class="dl-row"><div class="dl-info"><div class="dl-name">${esc(f.fileName)}</div><div class="dl-size">${fmtSize(f.fileSize)}</div></div><button class="btn btn-primary btn-sm" data-url="/api/friend/session/${sessionId}/items/${item.id}/download/${f.index}" data-name="${esc(f.fileName)}">Download</button></div>`;
            });
        }
        card.innerHTML = html;
        card.querySelectorAll('[data-copy]').forEach(b => b.addEventListener('click', () => copyText(b.dataset.copy)));
        card.querySelectorAll('[data-url]').forEach(b => b.addEventListener('click', () => {
            const a = document.createElement('a');
            a.href = b.dataset.url + '?token=' + getToken();
            a.download = b.dataset.name || '';
            document.body.appendChild(a); a.click(); document.body.removeChild(a);
        }));
        el.appendChild(card);
    });
    // Auto-scroll to bottom
    el.scrollTop = el.scrollHeight;
}

function renderFSessionAttach() {
    const bar = $('f-session-attachments');
    bar.innerHTML = '';
    show('f-session-attachments', fSessionAttach.length > 0);
    fSessionAttach.forEach((f, i) => {
        const tag = document.createElement('span'); tag.className = 'attach-tag';
        tag.innerHTML = `<span class="tag-name">${esc(f.name)}</span>`;
        const btn = document.createElement('button'); btn.className = 'tag-remove'; btn.textContent = '\u00d7';
        btn.addEventListener('click', () => { fSessionAttach.splice(i, 1); renderFSessionAttach(); });
        tag.appendChild(btn); bar.appendChild(tag);
    });
}

async function sendFriendSessionItem() {
    const text = $('f-session-text').value.trim();
    const hasFiles = fSessionAttach.length > 0;
    if (!text && !hasFiles) { showToast('Enter text or attach files'); return; }
    const btn = $('f-session-send-btn'); btn.disabled = true;
    try {
        if (text) {
            const r = await fetch('/api/friend/session/' + activeFSession + '/items/text', { method: 'POST', headers: { 'Content-Type': 'application/json', ...authHeader() }, body: JSON.stringify({ content: text }) });
            if (r.status === 401) { onAuthFail(); return; }
            if (!r.ok) throw new Error('Failed');
            $('f-session-text').value = '';
        }
        if (hasFiles) {
            const fd = new FormData();
            fSessionAttach.forEach(f => fd.append('file', f));
            const r = await fetch('/api/friend/session/' + activeFSession + '/items/file', { method: 'POST', headers: authHeader(), body: fd });
            if (r.status === 401) { onAuthFail(); return; }
            if (!r.ok) throw new Error('Failed');
            fSessionAttach = []; renderFSessionAttach();
        }
        refreshFriendSessionItems();
    } catch (e) { showToast(e.message); }
    finally { btn.disabled = false; }
}

/* ==================== Friend Session History ==================== */
async function refreshFriendSessionHistory() {
    if (!getToken()) return;
    try {
        const r = await fetch('/api/friend/session/history', { headers: authHeader() });
        if (!r.ok) return;
        const list = await r.json();
        const el = $('f-history-list');
        if (!list.length) { el.innerHTML = '<p class="muted" style="text-align:center;padding:24px">No session history</p>'; return; }
        el.innerHTML = '';
        list.forEach(h => {
            const card = document.createElement('div'); card.className = 'history-card clickable';
            card.innerHTML = `<div class="hist-name">${esc(h.participants)}</div><div class="hist-meta">${h.participantCount} members &middot; ${h.itemCount} items &middot; ${fmtTime(h.createdAt)} - ${fmtTime(h.closedAt)}</div>`;
            card.addEventListener('click', () => openHistoryDetail(h.id, h.participants));
            el.appendChild(card);
        });
    } catch (e) {}
}

async function openHistoryDetail(historyId, title) {
    activeHistoryId = historyId;
    $('f-history-title').textContent = title || 'Session';
    $('f-history-meta').textContent = '';
    show('f-myfriends', false); show('f-sessions', false); show('f-history', false);
    document.querySelector('#friend-nav').style.display = 'none';
    show('f-history-overlay', true);
    $('f-history-items').innerHTML = '<p class="muted" style="text-align:center;padding:24px">Loading...</p>';
    try {
        const r = await fetch('/api/friend/session/history/' + historyId, { headers: authHeader() });
        if (r.status === 401) { onAuthFail(); return; }
        if (!r.ok) throw new Error('Failed to load history');
        const data = await r.json();
        $('f-history-meta').textContent = data.participantCount + ' members \u00B7 ' + data.itemCount + ' items \u00B7 ' + fmtTime(data.createdAt) + ' - ' + fmtTime(data.closedAt);
        renderHistoryTimeline(data.items, historyId);
    } catch (e) {
        $('f-history-items').innerHTML = '<p class="muted" style="text-align:center;padding:24px">Failed to load</p>';
    }
}

function renderHistoryTimeline(items, historyId) {
    const el = $('f-history-items');
    const myId = parseInt(localStorage.getItem('bf_uid') || '0');
    if (!items || !items.length) { el.innerHTML = '<p class="muted" style="text-align:center;padding:24px">No items in this session</p>'; return; }
    el.innerHTML = '';
    const senderIds = [...new Set(items.map(i => i.senderId))];
    const colorMap = {}; colorMap[myId] = 0; let nextColor = 1;
    senderIds.forEach(id => { if (id !== myId) { colorMap[id] = nextColor % 8; nextColor++; } });
    items.forEach(item => {
        const isMine = item.senderId === myId;
        const colorIdx = colorMap[item.senderId] || 0;
        const card = document.createElement('div');
        card.className = 'timeline-card f-session-item ' + (isMine ? 'sent' : 'received');
        card.style.borderLeftColor = 'var(--user-color-' + colorIdx + ')';
        const typeClass = item.type === 'TEXT' ? 'text-type' : item.type === 'IMAGE' ? 'image-type' : 'file-type';
        let html = `<div class="f-sender-label" style="color:var(--user-color-${colorIdx})">${esc(item.senderUsername)}</div>`;
        html += `<div class="timeline-header"><span class="badge ${typeClass}">${item.type}</span><span class="time-label">${fmtTime(item.addedAt)}</span></div>`;
        if (item.type === 'TEXT') {
            html += `<pre class="text-bubble">${esc(item.content)}</pre><div style="margin-top:6px"><button class="btn-link" data-copy="${esc(item.content)}">Copy</button></div>`;
        } else if (item.type === 'IMAGE') {
            html += '<div class="img-grid">';
            (item.files || []).forEach(f => {
                html += `<div class="img-card"><img src="/api/friend/session/history/${historyId}/items/${item.id}/preview/${f.index}?token=${getToken()}"><div class="img-footer"><span>${esc(f.fileName)}</span><button class="btn-link sm" data-url="/api/friend/session/history/${historyId}/items/${item.id}/download/${f.index}" data-name="${esc(f.fileName)}">Download</button></div></div>`;
            });
            html += '</div>';
        } else {
            (item.files || []).forEach(f => {
                html += `<div class="dl-row"><div class="dl-info"><div class="dl-name">${esc(f.fileName)}</div><div class="dl-size">${fmtSize(f.fileSize)}</div></div><button class="btn btn-primary btn-sm" data-url="/api/friend/session/history/${historyId}/items/${item.id}/download/${f.index}" data-name="${esc(f.fileName)}">Download</button></div>`;
            });
        }
        card.innerHTML = html;
        card.querySelectorAll('[data-copy]').forEach(b => b.addEventListener('click', () => copyText(b.dataset.copy)));
        card.querySelectorAll('[data-url]').forEach(b => b.addEventListener('click', () => {
            const a = document.createElement('a');
            a.href = b.dataset.url + '?token=' + getToken();
            a.download = b.dataset.name || '';
            document.body.appendChild(a); a.click(); document.body.removeChild(a);
        }));
        el.appendChild(card);
    });
}

function leaveHistoryDetailView() {
    show('f-history-overlay', false);
    document.querySelector('#friend-nav').style.display = '';
    document.querySelectorAll('#friend-nav .pill').forEach(b => b.classList.remove('active'));
    const histTab = document.querySelector('#friend-nav .pill[data-tab="f-history"]');
    if (histTab) histTab.classList.add('active');
    show('f-myfriends', false); show('f-sessions', false); show('f-history', true);
    activeHistoryId = null;
}

/* ==================== Notification Drawer ==================== */
function addNotification(text, type = 'info', data = null) {
    notifications.unshift({ id: Date.now(), text, type, time: new Date(), read: false, data });
    if (notifications.length > 50) notifications = notifications.slice(0, 50);
    renderNotifications();
}

function markInvitationNotification(invitationId, action) {
    notifications.forEach(n => {
        if (n.type === 'session_invitation' && n.data && n.data.invitationId === invitationId) {
            n.data.handled = action; // 'accepted' or 'declined'
            n.read = true;
        }
    });
    renderNotifications();
}

function markFriendRequestNotification(requestId, action) {
    notifications.forEach(n => {
        if (n.type === 'friend_request' && n.data && n.data.requestId === requestId) {
            n.data.handled = action;
            n.read = true;
        }
    });
    renderNotifications();
}

function renderNotifications() {
    const list = $('notif-list');
    const badge = $('notif-badge');
    const unreadCount = notifications.filter(n => !n.read).length;
    if (unreadCount > 0) {
        badge.textContent = unreadCount > 99 ? '99+' : unreadCount;
        show('notif-badge', true);
    } else {
        show('notif-badge', false);
    }
    if (!notifications.length) {
        list.innerHTML = '<p class="muted" style="text-align:center;padding:24px">No notifications</p>';
        return;
    }
    list.innerHTML = '';
    notifications.forEach(n => {
        const item = document.createElement('div');
        item.className = 'notif-item' + (n.read ? ' read' : '');
        const elapsed = Math.floor((Date.now() - n.time.getTime()) / 1000);
        const timeStr = elapsed < 60 ? 'Just now' : elapsed < 3600 ? Math.floor(elapsed / 60) + 'm ago' : Math.floor(elapsed / 3600) + 'h ago';

        let actionsHtml = '';
        if (n.type === 'session_invitation' && n.data && n.data.handled) {
            actionsHtml = `<div class="notif-actions"><span class="muted">You have already ${esc(n.data.handled)} this invitation</span></div>`;
        } else if (n.type === 'friend_request' && n.data && n.data.handled) {
            actionsHtml = `<div class="notif-actions"><span class="muted">You have already ${esc(n.data.handled)} this request</span></div>`;
        } else if (n.data) {
            if (n.type === 'friend_request' && n.data.requestId) {
                actionsHtml = `<div class="notif-actions"><button class="btn btn-primary btn-sm" data-accept-friend="${n.data.requestId}">Accept</button><button class="btn-link sm" data-reject-friend="${n.data.requestId}">Reject</button></div>`;
            } else if (n.type === 'session_invitation' && n.data.invitationId) {
                actionsHtml = `<div class="notif-actions"><button class="btn btn-primary btn-sm" data-accept-inv="${n.data.invitationId}">Accept</button><button class="btn-link sm" data-decline-inv="${n.data.invitationId}">Decline</button></div>`;
            }
        }

        item.innerHTML = `<div class="notif-dot"></div><div style="flex:1"><div class="notif-text">${esc(n.text)}</div><div class="notif-time">${timeStr}</div>${actionsHtml}</div>`;

        // Mark as read on click (but not on button click)
        item.addEventListener('click', (e) => {
            if (!e.target.closest('button')) {
                n.read = true;
                renderNotifications();
            }
        });

        // Bind action buttons
        const acceptFriendBtn = item.querySelector('[data-accept-friend]');
        if (acceptFriendBtn) {
            acceptFriendBtn.addEventListener('click', async (e) => {
                e.stopPropagation();
                await acceptRequest(parseInt(acceptFriendBtn.dataset.acceptFriend));
                n.read = true;
                renderNotifications();
            });
        }
        const rejectFriendBtn = item.querySelector('[data-reject-friend]');
        if (rejectFriendBtn) {
            rejectFriendBtn.addEventListener('click', async (e) => {
                e.stopPropagation();
                await rejectRequest(parseInt(rejectFriendBtn.dataset.rejectFriend));
                n.read = true;
                renderNotifications();
            });
        }
        const acceptInvBtn = item.querySelector('[data-accept-inv]');
        if (acceptInvBtn) {
            acceptInvBtn.addEventListener('click', async (e) => {
                e.stopPropagation();
                await acceptSessionInvitation(acceptInvBtn.dataset.acceptInv);
                n.read = true;
                renderNotifications();
                closeNotifDrawer();
            });
        }
        const declineInvBtn = item.querySelector('[data-decline-inv]');
        if (declineInvBtn) {
            declineInvBtn.addEventListener('click', async (e) => {
                e.stopPropagation();
                await declineSessionInvitation(declineInvBtn.dataset.declineInv);
                n.read = true;
                renderNotifications();
            });
        }

        list.appendChild(item);
    });
}

function closeNotifDrawer() {
    $('notif-drawer').classList.remove('open');
    setTimeout(() => {
        $('notif-backdrop').classList.add('hidden');
        $('notif-drawer').classList.add('hidden');
    }, 300);
}

function clearAllNotifications() {
    notifications = [];
    renderNotifications();
}