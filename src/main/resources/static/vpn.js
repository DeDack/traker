const token = localStorage.getItem('token');
let currentUser = null;

function headers() {
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };
}

function showMessage(text, type = 'success') {
    const el = document.getElementById('message');
    if (!el) return;
    el.textContent = text;
    el.className = `alert alert-${type} shadow-sm`;
    el.classList.remove('d-none');
    setTimeout(() => el.classList.add('d-none'), 2500);
}

function toggleLoader(show) {
    const loader = document.getElementById('loader');
    if (loader) loader.style.display = show ? 'block' : 'none';
}

function escapeHtml(str = '') {
    return str.replace(/[&<>"']/g, c => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
}

function formatDateTime(value) {
    if (!value) return '—';
    const d = new Date(value);
    return Number.isNaN(d.getTime()) ? value : d.toLocaleString();
}

function statusBadge(status = '') {
    const map = {
        ACTIVE: 'success',
        EXPIRED: 'secondary',
        REVOKED: 'danger'
    };
    const cls = map[status] || 'info';
    return `<span class="badge bg-${cls} bg-opacity-75 text-uppercase">${escapeHtml(status)}</span>`;
}

async function loadCurrentUser() {
    try {
        const resp = await fetch('/api/users/me', {headers: headers()});
        if (!resp.ok) return;
        currentUser = await resp.json();
        const label = document.getElementById('user-label');
        if (label && currentUser?.name) {
            label.textContent = currentUser.name;
        }
        const roles = currentUser?.roles?.map(r => r?.name) || [];
        const auditLink = document.getElementById('audit-link');
        if (auditLink) {
            if (roles.includes('ADMIN')) {
                auditLink.classList.remove('d-none');
            } else {
                auditLink.classList.add('d-none');
            }
        }
    } catch (e) {
        console.error('Unable to load user', e);
    }
}

async function loadKeys() {
    toggleLoader(true);
    try {
        const resp = await fetch('/api/v1/vpn/keys', {headers: headers()});
        if (!resp.ok) return;
        const data = await resp.json();
        const body = document.querySelector('#keys-table tbody');
        if (!body) return;
        body.innerHTML = '';
        data.forEach(k => {
            const row = document.createElement('tr');
            const owner = currentUser?.id && k.ownerUserId === currentUser.id ? 'Вы' : (k.ownerUserId || '—');
            row.innerHTML = `
                <td>
                    <div class="fw-semibold">${escapeHtml(k.name)}</div>
                    <div class="text-muted small">${k.id}</div>
                </td>
                <td><span class="pill">${escapeHtml(k.protocol || 'VLESS')}</span></td>
                <td><span class="pill">${escapeHtml(k.realityDest || k.address)}</span></td>
                <td>${statusBadge(k.status)}</td>
                <td>${formatDateTime(k.expirationAt)}</td>
                <td>${formatDateTime(k.createdAt)}</td>
                <td>${escapeHtml(owner)}</td>
                <td class="text-end">
                    <div class="btn-group btn-group-sm" role="group">
                        <button data-id="${k.id}" class="btn btn-outline-primary download">Скачать</button>
                        <button data-id="${k.id}" class="btn btn-outline-danger revoke">Отозвать</button>
                    </div>
                </td>`;
            body.appendChild(row);
        });
        body.querySelectorAll('button.download').forEach(btn => btn.addEventListener('click', downloadConfig));
        body.querySelectorAll('button.revoke').forEach(btn => btn.addEventListener('click', revoke));
    } finally {
        toggleLoader(false);
    }
}

async function createKey() {
    const name = document.getElementById('key-name').value.trim();
    const expiration = document.getElementById('expiration').value;
    if (!name) {
        showMessage('Введите имя профиля', 'warning');
        return;
    }
    toggleLoader(true);
    try {
        const resp = await fetch('/api/v1/vpn/keys', {
            method: 'POST',
            headers: headers(),
            body: JSON.stringify({name: name, expirationAt: expiration || null})
        });
        if (resp.ok) {
            document.getElementById('key-name').value = '';
            document.getElementById('expiration').value = '';
            showMessage('Профиль создан и зарегистрирован', 'success');
            await loadKeys();
        } else {
            showMessage('Не удалось создать профиль', 'danger');
        }
    } finally {
        toggleLoader(false);
    }
}

async function revoke(event) {
    const id = event.target.dataset.id;
    if (!confirm('Отозвать профиль? Клиент будет удалён из Xray.')) return;
    toggleLoader(true);
    try {
        await fetch(`/api/v1/vpn/keys/${id}`, {method: 'DELETE', headers: headers()});
        showMessage('Профиль отозван', 'info');
        await loadKeys();
    } finally {
        toggleLoader(false);
    }
}

async function downloadConfig(event) {
    const id = event.target.dataset.id;
    const resp = await fetch(`/api/v1/vpn/keys/${id}/download`, {method: 'POST', headers: headers()});
    if (!resp.ok) {
        showMessage('Не удалось скачать профиль', 'danger');
        return;
    }
    const text = await resp.text();
    const blob = new Blob([text], {type: 'text/plain'});
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `vless-${id}.txt`;
    link.click();
    showMessage('Профиль скачан', 'success');
}

async function loadAudit() {
    const table = document.querySelector('#audit-table tbody');
    if (!table) return;
    toggleLoader(true);
    try {
        const resp = await fetch('/api/v1/vpn/audit', {headers: headers()});
        if (!resp.ok) return;
        const data = await resp.json();
        table.innerHTML = '';
        data.forEach(a => {
            const row = document.createElement('tr');
            row.innerHTML = `<td>${a.userId}</td><td>${a.action}</td><td>${a.vpnKeyId ?? ''}</td><td>${formatDateTime(a.timestamp)}</td><td>${a.details ?? ''}</td>`;
            table.appendChild(row);
        });
    } finally {
        toggleLoader(false);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const createBtn = document.getElementById('create-key');
    if (createBtn) {
        createBtn.addEventListener('click', createKey);
        loadCurrentUser().then(loadKeys);
    }
    loadAudit();
});
