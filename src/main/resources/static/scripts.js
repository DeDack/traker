let statusOptions = [];
let editingEntry = null;
let currentEntries = [];
const timeCharts = { donut: null, bar: null };
let classicEntryModal = null;
const classicModalState = {
    elements: null
};

const TIME_INPUT_SELECTOR = 'input.time-input';

function enhanceTimeInput(input) {
    if (!input || input.dataset.enhancedTime === 'true') return;
    input.dataset.enhancedTime = 'true';
    input.setAttribute('step', '60');
    input.setAttribute('inputmode', 'none');
    const supportsPicker = typeof input.showPicker === 'function';
    if (supportsPicker) {
        input.readOnly = true;
        input.addEventListener('keydown', event => event.preventDefault());
        const openPicker = () => input.showPicker();
        input.addEventListener('focus', openPicker);
        input.addEventListener('click', openPicker);
    } else {
        input.removeAttribute('inputmode');
    }
}

function enhanceTimePickers(root = document) {
    root.querySelectorAll(TIME_INPUT_SELECTOR).forEach(enhanceTimeInput);
}

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
            window.location.href = '/new-tracker.html';
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
            const raw = await resp.json();
            statusOptions = normalizeStatusOrderList(raw);
            populateStatusSelect();
            displayStatuses();
        } else {
            showMessage('Ошибка загрузки статусов', 'danger');
        }
    } catch {
        showMessage('Ошибка соединения', 'danger');
    }
}

function normalizeStatusOrderList(statuses) {
    const sorted = statuses
        .map((status, idx) => ({
            ...status,
            order: Number.isFinite(Number(status.order)) ? Number(status.order) : idx
        }))
        .sort((a, b) => {
            const orderDiff = (a.order ?? 0) - (b.order ?? 0);
            if (orderDiff !== 0) return orderDiff;
            return a.name.localeCompare(b.name, 'ru', { sensitivity: 'base' });
        });
    return sorted.map((status, idx) => ({ ...status, order: idx }));
}

async function saveCurrentOrder(showNotification = true) {
    reindexLocalStatuses();
    for (const status of statusOptions) {
        const payload = { name: status.name, order: status.order };
        await apiFetch(`/api/statuses/updateStatus/${status.id}`, {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
    }
    if (showNotification) {
        showMessage('Порядок статусов сохранён', 'success');
    }
}

function reindexLocalStatuses() {
    statusOptions.forEach((status, idx) => {
        status.order = idx;
    });
}

function populateStatusSelect() {
    const select = document.getElementById('status');
    if (!select) return;
    select.innerHTML = '<option value="">—</option>' + statusOptions
        .map(s => `<option value="${s.id}">${escapeHtml(s.name)}</option>`)
        .join('');
    populateClassicStatusSelect();
    populateStatusFilter();
}

function populateStatusFilter() {
    const filter = document.getElementById('statusFilter');
    if (!filter) return;
    const previouslySelected = new Set(Array.from(filter.selectedOptions).map(o => Number(o.value)));
    filter.innerHTML = '';
    statusOptions.forEach(status => {
        const option = document.createElement('option');
        option.value = status.id;
        option.textContent = status.name;
        if (previouslySelected.size === 0 || previouslySelected.has(status.id)) {
            option.selected = true;
        }
        filter.appendChild(option);
    });
    const noneOption = document.createElement('option');
    noneOption.value = '-1';
    noneOption.textContent = 'Без статуса';
    if (previouslySelected.size === 0 || previouslySelected.has(-1)) {
        noneOption.selected = true;
    }
    filter.appendChild(noneOption);
    updateCharts();
}

function populateClassicStatusSelect(selectedId) {
    if (!classicModalState.elements || !classicModalState.elements.status) {
        return;
    }
    const statusSelect = classicModalState.elements.status;
    const currentValue = selectedId !== undefined ? String(selectedId ?? '') : statusSelect.value;
    statusSelect.innerHTML = '<option value="">—</option>' + statusOptions
        .map(s => `<option value="${s.id}">${escapeHtml(s.name)}</option>`)
        .join('');
    statusSelect.value = currentValue || '';
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
        row.addEventListener('drop', async e => {
            e.preventDefault();
            row.classList.remove('table-primary');
            const from = Number(e.dataTransfer.getData('text/plain'));
            const to = Number(row.dataset.index);
            if (Number.isNaN(from) || Number.isNaN(to) || from === to) {
                return;
            }
            const [moved] = statusOptions.splice(from, 1);
            statusOptions.splice(to, 0, moved);
            reindexLocalStatuses();
            displayStatuses();
            populateStatusSelect();
            await saveCurrentOrder(false);
        });

        tbody.appendChild(row);
    });
}

async function addStatus() {
    const name = document.getElementById('newStatusName')?.value.trim();
    if (!name) return;
    const nextOrder = statusOptions.length
        ? Math.max(...statusOptions.map(s => s.order ?? 0)) + 1
        : 0;
    const resp = await apiFetch('/api/statuses/createStatus', {
        method: 'POST',
        body: JSON.stringify({ name, order: nextOrder })
    });
    if (resp.ok) {
        document.getElementById('newStatusName').value = '';
        await fetchStatuses();
        await loadDayData();
    } else {
        showMessage('Ошибка добавления', 'danger');
    }
}

async function updateStatus(id) {
    const name = document.querySelector(`input[data-id="${id}"]`)?.value.trim();
    if (!name) return;
    const status = statusOptions.find(s => s.id === id);
    const order = status ? status.order : null;
    const resp = await apiFetch(`/api/statuses/updateStatus/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ name, order })
    });
    if (resp.ok) {
        await fetchStatuses();
        await loadDayData();
    } else {
        showMessage('Ошибка обновления', 'danger');
    }
}

async function deleteStatus(id) {
    const resp = await apiFetch(`/api/statuses/deleteStatus/${id}`, { method: 'DELETE' });
    if (resp.ok) {
        await fetchStatuses();
        await loadDayData();
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
        currentEntries = resp.ok ? await resp.json() : [];
    } catch {
        currentEntries = [];
    }
    editingEntry = null;
    renderHourlyEntries(currentEntries);
    renderTimeEntryCards(currentEntries);
    updateWorkedHours();
    updateCharts();
}


function formatTimeValue(hour, minute) {
    const h = Number(hour);
    const m = Number(minute);
    if (Number.isNaN(h) || Number.isNaN(m)) {
        return '--:--';
    }
    return `${pad(h)}:${pad(m)}`;
}

function defaultStartForHour(hour) {
    return `${pad(hour)}:00`;
}

function defaultEndForHour(hour) {
    return hour === 23 ? '23:59' : `${pad(hour + 1)}:00`;
}

function parseTimeInput(value) {
    if (!value) return null;
    const [hour, minute] = value.split(':').map(Number);
    if (Number.isNaN(hour) || Number.isNaN(minute)) return null;
    return { hour, minute };
}

function buildEntryPayload(startValue, endValue, worked, comment, statusId, overrideId) {
    const start = parseTimeInput(startValue);
    const end = parseTimeInput(endValue);

    if (!start || !end) {
        throw new Error('Укажите корректное время начала и окончания');
    }

    if (end.hour < start.hour || (end.hour === start.hour && end.minute <= start.minute)) {
        throw new Error('Время окончания должно быть позже времени начала');
    }

    const sanitizedComment = comment ? comment.trim() : '';
    const hasStatus = statusId !== undefined && statusId !== null && statusId !== '';
    const numericStatusId = hasStatus ? Number(statusId) : null;

    if (hasStatus && Number.isNaN(numericStatusId)) {
        throw new Error('Некорректный идентификатор статуса');
    }

    let resolvedId;
    if (overrideId !== undefined) {
        resolvedId = overrideId;
    } else {
        resolvedId = editingEntry && typeof editingEntry.id === 'number' ? editingEntry.id : null;
    }

    return {
        id: resolvedId,
        hour: start.hour,
        minute: start.minute,
        endHour: end.hour,
        endMinute: end.minute,
        worked: Boolean(worked),
        comment: sanitizedComment,
        status: numericStatusId !== null ? { id: numericStatusId } : null
    };
}

function renderHourlyEntries(entries) {
    const table = document.getElementById('timeEntriesTable');
    if (!table) return;
    table.innerHTML = '';
    const entriesByHour = new Map();
    entries.forEach(entry => {
        const bucket = Math.min(23, Math.floor((entry.hour * 60 + entry.minute) / 60));
        if (!entriesByHour.has(bucket)) {
            entriesByHour.set(bucket, entry);
        }
    });

    for (let hour = 0; hour < 24; hour++) {
        const entry = entriesByHour.get(hour) || null;
        const startValue = entry ? formatTimeValue(entry.hour, entry.minute) : defaultStartForHour(hour);
        const endValue = entry ? formatTimeValue(entry.endHour, entry.endMinute) : defaultEndForHour(hour);
        const row = document.createElement('tr');
        row.dataset.hour = hour;
        row.dataset.entryId = entry && entry.id != null ? entry.id : '';
        row.innerHTML = `
            <td>${pad(hour)}:00</td>
            <td><input type="time" class="form-control form-control-sm time-input" data-field="start" value="${startValue}" step="60"></td>
            <td><input type="time" class="form-control form-control-sm time-input" data-field="end" value="${endValue}" step="60"></td>
            <td class="text-center"><input type="checkbox" data-field="worked" ${entry && entry.worked ? 'checked' : ''}></td>
            <td><input type="text" class="form-control form-control-sm" data-field="comment" value="${escapeHtml(entry?.comment || '')}"></td>
            <td>
                <select class="form-select form-select-sm" data-field="status">
                    <option value=""></option>
                    ${statusOptions.map(s => `<option value="${s.id}" ${entry?.status && entry.status.id === s.id ? 'selected' : ''}>${escapeHtml(s.name)}</option>`).join('')}
                </select>
            </td>
            <td>
                <div class="d-flex gap-2">
                    <button class="btn btn-sm btn-success" onclick="saveTimeEntry(${hour})">💾</button>
                    <button class="btn btn-sm btn-danger" onclick="deleteTimeEntry(${entry && entry.id != null ? entry.id : 'null'})" ${entry ? '' : 'disabled'}>🗑️</button>
                </div>
            </td>
        `;
        table.appendChild(row);
        enhanceTimePickers(row);
    }
}

async function saveTimeEntry(hour) {
    const date = document.getElementById('datePicker')?.value;
    if (!date) return;
    const row = document.querySelector(`tr[data-hour="${hour}"]`);
    if (!row) return;

    const startInput = row.querySelector('input[data-field="start"]');
    const endInput = row.querySelector('input[data-field="end"]');
    const commentInput = row.querySelector('input[data-field="comment"]');
    const statusSelect = row.querySelector('select[data-field="status"]');
    const workedInput = row.querySelector('input[data-field="worked"]');

    const rawId = row.dataset.entryId;
    const overrideId = rawId ? Number(rawId) : null;
    if (rawId && Number.isNaN(overrideId)) {
        return showMessage('Некорректный идентификатор записи', 'danger');
    }

    let payload;
    try {
        payload = buildEntryPayload(
            startInput.value,
            endInput.value,
            workedInput.checked,
            commentInput.value,
            statusSelect.value,
            overrideId
        );
    } catch (error) {
        return showMessage(error.message, 'danger');
    }

    const resp = await apiFetch(`/api/days/${date}`, { method: 'PUT', body: JSON.stringify(payload) });
    if (resp.ok) {
        showMessage('Запись сохранена!', 'success');
        await loadDayData();
    } else {
        const message = await resp.text().catch(() => '');
        showMessage(message || 'Ошибка при сохранении', 'danger');
    }
}

async function deleteTimeEntry(id) {
    if (id === null || id === undefined || id === 'null' || id === '') {
        return showMessage('Запись не выбрана', 'danger');
    }
    const numericId = Number(id);
    if (Number.isNaN(numericId)) {
        return showMessage('Некорректный идентификатор записи', 'danger');
    }
    const resp = await apiFetch(`/api/days/entries/${numericId}`, { method: 'DELETE' });
    if (resp.ok) {
        showMessage('Запись удалена!', 'success');
        await loadDayData();
    } else {
        const message = await resp.text().catch(() => '');
        showMessage(message || 'Ошибка при удалении', 'danger');
    }
}

function renderTimeEntryCards(entries) {
    const list = document.getElementById('timeEntriesList');
    if (!list) return;
    list.innerHTML = '';
    entries
        .slice()
        .sort((a, b) => {
            const aMinutes = Number(a.hour) * 60 + Number(a.minute);
            const bMinutes = Number(b.hour) * 60 + Number(b.minute);
            const safeA = Number.isNaN(aMinutes) ? 0 : aMinutes;
            const safeB = Number.isNaN(bMinutes) ? 0 : bMinutes;
            return safeA - safeB;
        })
        .forEach(entry => {
            const start = formatTimeValue(entry.hour, entry.minute);
            const end = formatTimeValue(entry.endHour, entry.endMinute);
            const duration = formatDuration(computeEntryMinutes(entry));
            const card = document.createElement('div');
            card.className = 'col fade-in compact-card';
            card.innerHTML = `
                <div class="card h-100 shadow-sm">
                    <div class="card-body">
                        <h5 class="card-title">${start} — ${end}</h5>
                        <p class="card-text mb-1">${escapeHtml(entry.comment || 'Без комментария')}</p>
                        <p class="card-text mb-1"><small class="text-muted">Статус: ${entry.status ? escapeHtml(entry.status.name) : '—'}</small></p>
                        <p class="card-text"><small class="text-muted">Продолжительность: ${duration}${entry.worked ? ' • учтено' : ''}</small></p>
                    </div>
                    <div class="card-footer d-flex justify-content-end gap-2">
                        <button class="btn btn-primary btn-sm"
                            data-id="${entry.id}"
                            data-start="${start}"
                            data-end="${end}"
                            data-worked="${entry.worked}"
                            data-comment="${encodeURIComponent(entry.comment || '')}"
                            data-status-id="${entry.status ? entry.status.id : ''}"
                            onclick="editTimeEntry(this)">Редактировать</button>
                        <button class="btn btn-danger btn-sm" onclick="deleteTimeEntry(${entry.id})">Удалить</button>
                    </div>
                </div>
            `;
            list.appendChild(card);
        });
}

async function addTimeEntry() {
    const date = document.getElementById('datePicker')?.value;
    const startValue = document.getElementById('startTime')?.value;
    const endValue = document.getElementById('endTime')?.value;
    const worked = document.getElementById('worked')?.checked;
    const comment = document.getElementById('comment')?.value.trim();
    const statusId = document.getElementById('status')?.value;

    if (!date || !startValue || !endValue) {
        return showMessage('Заполните дату и время', 'danger');
    }

    let payload;
    try {
        payload = buildEntryPayload(startValue, endValue, worked, comment, statusId);
    } catch (error) {
        return showMessage(error.message, 'danger');
    }

    const resp = await apiFetch(`/api/days/${date}`, { method: 'PUT', body: JSON.stringify(payload) });
    if (resp.ok) {
        editingEntry = null;
        const startInput = document.getElementById('startTime');
        const endInput = document.getElementById('endTime');
        const workedInput = document.getElementById('worked');
        const commentInput = document.getElementById('comment');
        const statusSelect = document.getElementById('status');
        if (startInput) startInput.value = '';
        if (endInput) endInput.value = '';
        if (workedInput) workedInput.checked = false;
        if (commentInput) commentInput.value = '';
        if (statusSelect) statusSelect.value = '';
        await loadDayData();
        showMessage('Запись сохранена!', 'success');
    } else {
        const message = await resp.text().catch(() => '');
        showMessage(message || 'Ошибка при сохранении', 'danger');
    }
}

function getDefaultModalRange() {
    const now = new Date();
    const startMinutes = now.getHours() * 60 + now.getMinutes();
    const endMinutes = Math.min(startMinutes + 60, 23 * 60 + 59);
    const startHour = Math.floor(startMinutes / 60);
    const startMinute = startMinutes % 60;
    const endHour = Math.floor(endMinutes / 60);
    const endMinute = endMinutes % 60;
    return {
        start: formatTimeValue(startHour, startMinute),
        end: formatTimeValue(endHour, endMinute)
    };
}

function openClassicEntryModal(entry = null) {
    if (!classicEntryModal || !classicModalState.elements) return;
    const { start, end, worked, comment, status, title, form } = classicModalState.elements;
    if (!start || !end || !worked || !comment || !status) return;

    const defaultRange = getDefaultModalRange();
    start.value = entry ? entry.start : defaultRange.start;
    end.value = entry ? entry.end : defaultRange.end;
    worked.checked = entry ? Boolean(entry.worked) : false;
    comment.value = entry ? (entry.comment || '') : '';
    populateClassicStatusSelect(entry ? entry.statusId : undefined);
    status.value = entry && entry.statusId ? String(entry.statusId) : '';
    if (title) {
        title.textContent = entry ? 'Редактирование записи' : 'Новая запись';
    }
    if (form) {
        form.classList.remove('was-validated');
    }
    editingEntry = entry ? { id: entry.id } : null;
    classicEntryModal.show();
}

async function saveClassicEntry() {
    if (!classicModalState.elements) return;
    const date = document.getElementById('datePicker')?.value;
    if (!date) {
        showMessage('Выберите дату', 'danger');
        return;
    }

    const { start, end, worked, comment, status } = classicModalState.elements;
    if (!start || !end || !worked || !comment || !status) return;

    let payload;
    try {
        payload = buildEntryPayload(start.value, end.value, worked.checked, comment.value, status.value);
    } catch (error) {
        showMessage(error.message, 'danger');
        return;
    }

    const resp = await apiFetch(`/api/days/${date}`, { method: 'PUT', body: JSON.stringify(payload) });
    if (resp.ok) {
        classicEntryModal.hide();
        resetClassicModal();
        await loadDayData();
        showMessage('Запись сохранена!', 'success');
    } else {
        const message = await resp.text().catch(() => '');
        showMessage(message || 'Ошибка при сохранении', 'danger');
    }
}

function resetClassicModal() {
    if (!classicModalState.elements) return;
    const { start, end, worked, comment, status } = classicModalState.elements;
    if (start) start.value = '';
    if (end) end.value = '';
    if (worked) worked.checked = false;
    if (comment) comment.value = '';
    if (status) status.value = '';
    editingEntry = null;
}

function setupClassicModal() {
    const modalEl = document.getElementById('classicEntryModal');
    if (!modalEl || typeof bootstrap === 'undefined') {
        return;
    }
    classicModalState.elements = {
        modalEl,
        form: modalEl.querySelector('#classicEntryForm'),
        start: modalEl.querySelector('#classicStartTime'),
        end: modalEl.querySelector('#classicEndTime'),
        worked: modalEl.querySelector('#classicWorked'),
        comment: modalEl.querySelector('#classicComment'),
        status: modalEl.querySelector('#classicStatus'),
        title: modalEl.querySelector('#classicModalTitle')
    };
    classicEntryModal = bootstrap.Modal.getOrCreateInstance(modalEl);
    if (classicModalState.elements.form) {
        classicModalState.elements.form.addEventListener('submit', async (e) => {
            e.preventDefault();
            await saveClassicEntry();
        });
    }
    modalEl.addEventListener('hidden.bs.modal', resetClassicModal);
    const addButton = document.getElementById('openClassicModal');
    if (addButton) {
        addButton.addEventListener('click', () => openClassicEntryModal());
    }
}

function editTimeEntry(btn) {
    const { id, start, end, worked, comment, statusId } = btn.dataset;
    const numericId = Number(id);
    if (Number.isNaN(numericId)) {
        return showMessage('Некорректный идентификатор записи', 'danger');
    }

    const startInput = document.getElementById('startTime');
    const endInput = document.getElementById('endTime');
    const workedInput = document.getElementById('worked');
    const commentInput = document.getElementById('comment');
    const statusSelect = document.getElementById('status');

    if (startInput && endInput && workedInput && commentInput && statusSelect) {
        startInput.value = start;
        endInput.value = end;
        workedInput.checked = worked === 'true';
        commentInput.value = decodeURIComponent(comment || '');
        statusSelect.value = statusId || '';
        editingEntry = { id: numericId };
        return;
    }

    if (classicEntryModal && classicModalState.elements) {
        openClassicEntryModal({
            id: numericId,
            start,
            end,
            worked: worked === 'true',
            comment: decodeURIComponent(comment || ''),
            statusId: statusId || ''
        });
        return;
    }

    editingEntry = { id: numericId };
}

function computeEntryMinutes(entry) {
    const startHour = Number(entry.hour);
    const startMinute = Number(entry.minute);
    const endHour = Number(entry.endHour);
    const endMinute = Number(entry.endMinute);
    if ([startHour, startMinute, endHour, endMinute].some(value => Number.isNaN(value))) {
        return 0;
    }
    const start = startHour * 60 + startMinute;
    const end = endHour * 60 + endMinute;
    return Math.max(end - start, 0);
}

function formatDuration(minutes) {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours && mins) return `${hours} ч ${mins} мин`;
    if (hours) return `${hours} ч`;
    return `${mins} мин`;
}

function updateWorkedHours() {
    const label = document.getElementById('workedHours');
    if (!label) return;
    const totalMinutes = currentEntries.reduce((sum, entry) => sum + (entry.worked ? computeEntryMinutes(entry) : 0), 0);
    const hoursDecimal = (totalMinutes / 60).toFixed(2);
    label.textContent = `${formatDuration(totalMinutes)} (${hoursDecimal} ч)`;
}

function buildStatusAggregations(entries, filterSet) {
    const map = new Map();
    let total = 0;
    entries.forEach(entry => {
        const minutes = computeEntryMinutes(entry);
        if (minutes <= 0) return;
        const statusId = entry.status && entry.status.id != null ? Number(entry.status.id) : -1;
        if (filterSet.size && !filterSet.has(statusId)) return;
        const key = statusId;
        const label = entry.status && entry.status.name ? entry.status.name : 'Без статуса';
        if (map.has(key)) {
            map.get(key).minutes += minutes;
        } else {
            map.set(key, { id: key, label, minutes });
        }
        total += minutes;
    });
    return { total, rows: Array.from(map.values()) };
}

function setupCollapseToggle(buttonId, collapseId) {
    const toggle = document.getElementById(buttonId);
    const collapse = document.getElementById(collapseId);
    if (!toggle || !collapse) return;

    const expandedLabel = toggle.dataset.expandedLabel || 'Свернуть';
    const collapsedLabel = toggle.dataset.collapsedLabel || 'Развернуть';

    const updateLabel = () => {
        const expanded = collapse.classList.contains('show');
        toggle.textContent = expanded ? expandedLabel : collapsedLabel;
    };

    collapse.addEventListener('shown.bs.collapse', updateLabel);
    collapse.addEventListener('hidden.bs.collapse', updateLabel);
    updateLabel();
}

function setupTrackerCollapses() {
    setupCollapseToggle('analyticsToggle', 'analyticsCollapse');
    setupCollapseToggle('quickSlotsToggle', 'quickSlotsCollapse');
    setupCollapseToggle('statusToggle', 'statusCollapse');
}

function updateCharts() {
    const donutCanvas = document.getElementById('timeByStatusChart');
    const barCanvas = document.getElementById('timeByStatusBar');
    const legend = document.getElementById('statusLegend');
    const filter = document.getElementById('statusFilter');
    if (!donutCanvas || !barCanvas || !legend || typeof Chart === 'undefined') return;

    const filterSet = new Set();
    if (filter) {
        Array.from(filter.selectedOptions).forEach(opt => filterSet.add(Number(opt.value)));
    }

    const aggregated = buildStatusAggregations(currentEntries, filterSet);
    const chartPalette = ['#4c6ef5', '#15aabf', '#fab005', '#fa5252', '#5f3dc4', '#40c057', '#fd7e14', '#6f42c1', '#099268'];

    if (aggregated.total === 0) {
        legend.innerHTML = '<li class="list-group-item text-muted">Нет данных за выбранный период</li>';
        if (timeCharts.donut) { timeCharts.donut.destroy(); timeCharts.donut = null; }
        if (timeCharts.bar) { timeCharts.bar.destroy(); timeCharts.bar = null; }
        return;
    }

    const labels = aggregated.rows.map(row => row.label);
    const data = aggregated.rows.map(row => row.minutes);
    const colors = labels.map((_, idx) => chartPalette[idx % chartPalette.length]);

    if (timeCharts.donut) {
        timeCharts.donut.data.labels = labels;
        timeCharts.donut.data.datasets[0].data = data;
        timeCharts.donut.data.datasets[0].backgroundColor = colors;
        timeCharts.donut.update();
    } else {
        timeCharts.donut = new Chart(donutCanvas, {
            type: 'doughnut',
            data: { labels, datasets: [{ data, backgroundColor: colors }] },
            options: { plugins: { legend: { display: false } } }
        });
    }

    if (timeCharts.bar) {
        timeCharts.bar.data.labels = labels;
        timeCharts.bar.data.datasets[0].data = data;
        timeCharts.bar.data.datasets[0].backgroundColor = colors;
        timeCharts.bar.update();
    } else {
        timeCharts.bar = new Chart(barCanvas, {
            type: 'bar',
            data: { labels, datasets: [{ data, backgroundColor: colors }] },
            options: {
                plugins: { legend: { display: false } },
                scales: { y: { ticks: { callback: value => `${value} мин` } } }
            }
        });
    }

    legend.innerHTML = '';
    aggregated.rows.forEach((row, idx) => {
        const percent = ((row.minutes / aggregated.total) * 100).toFixed(1);
        const item = document.createElement('li');
        item.className = 'list-group-item d-flex justify-content-between align-items-center';
        item.innerHTML = `<span><span class="badge me-2" style="background-color:${colors[idx]};">&nbsp;</span>${escapeHtml(row.label)}</span><span class="fw-semibold">${formatDuration(row.minutes)} · ${percent}%</span>`;
        legend.appendChild(item);
    });
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
    enhanceTimePickers();

    const statusFilter = document.getElementById('statusFilter');
    if (statusFilter) {
        statusFilter.addEventListener('change', updateCharts);
    }

    if (path.endsWith('tracker.html')) {
        if (!(await isAuthenticated()) && !(await refreshAccessToken())) {
            return showMessage('Сессия истекла, пожалуйста, войдите снова', 'danger', '/login.html');
        }
        setupClassicModal();
        await fetchStatuses();
        await displayUsername();
        const datePicker = document.getElementById('datePicker');
        if (datePicker) {
            const today = new Date().toISOString().split('T')[0];
            datePicker.value = today;
            datePicker.addEventListener('change', async () => { await loadDayData(); });
            await loadDayData();
        }
        displayStatuses();
    } else if (path.endsWith('new-tracker.html')) {
        if (!(await isAuthenticated()) && !(await refreshAccessToken())) {
            return showMessage('Сессия истекла, пожалуйста, войдите снова', 'danger', '/login.html');
        }
        await fetchStatuses();
        await displayUsername();
        setupTrackerCollapses();
        const datePicker = document.getElementById('datePicker');
        if (datePicker) {
            const today = new Date().toISOString().split('T')[0];
            datePicker.value = today;
            datePicker.addEventListener('change', async () => { await loadDayData(); });
            await loadDayData();
        }
        displayStatuses();
    } else if (path.endsWith('index.html') || path === '/') {
        if (await isAuthenticated()) window.location.href = '/new-tracker.html';
    } else if (path.endsWith('login.html') || path.endsWith('register.html')) {
        if (await isAuthenticated()) window.location.href = '/new-tracker.html';
    } else if (path.endsWith('finance.html')) {
        if (typeof initFinancePage === 'function') {
            await initFinancePage();
        }
    }
});
