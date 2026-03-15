/* ==================== State ==================== */
let qType = 'text', qFiles = [];
let sessCode = null, sessAttach = [], sessTimerId = null;
let recvCode = null, recvPollId = null;
let spAttach = [], spPollId = null;

/* ==================== DOM Ready ==================== */
document.addEventListener('DOMContentLoaded', () => {
    // Mode nav
    document.querySelectorAll('#mode-nav .pill').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#mode-nav .pill').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            show('view-quick', btn.dataset.mode === 'quick');
            show('view-session', btn.dataset.mode === 'session');
            show('view-space', btn.dataset.mode === 'space');
            if (btn.dataset.mode !== 'session') stopRecvPoll();
            if (btn.dataset.mode === 'space') refreshSpace();
            else stopSpacePoll();
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

    // Session join
    $('sr-join-btn').addEventListener('click', sessJoin);
    $('sr-code').addEventListener('keydown', e => { if (e.key === 'Enter') sessJoin(); });
    $('sr-leave-btn').addEventListener('click', sessLeave);
    $('sr-closed-leave').addEventListener('click', sessLeave);

    // Auth
    $('login-btn').addEventListener('click', () => show('auth-modal', true));
    $('auth-close').addEventListener('click', () => show('auth-modal', false));
    $('auth-modal').addEventListener('click', e => { if (e.target === $('auth-modal')) show('auth-modal', false); });
    bindTabs('auth-nav', { 'auth-login-form': 'auth-login-form', 'auth-reg-form': 'auth-reg-form' });
    $('auth-login-form').addEventListener('submit', loginSubmit);
    $('auth-reg-form').addEventListener('submit', registerSubmit);
    $('logout-btn').addEventListener('click', logout);

    // Space
    $('sp-attach-file-btn').addEventListener('click', () => $('sp-file-in').click());
    $('sp-attach-img-btn').addEventListener('click', () => $('sp-img-in').click());
    $('sp-file-in').addEventListener('change', e => { spAttach = spAttach.concat(Array.from(e.target.files)); e.target.value = ''; renderSpAttach(); });
    $('sp-img-in').addEventListener('change', e => { spAttach = spAttach.concat(Array.from(e.target.files)); e.target.value = ''; renderSpAttach(); });
    $('sp-send-btn').addEventListener('click', spaceAdd);

    // Restore session
    restoreAuth();
});

/* ==================== Helpers ==================== */
function $(id) { return document.getElementById(id); }
function show(id, visible) { $(id).classList.toggle('hidden', !visible); }
function esc(s) { if (!s) return ''; const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
function fmtSize(b) { if (!b) return '0 B'; const k = 1024, u = ['B','KB','MB','GB'], i = Math.floor(Math.log(b)/Math.log(k)); return (b/Math.pow(k,i)).toFixed(1)+' '+u[i]; }
function fmtTime(iso) { if (!iso) return ''; const d = new Date(iso); return [d.getHours(),d.getMinutes(),d.getSeconds()].map(n=>String(n).padStart(2,'0')).join(':'); }
function fmtTimer(sec) { return Math.floor(sec/60) + ':' + String(sec%60).padStart(2,'0'); }

function showToast(msg) {
    const t = $('toast'); t.textContent = msg;
    t.classList.add('show');
    setTimeout(() => t.classList.remove('show'), 2200);
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
        showToast('Sent!');
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
        const resp = await fetch('/api/session', { method:'POST', headers:{'Content-Type':'application/json'}, body:'{}' });
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
}

/* ==================== Auth ==================== */
function getToken() { return localStorage.getItem('bf_token'); }
function getUser() { return localStorage.getItem('bf_user'); }

function authHeader() {
    const t = getToken();
    return t ? { 'Authorization': 'Bearer ' + t } : {};
}

function restoreAuth() {
    if (getToken() && getUser()) {
        onLogin(getUser());
    }
}

function onLogin(username) {
    show('user-area', false);
    show('user-info', true);
    $('user-display').textContent = username;
    show('nav-space', true);
}

function logout() {
    localStorage.removeItem('bf_token');
    localStorage.removeItem('bf_user');
    show('user-area', true);
    show('user-info', false);
    show('nav-space', false);
    stopSpacePoll();
    // If on space tab, switch to quick
    if (!$('view-space').classList.contains('hidden')) {
        document.querySelector('#mode-nav .pill[data-mode="quick"]').click();
    }
    showToast('Logged out');
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
    const btn = $('sp-send-btn'); btn.disabled = true;
    try {
        if (text) {
            const r = await fetch('/api/space/items/text', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', ...authHeader() },
                body: JSON.stringify({ content: text })
            });
            if (r.status === 401) { onAuthFail(); return; }
            if (!r.ok) throw new Error('Failed');
            $('sp-text').value = '';
        }
        if (hasFiles) {
            const fd = new FormData();
            spAttach.forEach(f => fd.append('file', f));
            const r = await fetch('/api/space/items/file', {
                method: 'POST', headers: authHeader(), body: fd
            });
            if (r.status === 401) { onAuthFail(); return; }
            if (!r.ok) throw new Error('Failed');
            spAttach = []; renderSpAttach();
        }
        showToast('Added');
        refreshSpace();
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
    } catch (e) { /* ignore */ }
    // Start polling if not already
    if (!spPollId) {
        spPollId = setInterval(refreshSpace, 10000);
    }
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
    if (el.dataset.n === String(items.length)) return;
    el.dataset.n = String(items.length);
    el.innerHTML = '';

    items.forEach(item => {
        const card = document.createElement('div'); card.className = 'timeline-card';
        const typeClass = item.type === 'TEXT' ? 'text-type' : item.type === 'IMAGE' ? 'image-type' : 'file-type';
        let html = `<div class="timeline-header"><span class="badge ${typeClass}">${item.type}</span><div class="row gap-sm"><span class="time-label">${fmtTime(item.createdAt)}</span><button class="timeline-delete" data-del="${item.id}">Delete</button></div></div>`;

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
            // Add auth token as query param for authenticated downloads
            a.href += (a.href.includes('?') ? '&' : '?') + 'token=' + getToken();
            a.download = b.dataset.name || '';
            document.body.appendChild(a); a.click(); document.body.removeChild(a);
        }));
        card.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', async () => {
            await fetch('/api/space/items/' + b.dataset.del, { method: 'DELETE', headers: authHeader() });
            el.dataset.n = '0'; // force re-render
            refreshSpace();
        }));
        el.appendChild(card);
    });
}

function onAuthFail() {
    logout();
    show('auth-modal', true);
    showToast('Session expired, please login again');
}
