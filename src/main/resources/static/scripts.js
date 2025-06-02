let statusOptions = [];

// Ключ для хранения порядка в localStorage
const STATUS_ORDER_KEY = 'statusOrder';
const TOKEN_KEY = 'jwtToken';

// Проверка авторизации
function isAuthenticated() {
    const token = localStorage.getItem(TOKEN_KEY);
    console.log('Checking authentication, token exists:', !!token);
    return !!token;
}

// Получение токена
function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

// Установка токена
function setToken(token) {
    console.log('Setting token:', token);
    localStorage.setItem(TOKEN_KEY, token);
}

// Удаление токена
function removeToken() {
    console.log('Removing token');
    localStorage.removeItem(TOKEN_KEY);
}

// Показ сообщения
function showMessage(message, type = 'info') {
    console.log('Showing message:', message, 'Type:', type);
    const msgDiv = document.getElementById('message');
    if (msgDiv) {
        msgDiv.textContent = message;
        msgDiv.className = `alert alert-${type}`;
        setTimeout(() => {
            msgDiv.textContent = '';
            msgDiv.className = '';
        }, 3000);
    }
}

// Регистрация
async function register() {
    console.log('Register function called');
    const name = document.getElementById('name')?.value.trim();
    const username = document.getElementById('username')?.value.trim();
    const password = document.getElementById('password')?.value.trim();

    if (!name || !username || !password) {
        showMessage('Заполните все поля', 'danger');
        return;
    }

    const payload = { name, username, password };
    console.log('Sending register request with payload:', payload);

    try {
        const resp = await fetch('/api/users/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        console.log('Register response status:', resp.status);

        if (resp.ok) {
            showMessage('Регистрация успешна! Войдите в систему.', 'success');
            setTimeout(() => window.location.href = 'login.html', 2000);
        } else {
            const error = await resp.text();
            showMessage(error || 'Ошибка регистрации', 'danger');
        }
    } catch (error) {
        console.error('Register error:', error);
        showMessage('Ошибка соединения', 'danger');
    }
}

// Вход
async function login() {
    console.log('Login function called');
    const username = document.getElementById('username')?.value.trim();
    const password = document.getElementById('password')?.value.trim();

    if (!username || !password) {
        showMessage('Заполните все поля', 'danger');
        return;
    }

    const payload = { username, password };
    console.log('Sending login request with payload:', payload);

    try {
        const resp = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        console.log('Login response status:', resp.status);

        if (resp.ok) {
            const data = await resp.json();
            console.log('Received token:', data.token);
            setToken(data.token);
            window.location.href = 'tracker.html';
        } else {
            const error = await resp.text();
            showMessage(error || 'Ошибка входа', 'danger');
        }
    } catch (error) {
        console.error('Login error:', error);
        showMessage('Ошибка соединения', 'danger');
    }
}

// Выход
function logout() {
    console.log('Logout function called');
    removeToken();
    window.location.href = 'index.html';
}

// Загрузка статусов
async function fetchStatuses() {
    if (!isAuthenticated()) {
        console.log('Not authenticated, redirecting to login');
        window.location.href = 'login.html';
        return;
    }
    try {
        console.log('Fetching statuses');
        const resp = await fetch('/api/statuses/getAllStatuses', {
            headers: { 'Authorization': `Bearer ${getToken()}` }
        });
        console.log('Fetch statuses response status:', resp.status);
        if (resp.ok) {
            statusOptions = await resp.json();
            console.log('Statuses loaded:', statusOptions);
            applySavedOrder();
        } else if (resp.status === 401) {
            console.log('Unauthorized, logging out');
            logout();
        } else {
            showMessage('Ошибка загрузки статусов', 'danger');
        }
    } catch (e) {
        console.error('Error fetching statuses:', e);
        showMessage('Ошибка соединения', 'danger');
    }
}

// Применение сохраненного порядка статусов
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
        console.error('Failed to apply saved order:', e);
    }
}

// Сохранение порядка статусов
function saveCurrentOrder() {
    localStorage.setItem(STATUS_ORDER_KEY, JSON.stringify(statusOptions.map(s => s.id)));
}

// Загрузка данных дня
async function loadDayData() {
    if (!isAuthenticated()) {
        console.log('Not authenticated, redirecting to login');
        window.location.href = 'login.html';
        return;
    }
    const date = document.getElementById('datePicker')?.value;
    if (!date) return;
    try {
        console.log('Loading day data for date:', date);
        const resp = await fetch(`/api/days/${date}`, {
            headers: { 'Authorization': `Bearer ${getToken()}` }
        });
        console.log('Load day data response status:', resp.status);
        if (resp.ok) {
            const entries = await resp.json();
            renderTimeEntries(entries);
        } else if (resp.status === 401) {
            console.log('Unauthorized, logging out');
            logout();
        } else {
            showMessage('Ошибка загрузки данных дня!', 'danger');
        }
    } catch (e) {
        console.error('Error loading day data:', e);
        showMessage('Ошибка соединения', 'danger');
    }
}

// Обновление отработанных часов
async function updateWorkedHours() {
    if (!isAuthenticated()) {
        console.log('Not authenticated, redirecting to login');
        window.location.href = 'login.html';
        return;
    }
    const date = document.getElementById('datePicker')?.value;
    if (!date) return;
    try {
        console.log('Updating worked hours for date:', date);
        const resp = await fetch(`/api/stats/daily?date=${date}`, {
            headers: { 'Authorization': `Bearer ${getToken()}` }
        });
        console.log('Update worked hours response status:', resp.status);
        if (resp.ok) {
            const hours = await resp.json();
            document.getElementById('workedHours').textContent = hours;
        } else if (resp.status === 401) {
            console.log('Unauthorized, logging out');
            logout();
        }
    } catch (e) {
        console.error('Error updating worked hours:', e);
    }
}

// Отрисовка записей времени
function renderTimeEntries(timeEntries) {
    const tbody = document.getElementById('timeEntriesTable');
    if (!tbody) return;
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

// Сохранение записи времени
async function saveTimeEntry(hour) {
    if (!isAuthenticated()) {
        console.log('Not authenticated, redirecting to login');
        window.location.href = 'login.html';
        return;
    }
    const date = document.getElementById('datePicker')?.value;
    if (!date) return;
    const worked = document.querySelector(`input[type="checkbox"][data-hour="${hour}"]`)?.checked;
    const comment = document.querySelector(`input[type="text"][data-hour="${hour}"]`)?.value;
    const statusName = document.querySelector(`select[data-hour="${hour}"]`)?.value;

    const payload = { hour, worked, comment, status: statusName ? { name: statusName } : null };
    console.log('Saving time entry with payload:', payload);

    try {
        const resp = await fetch(`/api/days/${date}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getToken()}`
            },
            body: JSON.stringify(payload)
        });
        console.log('Save time entry response status:', resp.status);
        if (resp.ok) {
            showMessage(`Час ${hour}:00 сохранён!`, 'success');
            await updateWorkedHours();
        } else if (resp.status === 401) {
            console.log('Unauthorized, logging out');
            logout();
        } else {
            showMessage('Ошибка при сохранении', 'danger');
        }
    } catch (e) {
        console.error('Error saving time entry:', e);
        showMessage('Ошибка соединения', 'danger');
    }
}

// Отображение статусов
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

// Добавление статуса
async function addStatus() {
    if (!isAuthenticated()) {
        console.log('Not authenticated, redirecting to login');
        window.location.href = 'login.html';
        return;
    }
    const name = document.getElementById('newStatusName')?.value.trim();
    if (!name) return;
    const payload = { name };
    console.log('Adding status with payload:', payload);

    try {
        const resp = await fetch('/api/statuses/createStatus', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getToken()}`
            },
            body: JSON.stringify(payload)
        });
        console.log('Add status response status:', resp.status);
        if (resp.ok) {
            document.getElementById('newStatusName').value = '';
            await fetchStatuses();
            displayStatuses();
            await loadDayData();
            await updateWorkedHours();
        } else if (resp.status === 401) {
            console.log('Unauthorized, logging out');
            logout();
        } else {
            showMessage('Ошибка добавления', 'danger');
        }
    } catch (e) {
        console.error('Error adding status:', e);
        showMessage('Ошибка соединения', 'danger');
    }
}

// Обновление статуса
async function updateStatus(id) {
    if (!isAuthenticated()) {
        console.log('Not authenticated, redirecting to login');
        window.location.href = 'login.html';
        return;
    }
    const name = document.querySelector(`input[data-id="${id}"]`)?.value.trim();
    if (!name) return;
    const payload = { name };
    console.log('Updating status with payload:', payload);

    try {
        const resp = await fetch(`/api/statuses/updateStatus/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getToken()}`
            },
            body: JSON.stringify(payload)
        });
        console.log('Update status response status:', resp.status);
        if (resp.ok) {
            await fetchStatuses();
            displayStatuses();
            await loadDayData();
            await updateWorkedHours();
        } else if (resp.status === 401) {
            console.log('Unauthorized, logging out');
            logout();
        } else {
            showMessage('Ошибка обновления', 'danger');
        }
    } catch (e) {
        console.error('Error updating status:', e);
        showMessage('Ошибка соединения', 'danger');
    }
}

// Удаление статуса
async function deleteStatus(id) {
    if (!isAuthenticated()) {
        console.log('Not authenticated, redirecting to login');
        window.location.href = 'login.html';
        return;
    }
    console.log('Deleting status with id:', id);

    try {
        const resp = await fetch(`/api/statuses/deleteStatus/${id}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${getToken()}` }
        });
        console.log('Delete status response status:', resp.status);
        if (resp.ok) {
            await fetchStatuses();
            displayStatuses();
            await loadDayData();
            await updateWorkedHours();
        } else if (resp.status === 401) {
            console.log('Unauthorized, logging out');
            logout();
        } else {
            showMessage('Ошибка удаления', 'danger');
        }
    } catch (e) {
        console.error('Error deleting status:', e);
        showMessage('Ошибка соединения', 'danger');
    }
}

// Инициализация
document.addEventListener('DOMContentLoaded', async () => {
    console.log('DOM loaded, current path:', window.location.pathname);
    if (window.location.pathname.endsWith('tracker.html')) {
        if (!isAuthenticated()) {
            console.log('Not authenticated, redirecting to login');
            window.location.href = 'login.html';
            return;
        }
        await fetchStatuses();
        applySavedOrder();

        const today = new Date().toISOString().split('T')[0];
        const datePicker = document.getElementById('datePicker');
        if (datePicker) {
            datePicker.value = today;
            datePicker.addEventListener('change', async () => {
                console.log('Date picker changed to:', datePicker.value);
                await loadDayData();
                await updateWorkedHours();
            });

            await loadDayData();
            await updateWorkedHours();
            displayStatuses();
        }
    } else if (window.location.pathname.endsWith('index.html')) {
        if (isAuthenticated()) {
            console.log('Authenticated, redirecting to tracker');
            window.location.href = 'tracker.html';
        }
    }
});