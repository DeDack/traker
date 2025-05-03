let statusOptions = [];

// Ключ для хранения порядка в localStorage
const STATUS_ORDER_KEY = 'statusOrder';

document.addEventListener('DOMContentLoaded', async () => {
    await fetchStatuses();
    applySavedOrder();

    const today = new Date().toISOString().split('T')[0];
    const datePicker = document.getElementById('datePicker');
    datePicker.value = today;
    datePicker.addEventListener('change', async () => {
        await loadDayData();
        await updateWorkedHours();
    });

    await loadDayData();
    await updateWorkedHours();
    displayStatuses();
});

function showMessage(message, type = 'info') {
    const msgDiv = document.getElementById('message');
    msgDiv.textContent = message;
    msgDiv.className = `alert alert-${type}`;
    setTimeout(() => msgDiv.textContent = '', 3000);
}

async function fetchStatuses() {
    try {
        const resp = await fetch('/api/statuses/getAllStatuses');
        if (resp.ok) {
            statusOptions = await resp.json();
            applySavedOrder();
        }
    } catch (e) {
        console.error('Ошибка при загрузке статусов', e);
    }
}

function applySavedOrder() {
    const saved = localStorage.getItem(STATUS_ORDER_KEY);
    if (!saved || !statusOptions.length) return;
    try {
        const order = JSON.parse(saved);
        statusOptions.sort((a, b) => {
            const ai = order.indexOf(a.id), bi = order.indexOf(b.id);
            if (ai === -1 && bi === -1) return 0;
            if (ai === -1) return 1;
            if (bi === -1) return -1;
            return ai - bi;
        });
    } catch (e) {
        console.error('Не удалось применить порядок', e);
    }
}

function saveCurrentOrder() {
    localStorage.setItem(STATUS_ORDER_KEY, JSON.stringify(statusOptions.map(s => s.id)));
}

async function loadDayData() {
    const date = document.getElementById('datePicker').value;
    if (!date) return;
    try {
        const resp = await fetch(`/api/days/${date}`);
        if (!resp.ok) throw new Error();
        const entries = await resp.json();
        renderTimeEntries(entries);
    } catch {
        showMessage('Ошибка загрузки данных дня!', 'danger');
    }
}

async function updateWorkedHours() {
    const date = document.getElementById('datePicker').value;
    if (!date) return;
    try {
        const resp = await fetch(`/api/stats/daily?date=${date}`);
        if (resp.ok) {
            const hours = await resp.json();
            document.getElementById('workedHours').textContent = hours;
        }
    } catch {
        console.error('Ошибка при загрузке статистики');
    }
}

function renderTimeEntries(timeEntries) {
    const tbody = document.getElementById('timeEntriesTable');
    tbody.innerHTML = '';
    for (let hour = 0; hour < 24; hour++) {
        const entry = timeEntries.find(e => e.hour === hour) || { hour, worked: false, comment: '', status: null };
        const row = document.createElement('tr');

        const select = document.createElement('select');
        select.className = 'form-select';
        select.dataset.hour = hour;
        select.append(new Option('—', ''));
        statusOptions.forEach(s => {
            const opt = new Option(s.name, s.name);
            if (entry.status && entry.status.name === s.name) opt.selected = true;
            select.append(opt);
        });

        row.innerHTML = `
            <td>${hour}:00</td>
            <td><input type="checkbox" ${entry.worked ? 'checked' : ''} data-hour="${hour}"></td>
            <td><input type="text" class="form-control" value="${entry.comment}" data-hour="${hour}"></td>
            <td></td>
            <td><button class="btn btn-success button" onclick="saveTimeEntry(${hour})">💾</button></td>
        `;
        row.children[3].appendChild(select);
        tbody.appendChild(row);
    }
}

async function saveTimeEntry(hour) {
    const date = document.getElementById('datePicker').value;
    if (!date) return;
    const worked = document.querySelector(`input[type="checkbox"][data-hour="${hour}"]`).checked;
    const comment = document.querySelector(`input[type="text"][data-hour="${hour}"]`).value;
    const statusName = document.querySelector(`select[data-hour="${hour}"]`).value;

    const payload = { hour, worked, comment, status: statusName ? { name: statusName } : null };

    try {
        const resp = await fetch(`/api/days/${date}`, {
            method: 'PUT',
            headers: { 'Content-Type':'application/json' },
            body: JSON.stringify(payload)
        });
        if (resp.ok) {
            showMessage(`Час ${hour}:00 сохранён!`, 'success');
            await updateWorkedHours();
        } else showMessage('Ошибка при сохранении', 'danger');
    } catch {
        showMessage('Ошибка соединения', 'danger');
    }
}

function displayStatuses() {
    const tbody = document.getElementById('statusTableBody');
    tbody.innerHTML = '';
    statusOptions.forEach((s, idx) => {
        const row = document.createElement('tr');
        row.draggable = true;
        row.dataset.index = idx;
        row.innerHTML = `
            <td class="drag-handle">☰</td>
            <td>${s.id}</td>
            <td><input type="text" class="form-control" value="${s.name}" data-id="${s.id}"></td>
            <td>
                <button class="btn btn-sm btn-success button" onclick="updateStatus(${s.id})">💾</button>
                <button class="btn btn-sm btn-danger button" onclick="deleteStatus(${s.id})">🗑️</button>
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
    const name = document.getElementById('newStatusName').value.trim();
    if (!name) return;
    try {
        const resp = await fetch('/api/statuses/createStatus', {
            method:'POST', headers:{'Content-Type':'application/json'},
            body: JSON.stringify({ name })
        });
        if (resp.ok) {
            document.getElementById('newStatusName').value = '';
            await fetchStatuses();
            displayStatuses();
            await loadDayData();
            await updateWorkedHours();
        } else showMessage('Ошибка добавления', 'danger');
    } catch {
        showMessage('Ошибка соединения', 'danger');
    }
}

async function updateStatus(id) {
    const name = document.querySelector(`input[data-id="${id}"]`).value.trim();
    if (!name) return;
    try {
        const resp = await fetch(`/api/statuses/updateStatus/${id}`, {
            method:'PUT', headers:{'Content-Type':'application/json'},
            body: JSON.stringify({ name })
        });
        if (resp.ok) {
            await fetchStatuses();
            displayStatuses();
            await loadDayData();
            await updateWorkedHours();
        } else showMessage('Ошибка обновления', 'danger');
    } catch {
        showMessage('Ошибка соединения', 'danger');
    }
}

async function deleteStatus(id) {
    try {
        const resp = await fetch(`/api/statuses/deleteStatus/${id}`, { method:'DELETE' });
        if (resp.ok) {
            await fetchStatuses();
            displayStatuses();
            await loadDayData();
            await updateWorkedHours();
        } else showMessage('Ошибка удаления', 'danger');
    } catch {
        showMessage('Ошибка соединения', 'danger');
    }
}
