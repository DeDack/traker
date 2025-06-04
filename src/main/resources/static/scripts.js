let statusOptions = [];

// Ключ для хранения порядка статусов в localStorage
const STATUS_ORDER_KEY = 'statusOrder';

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

// Показ/скрытие лоадера
function toggleLoader(show) {
    const loader = document.getElementById('loader');
    if (loader) {
        loader.style.display = show ? 'block' : 'none';
    }
}

// Проверка авторизации через API
async function isAuthenticated() {
    console.log('Checking authentication status');
    try {
        const resp = await fetch('/api/users/me', {
            method: 'GET',
            credentials: 'include'
        });
        console.log('Auth check status:', resp.status);
        return resp.ok;
    } catch (e) {
        console.error('Error checking auth:', e);
        return false;
    }
}

// Обновление токена
async function refreshAccessToken() {
    console.log('Attempting to refresh token');
    toggleLoader(true);
    try {
        const resp = await fetch('/api/auth/refresh', {
            method: 'POST',
            credentials: 'include'
        });

        console.log('Refresh token response status:', resp.status);
        if (resp.ok) {
            console.log('Token refreshed successfully');
            return true;
        } else {
            console.log('Refresh failed');
            return false;
        }
    } catch (e) {
        console.error('Error refreshing token:', e);
        return false;
    } finally {
        toggleLoader(false);
    }
}

// Универсальная функция для API-запросов с ретраем
async function apiFetch(url, options = {}, retries = 2) {
    for (let i = 0; i <= retries; i++) {
        try {
            const resp = await fetch(url, {
                ...options,
                credentials: 'include',
                headers: {
                    ...options.headers,
                    'Content-Type': 'application/json'
                }
            });

            if (resp.status === 401 || resp.status === 403) {
                console.log(`Received ${resp.status}, attempting to refresh token`);
                const refreshed = await refreshAccessToken();
                if (refreshed) {
                    // Повторяем запрос после обновления токена
                    return fetch(url, {
                        ...options,
                        credentials: 'include',
                        headers: {
                            ...options.headers,
                            'Content-Type': 'application/json'
                        }
                    });
                } else {
                    console.log('Refresh failed, redirecting to index');
                    window.location.href = '/index.html';
                    throw new Error('Authentication failed');
                }
            }
            return resp;
        } catch (e) {
            if (i === retries) {
                console.error('API fetch failed after retries:', e);
                window.location.href = '/index.html';
                throw e;
            }
            console.log(`Retrying request (${i + 1}/${retries})...`);
            await new Promise(resolve => setTimeout(resolve, 1000));
        }
    }
}

// Отображение имени пользователя
async function displayUsername() {
    try {
        const resp = await apiFetch('/api/users/me');
        if (resp.ok) {
            const user = await resp.json();
            const usernameSpan = document.getElementById('username');
            if (usernameSpan) {
                usernameSpan.textContent = user.name || user.username || 'Пользователь';
            }
        }
    } catch (e) {
        console.error('Error fetching user:', e);
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
    if (username.length < 3) {
        showMessage('Имя пользователя должно быть не короче 3 символов', 'danger');
        return;
    }
    if (password.length < 6) {
        showMessage('Пароль должен быть не короче 6 символов', 'danger');
        return;
    }
    if (!/^[a-zA-Zа-яА-Я\s]+$/.test(name)) {
        showMessage('Имя должно содержать только буквы и пробелы', 'danger');
        return;
    }

    const payload = { name, username, password };
    console.log('Sending register request with payload:', payload);

    try {
        const resp = await fetch('/api/users/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
            credentials: 'include'
        });

        console.log('Register response status:', resp.status);
        if (resp.ok) {
            showMessage('Регистрация успешна! Войдите в систему.', 'success');
            setTimeout(() => window.location.href = '/login.html', 2000);
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
            body: JSON.stringify(payload),
            credentials: 'include'
        });

        console.log('Login response status:', resp.status);
        if (resp.ok) {
            console.log('Login successful, redirecting to tracker');
            window.location.href = '/tracker.html';
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
async function logout() {
    console.log('Logout function called');
    try {
        const resp = await fetch('/api/auth/logout', {
            method: 'POST',
            credentials: 'include'
        });
        console.log('Logout response status:', resp.status);
        window.location.href = '/index.html';
    } catch (error) {
        console.error('Logout error:', error);
        window.location.href = '/index.html';
    }
}

// Загрузка статусов
async function fetchStatuses() {
    console.log('Fetching statuses');
    try {
        const resp = await apiFetch('/api/statuses/getAllStatuses');
        console.log('Fetch statuses response status:', resp.status);
        if (resp.ok) {
            statusOptions = await resp.json();
            console.log('Statuses loaded:', statusOptions);
            applySavedOrder();
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
    const date = document.getElementById('datePicker')?.value;
    if (!date) return;
    try {
        console.log('Loading day data for date:', date);
        const resp = await apiFetch(`/api/days/${date}`);
        console.log('Load day data response status:', resp.status);
        if (resp.ok) {
            const entries = await resp.json();
            renderTimeEntries(entries);
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
    const date = document.getElementById('datePicker')?.value;
    if (!date) return;
    try {
        console.log('Updating worked hours for date:', date);
        const resp = await apiFetch(`/api/stats/daily?date=${date}`);
        console.log('Update worked hours response status:', resp.status);
        if (resp.ok) {
            const hours = await resp.json();
            document.getElementById('workedHours').textContent = hours;
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
    const date = document.getElementById('datePicker')?.value;
    if (!date) return;
    const worked = document.querySelector(`input[type="checkbox"][data-hour="${hour}"]`)?.checked;
    const comment = document.querySelector(`input[type="text"][data-hour="${hour}"]`)?.value;
    const statusName = document.querySelector(`select[data-hour="${hour}"]`)?.value;

    const payload = { hour, worked, comment, status: statusName ? { name: statusName } : null };
    console.log('Saving time entry with payload:', payload);

    try {
        const resp = await apiFetch(`/api/days/${date}`, {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
        console.log('Save time entry response status:', resp.status);
        if (resp.ok) {
            showMessage(`Час ${hour}:00 сохранён!`, 'success');
            await updateWorkedHours();
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
    const name = document.getElementById('newStatusName')?.value.trim();
    if (!name) return;
    const payload = { name };
    console.log('Adding status with payload:', payload);

    try {
        const resp = await apiFetch('/api/statuses/createStatus', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        console.log('Add status response status:', resp.status);
        if (resp.ok) {
            document.getElementById('newStatusName').value = '';
            await fetchStatuses();
            displayStatuses();
            await loadDayData();
            await updateWorkedHours();
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
    const name = document.querySelector(`input[data-id="${id}"]`)?.value.trim();
    if (!name) return;
    const payload = { name };
    console.log('Updating status with payload:', payload);

    try {
        const resp = await apiFetch(`/api/statuses/updateStatus/${id}`, {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
        console.log('Update status response status:', resp.status);
        if (resp.ok) {
            await fetchStatuses();
            displayStatuses();
            await loadDayData();
            await updateWorkedHours();
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
    console.log('Deleting status with id:', id);

    try {
        const resp = await apiFetch(`/api/statuses/deleteStatus/${id}`, {
            method: 'DELETE'
        });
        console.log('Delete status response status:', resp.status);
        if (resp.ok) {
            await fetchStatuses();
            displayStatuses();
            await loadDayData();
            await updateWorkedHours();
        } else {
            showMessage('Ошибка удаления', 'danger');
        }
    } catch (e) {
        console.error('Error deleting status:', e);
        showMessage('Ошибка соединения', 'danger');
    }
}

// Превентивное обновление токена
function startTokenRefreshTimer() {
    setInterval(async () => {
        if (await isAuthenticated()) {
            await refreshAccessToken();
        }
    }, 55 * 60 * 1000); // Каждые 55 минут
}

// Инициализация
document.addEventListener('DOMContentLoaded', async () => {
    console.log('DOM loaded, current path:', window.location.pathname);
    startTokenRefreshTimer();

    if (window.location.pathname.endsWith('tracker.html')) {
        const authenticated = await isAuthenticated();
        if (!authenticated) {
            console.log('Not authenticated, attempting to refresh token');
            const refreshed = await refreshAccessToken();
            if (!refreshed) {
                console.log('Refresh failed, redirecting to index');
                window.location.href = '/index.html';
                return;
            }
        }
        await fetchStatuses();
        applySavedOrder();
        await displayUsername();

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
    } else if (window.location.pathname.endsWith('index.html') || window.location.pathname === '/') {
        const authenticated = await isAuthenticated();
        if (authenticated) {
            console.log('Authenticated, redirecting to tracker');
            window.location.href = '/tracker.html';
        } else {
            console.log('Not authenticated, attempting to refresh token');
            const refreshed = await refreshAccessToken();
            if (refreshed) {
                console.log('Token refreshed, redirecting to tracker');
                window.location.href = '/tracker.html';
            }
        }
    } else if (window.location.pathname.endsWith('login.html') || window.location.pathname.endsWith('register.html')) {
        const authenticated = await isAuthenticated();
        if (authenticated) {
            console.log('Authenticated, redirecting to tracker');
            window.location.href = '/tracker.html';
        } else {
            console.log('Not authenticated, attempting to refresh token');
            const refreshed = await refreshAccessToken();
            if (refreshed) {
                console.log('Token refreshed, redirecting to tracker');
                window.location.href = '/tracker.html';
            }
        }
    }
});