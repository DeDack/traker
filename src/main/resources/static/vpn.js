const token = localStorage.getItem('token');

function headers() {
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };
}

async function loadKeys() {
    const resp = await fetch('/api/vpn/keys/list?all=true', {headers: headers()});
    if (!resp.ok) return;
    const data = await resp.json();
    const body = document.querySelector('#keys-table tbody');
    if (!body) return;
    body.innerHTML = '';
    data.forEach(k => {
        const row = document.createElement('tr');
        row.innerHTML = `<td>${k.name}</td><td>${k.address}</td><td>${k.status}</td><td>${k.expirationAt ?? ''}</td><td>${k.ownerUserId}</td>
            <td><button data-id="${k.id}" class="download">Download</button><button data-id="${k.id}" class="revoke">Revoke</button></td>`;
        body.appendChild(row);
    });
    body.querySelectorAll('button.download').forEach(btn => btn.addEventListener('click', downloadConfig));
    body.querySelectorAll('button.revoke').forEach(btn => btn.addEventListener('click', revoke));
}

async function createKey() {
    const name = document.getElementById('key-name').value;
    const expiration = document.getElementById('expiration').value;
    await fetch('/api/vpn/keys/create', {
        method: 'POST',
        headers: headers(),
        body: JSON.stringify({name: name, expirationAt: expiration || null})
    });
    await loadKeys();
}

async function revoke(event) {
    const id = event.target.dataset.id;
    await fetch(`/api/vpn/keys/delete/${id}?admin=true`, {method: 'DELETE', headers: headers()});
    await loadKeys();
}

async function downloadConfig(event) {
    const id = event.target.dataset.id;
    const resp = await fetch(`/api/vpn/keys/download-config/${id}?admin=true`, {headers: headers()});
    const text = await resp.text();
    const blob = new Blob([text], {type: 'text/plain'});
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `wg-${id}.conf`;
    link.click();
}

async function loadAudit() {
    const table = document.querySelector('#audit-table tbody');
    if (!table) return;
    const resp = await fetch('/api/vpn/audit/list', {headers: headers()});
    if (!resp.ok) return;
    const data = await resp.json();
    table.innerHTML = '';
    data.forEach(a => {
        const row = document.createElement('tr');
        row.innerHTML = `<td>${a.userId}</td><td>${a.action}</td><td>${a.vpnKeyId ?? ''}</td><td>${a.timestamp}</td><td>${a.details ?? ''}</td>`;
        table.appendChild(row);
    });
}

document.addEventListener('DOMContentLoaded', () => {
    const createBtn = document.getElementById('create-key');
    if (createBtn) {
        createBtn.addEventListener('click', createKey);
        loadKeys();
    }
    loadAudit();
});
