let statusOptions = [];
const STATUS_ORDER_KEY = 'statusOrder';
let editingEntry = null;

function pad(num) {
    return String(num).padStart(2, '0');
}

function escapeHtml(str) {
    return str.replace(/[&<>"']/g, c => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
}

function showMessage(message, type = 'info', redirectUrl = null) {
    const msgDiv = document.getElementById('message');
    if (msgDiv) {
        msgDiv.textContent = message;
        msgDiv.className = `alert alert-${type}`;
        if (redirectUrl) {
            setTimeout(() => { window.location.href = redirectUrl; }, 2000);
        } else {
            setTimeout(() => { msgDiv.textContent = ''; msgDiv.className = ''; }, 3000);
        }
    } else if (redirectUrl) {
        window.location.href = redirectUrl;
    }
}

function toggleLoader(show) {
    const loader = document.getElementById('loader');
    if (loader) loader.style.display = show ? 'block' : 'none';
}

async function isAuthenticated() {
    try {
        const resp = await fetch('/api/users/me', { credentials: 'include' });
        return resp.ok;
    } catch {
        return false;
    }
}

async function refreshAccessToken() {
    toggleLoader(true);
    try {
        const resp = await fetch('/api/auth/refresh', { method: 'POST', credentials: 'include' });
        return resp.ok;
    } catch {
        return false;
    } finally {
        toggleLoader(false);
    }
}

async function apiFetch(url, options = {}, retries = 2) {
    for (let i = 0; i <= retries; i++) {
        try {
            const resp = await fetch(url, {
                ...options,
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                    ...(options.headers || {})
                }
            });
            if (resp.status === 401 || resp.status === 403) {
                if (await refreshAccessToken()) {
                    return apiFetch(url, options, retries - i - 1);
                } else {
                    showMessage('Сессия истекла, пожалуйста, войдите снова', 'danger', '/login.html');
                    throw new Error('Authentication failed');
                }
            }
            return resp;
        } catch (e) {
            if (i === retries) {
                showMessage('Ошибка соединения, перенаправление на страницу входа', 'danger', '/login.html');
                throw e;
            }
            await new Promise(r => setTimeout(r, 1000));
        }
    }
}

async function displayUsername() {
    try {
        const resp = await apiFetch('/api/users/me');
        if (resp.ok) {
            const user = await resp.json();
            const el = document.getElementById('username');
            if (el) el.textContent = user.name || user.username || 'Пользователь';
        }
    } catch {}
}

async function register() {
    const name = document.getElementById('name')?.value.trim();
    const username = document.getElementById('username')?.value.trim();
    const password = document.getElementById('password')?.value.trim();
    if (!name || !username || !password) return showMessage('Заполните все поля', 'danger');
    if (username.length < 3) return showMessage('Имя пользователя должно быть не короче 3 символов', 'danger');
    if (password.length < 6) return showMessage('Пароль должен быть не короче 6 символов', 'danger');
    if (!/^[a-zA-Zа-яА-Я\s]+$/.test(name)) return showMessage('Имя должно содержать только буквы и пробелы', 'danger');

    try {
        const resp = await fetch('/api/users/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, username, password }),
            credentials: 'include'
        });
        if (resp.ok) {
            showMessage('Регистрация успешна! Войдите в систему.', 'success', '/login.html');
        } else {
            showMessage(await resp.text() || 'Ошибка регистрации', 'danger');
        }
    } catch {
        showMessage('Ошибка соединения', 'danger');
    }
}

async function login() {
    const username = document.getElementById('username')?.value.trim();
    const password = document.getElementById('password')?.value.trim();
    if (!username || !password) return showMessage('Заполните все поля', 'danger');

    try {
        const resp = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password }),
            credentials: 'include'
        });
        if (resp.ok) {
            window.location.href = '/tracker.html';
        } else {
            showMessage(await resp.text() || 'Ошибка входа', 'danger');
        }
    } catch {
        showMessage('Ошибка соединения', 'danger');
    }
}

async function logout() {
    try { await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' }); }
    finally { window.location.href = '/index.html'; }
}

// --------- Statuses ---------
async function fetchStatuses() {
    try {
        const resp = await apiFetch('/api/statuses/getAllStatuses');
        if (resp.ok) {
            statusOptions = await resp.json();
            applySavedOrder();
            populateStatusSelect();
        } else {
            showMessage('Ошибка загрузки статусов', 'danger');
        }
    } catch {
        showMessage('Ошибка соединения', 'danger');
    }
}

function applySavedOrder() {
    const saved = localStorage.getItem(STATUS_ORDER_KEY);
    if (!saved) return;
    try {
        const order = JSON.parse(saved).filter(id => statusOptions.some(s => s.id === id));
        statusOptions.sort((a, b) => order.indexOf(a.id) - order.indexOf(b.id));
    } catch {}
}

async function saveCurrentOrder() {
    localStorage.setItem(STATUS_ORDER_KEY, JSON.stringify(statusOptions.map(s => s.id)));
    for (let i = 0; i < statusOptions.length; i++) {
        const payload = { name: statusOptions[i].name, order: i };
        await apiFetch(`/api/statuses/updateStatus/${statusOptions[i].id}`, {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
    }
}

function populateStatusSelect() {
    const select = document.getElementById('status');
    if (!select) return;
    select.innerHTML = '<option value="">—</option>' + statusOptions.map(s => `<option value="${escapeHtml(s.name)}">${escapeHtml(s.name)}</option>`).join('');
}

function displayStatuses() {
    const tbody = document.getElementById('statusTableBody');
    if (!tbody) return;
    tbody.innerHTML = '';
    statusOptions.forEach((s, idx) => {
        const row = document.createElement('tr');
        row.draggable = true;
        row.dataset.index = idx;

        row.innerHTML = `
            <td class="drag-handle">☰</td>
            <td>${s.id}</td>
            <td><input type="text" class="form-control" value="${escapeHtml(s.name)}" data-id="${s.id}"></td>
            <td>
                <button class="btn btn-sm btn-success" onclick="updateStatus(${s.id})">💾</button>
                <button class="btn btn-sm btn-danger" onclick="deleteStatus(${s.id})">🗑️</button>
            </td>
        `;

        row.addEventListener('dragstart', e => e.dataTransfer.setData('text/plain', idx));
        row.addEventListener('dragover', e => { e.preventDefault(); row.classList.add('table-primary'); });
        row.addEventListener('dragleave', () => row.classList.remove('table-primary'));
        row.addEventListener('drop', e => {
            e.preventDefault();
            row.classList.remove('table-primary');
            const from = +e.dataTransfer.getData('text/plain');
            const to = +row.dataset.index;
            [statusOptions[from], statusOptions[to]] = [statusOptions[to], statusOptions[from]];
            saveCurrentOrder();
            displayStatuses();
        });

        tbody.appendChild(row);
    });
}

async function addStatus() {
    const name = document.getElementById('newStatusName')?.value.trim();
    if (!name) return;
    const resp = await apiFetch('/api/statuses/createStatus', {
        method: 'POST',
        body: JSON.stringify({ name })
    });
    if (resp.ok) {
        document.getElementById('newStatusName').value = '';
        await fetchStatuses();
        displayStatuses();
        await loadDayData();
        await updateWorkedHours();
    } else {
        showMessage('Ошибка добавления', 'danger');
    }
}

async function updateStatus(id) {
    const name = document.querySelector(`input[data-id="${id}"]`)?.value.trim();
    if (!name) return;
    const resp = await apiFetch(`/api/statuses/updateStatus/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ name })
    });
    if (resp.ok) {
        await fetchStatuses();
        displayStatuses();
        await loadDayData();
        await updateWorkedHours();
    } else {
        showMessage('Ошибка обновления', 'danger');
    }
}

async function deleteStatus(id) {
    const resp = await apiFetch(`/api/statuses/deleteStatus/${id}`, { method: 'DELETE' });
    if (resp.ok) {
        await fetchStatuses();
        displayStatuses();
        await loadDayData();
        await updateWorkedHours();
    } else {
        showMessage('Ошибка удаления', 'danger');
    }
}

// --------- Hour-based tracker ---------
async function loadDayData() {
    const date = document.getElementById('datePicker')?.value;
    if (!date) return;
    try {
        const resp = await apiFetch(`/api/days/${date}`);
        const entries = resp.ok ? await resp.json() : [];
        renderHourlyEntries(entries);
    } catch {
        renderHourlyEntries([]);
    }
}

function renderHourlyEntries(entries) {
    const table = document.getElementById('timeEntriesTable');
    if (!table) return;
    table.innerHTML = '';
    for (let hour = 0; hour < 24; hour++) {
        const entry = entries.find(e => e.hour === hour && e.minute === 0) || {};
        const comment = escapeHtml(entry.comment || '');
        const statusName = entry.status ? entry.status.name : '';
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${hour}:00</td>
            <td><input type="checkbox" data-hour="${hour}" ${entry.worked ? 'checked' : ''}></td>
            <td><input type="text" data-hour="${hour}" value="${comment}"></td>
            <td>
                <select data-hour="${hour}">
                    <option value=""></option>
                    ${statusOptions.map(s => `<option value="${escapeHtml(s.name)}" ${statusName === s.name ? 'selected' : ''}>${escapeHtml(s.name)}</option>`).join('')}
                </select>
            </td>
            <td>
                <button class="btn btn-sm btn-success" onclick="saveTimeEntry(${hour})">💾</button>
                <button class="btn btn-sm btn-danger" onclick="deleteTimeEntry(${hour})">🗑️</button>
            </td>
        `;
        table.appendChild(row);
    }
}

async function saveTimeEntry(hour) {
    const date = document.getElementById('datePicker')?.value;
    if (!date) return;
    const worked = document.querySelector(`input[type="checkbox"][data-hour="${hour}"]`).checked;
    const comment = document.querySelector(`input[type="text"][data-hour="${hour}"]`).value;
    const statusName = document.querySelector(`select[data-hour="${hour}"]`).value;
    const payload = { hour, minute: 0, worked, comment, status: statusName ? { name: statusName } : null };
    const resp = await apiFetch(`/api/days/${date}`, { method: 'PUT', body: JSON.stringify(payload) });
    if (resp.ok) {
        showMessage(`Час ${hour}:00 сохранён!`, 'success');
        await updateWorkedHours();
    } else {
        showMessage('Ошибка при сохранении', 'danger');
    }
}

async function deleteTimeEntry(hour, minute = 0) {
    const date = document.getElementById('datePicker')?.value;
    if (!date) return;
    const resp = await apiFetch(`/api/days/${date}/${hour}/${minute}`, { method: 'DELETE' });
    if (resp.ok) {
        showMessage('Запись удалена!', 'success');
        await loadDayData();
        await updateWorkedHours();
    } else {
        showMessage('Ошибка при удалении', 'danger');
    }
}

// --------- Minute-based tracker ---------
async function loadNewDayData() {
    const date = document.getElementById('datePicker')?.value;
    if (!date) return;
    try {
        const resp = await apiFetch(`/api/days/${date}`);
        const entries = resp.ok ? await resp.json() : [];
        renderNewTimeEntries(entries);
    } catch {
        renderNewTimeEntries([]);
    }
}

function parseInterval(comment) {
    if (!comment) return { interval: '', text: '' };
    const m = comment.match(/^(\d{2}:\d{2}-\d{2}:\d{2})(?:\s*:\s*(.*))?$/);
    if (m) return { interval: m[1], text: m[2] || '' };
    return { interval: '', text: comment };
}

function renderNewTimeEntries(entries) {
    const list = document.getElementById('timeEntriesList');
    if (!list) return;
    list.innerHTML = '';
    entries.sort((a,b)=>(a.hour*60+a.minute)-(b.hour*60+b.minute))
        .forEach(entry => {
            const { interval, text } = parseInterval(entry.comment);
            const card = document.createElement('div');
            card.className = 'col fade-in compact-card';
            card.innerHTML = `
                <div class="card h-100">
                    <div class="card-body">
                        <h5 class="card-title">${interval || `${pad(entry.hour)}:${pad(entry.minute)}-${pad(entry.hour+1)}:${pad(entry.minute)}`}</h5>
                        <p class="card-text">${escapeHtml(text)}</p>
                        <p class="card-text"><small class="text-muted">Статус: ${entry.status ? escapeHtml(entry.status.name) : '—'}</small></p>
                        <p class="card-text"><small class="text-muted">Отработано: ${entry.worked ? 'Да' : 'Нет'}</small></p>
                    </div>
                    <div class="card-footer d-flex justify-content-end">
                        <button class="btn btn-primary btn-sm me-2"
                            data-hour="${entry.hour}"
                            data-minute="${entry.minute}"
                            data-interval="${encodeURIComponent(interval)}"
                            data-worked="${entry.worked}"
                            data-comment="${encodeURIComponent(text)}"
                            data-status="${entry.status ? encodeURIComponent(entry.status.name) : ''}"
                            onclick="editTimeEntry(this)">Редактировать</button>
                        <button class="btn btn-danger btn-sm" onclick="deleteTimeEntry(${entry.hour}, ${entry.minute})">Удалить</button>
                    </div>
                </div>
            `;
            list.appendChild(card);
        });
}

async function addTimeEntry() {
    const date = document.getElementById('datePicker')?.value;
    const startTime = document.getElementById('startTime')?.value;
    const endTime = document.getElementById('endTime')?.value;
    const worked = document.getElementById('worked')?.checked;
    const comment = document.getElementById('comment')?.value.trim();
    const statusName = document.getElementById('status')?.value;

    if (!date || !startTime || !endTime) return showMessage('Заполните дату и время', 'danger');

    const [startHour, startMinute] = startTime.split(':').map(Number);
    const [endHour, endMinute] = endTime.split(':').map(Number);
    if (endHour < startHour || (endHour === startHour && endMinute <= startMinute)) {
        return showMessage('Время окончания должно быть позже начала', 'danger');
    }

    const interval = `${startTime}-${endTime}`;
    const commentPayload = comment ? `${interval}: ${comment}` : interval;
    const payload = {
        hour: startHour,
        minute: startMinute,
        worked,
        comment: commentPayload,
        status: statusName ? { name: statusName } : null
    };

    const resp = await apiFetch(`/api/days/${date}`, { method: 'PUT', body: JSON.stringify(payload) });
    if (resp.ok) {
        if (editingEntry && (editingEntry.hour !== startHour || editingEntry.minute !== startMinute)) {
            await apiFetch(`/api/days/${date}/${editingEntry.hour}/${editingEntry.minute}`, { method: 'DELETE' }).catch(()=>{});
        }
        editingEntry = null;
        document.getElementById('startTime').value = '';
        document.getElementById('endTime').value = '';
        document.getElementById('worked').checked = false;
        document.getElementById('comment').value = '';
        document.getElementById('status').value = '';
        await loadNewDayData();
        await updateWorkedHours();
        showMessage('Запись сохранена!', 'success');
    } else {
        showMessage('Ошибка при сохранении', 'danger');
    }
}
function editTimeEntry(btn) {
    const { hour, minute, interval, worked, comment, status } = btn.dataset;
    document.getElementById('startTime').value = decodeURIComponent(interval).split('-')[0];
    document.getElementById('endTime').value = decodeURIComponent(interval).split('-')[1];
    document.getElementById('worked').checked = worked === 'true';
    document.getElementById('comment').value = decodeURIComponent(comment || '');
    document.getElementById('status').value = decodeURIComponent(status || '');
    editingEntry = { hour: parseInt(hour, 10), minute: parseInt(minute, 10) };
}

function computeEntryMinutes(entry) {
    const { interval } = parseInterval(entry.comment);
    if (interval) {
        const [s, e] = interval.split('-');
        const [sh, sm] = s.split(':').map(Number);
        const [eh, em] = e.split(':').map(Number);
        return (eh * 60 + em) - (sh * 60 + sm);
    }
    return entry.worked ? 60 : 0;
}

async function updateWorkedHours() {
    const date = document.getElementById('datePicker')?.value;
    if (!date) return;
    try {
        const resp = await apiFetch(`/api/days/${date}`);
        const entries = resp.ok ? await resp.json() : [];
        const totalMinutes = entries.reduce((sum, e) => sum + computeEntryMinutes(e), 0);
        document.getElementById('workedHours').textContent = (totalMinutes / 60).toFixed(2);
    } catch {
        document.getElementById('workedHours').textContent = '0';
    }
}

// --------- Common ---------
function startTokenRefreshTimer() {
    setInterval(async () => {
        if (await isAuthenticated()) await refreshAccessToken();
    }, 55 * 60 * 1000);
}

document.addEventListener('DOMContentLoaded', async () => {
    const path = window.location.pathname;
    startTokenRefreshTimer();

    if (path.endsWith('tracker.html')) {
        if (!(await isAuthenticated()) && !(await refreshAccessToken())) {
            return showMessage('Сессия истекла, пожалуйста, войдите снова', 'danger', '/login.html');
        }
        await fetchStatuses();
        await displayUsername();
        const datePicker = document.getElementById('datePicker');
        if (datePicker) {
            const today = new Date().toISOString().split('T')[0];
            datePicker.value = today;
            datePicker.addEventListener('change', async () => { await loadDayData(); await updateWorkedHours(); });
            await loadDayData();
            await updateWorkedHours();
        }
        displayStatuses();
    } else if (path.endsWith('new-tracker.html')) {
        if (!(await isAuthenticated()) && !(await refreshAccessToken())) {
            return showMessage('Сессия истекла, пожалуйста, войдите снова', 'danger', '/login.html');
        }
        await fetchStatuses();
        await displayUsername();
        const datePicker = document.getElementById('datePicker');
        if (datePicker) {
            const today = new Date().toISOString().split('T')[0];
            datePicker.value = today;
            datePicker.addEventListener('change', async () => { await loadNewDayData(); await updateWorkedHours(); });
            await loadNewDayData();
            await updateWorkedHours();
        }
    } else if (path.endsWith('index.html') || path === '/') {
        if (await isAuthenticated()) window.location.href = '/tracker.html';
    } else if (path.endsWith('login.html') || path.endsWith('register.html')) {
        if (await isAuthenticated()) window.location.href = '/tracker.html';
    }
});
