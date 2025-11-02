(function () {
    let expenseCategories = [];
    let incomeCategories = [];
    const chartInstances = {};
    let categoryTrendSource = new Map();
    let categoryTrendLabels = [];

    const COLOR_PALETTE = [
        '#1f6feb', '#d73a4a', '#238636', '#8250df', '#fb8c00',
        '#e11d48', '#0ea5e9', '#6366f1', '#14b8a6', '#f97316'
    ];

    window.initFinancePage = async function () {
        if (!(await isAuthenticated()) && !(await refreshAccessToken())) {
            return showMessage('Сессия истекла, пожалуйста, войдите снова', 'danger', '/login.html');
        }

        setupEventHandlers();
        await Promise.all([loadExpenseCategories(), loadIncomeCategories()]);
        addBatchRow('expense');
        addBatchRow('income');

        const currentMonth = new Date().toISOString().slice(0, 7);
        document.getElementById('expenseDefaultMonth').value = currentMonth;
        document.getElementById('incomeDefaultMonth').value = currentMonth;
        document.getElementById('budgetMonth').value = currentMonth;
        document.getElementById('filterMonth').value = currentMonth;

        await loadBudgetData(currentMonth);
        await loadDashboard();
    };

    function setupEventHandlers() {
        document.getElementById('refreshCategoriesBtn').addEventListener('click', async () => {
            await Promise.all([loadExpenseCategories(), loadIncomeCategories()]);
            showMessage('Категории обновлены', 'success');
        });

        document.getElementById('expenseCategoryForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await submitCategory('expense');
        });
        document.getElementById('incomeCategoryForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await submitCategory('income');
        });

        document.getElementById('toggleExpenses').addEventListener('click', () => toggleBatchSection('expense'));
        document.getElementById('toggleIncomes').addEventListener('click', () => toggleBatchSection('income'));
        document.getElementById('addExpenseRow').addEventListener('click', () => addBatchRow('expense'));
        document.getElementById('addIncomeRow').addEventListener('click', () => addBatchRow('income'));
        document.getElementById('applyExpenseMonth').addEventListener('click', () => applyDefaultMonth('expense'));
        document.getElementById('applyIncomeMonth').addEventListener('click', () => applyDefaultMonth('income'));
        document.getElementById('saveExpenseBatch').addEventListener('click', () => saveBatch('expense'));
        document.getElementById('saveIncomeBatch').addEventListener('click', () => saveBatch('income'));

        document.getElementById('loadBudget').addEventListener('click', async () => {
            const month = document.getElementById('budgetMonth').value;
            if (!month) return showMessage('Выберите месяц для загрузки бюджета', 'warning');
            await loadBudgetData(month);
        });

        document.getElementById('budgetForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await saveBudget();
        });

        document.getElementById('analyticsFilters').addEventListener('submit', async (e) => {
            e.preventDefault();
            await loadDashboard();
        });
    }

    async function loadExpenseCategories() {
        const resp = await apiFetch('/api/expense-categories');
        expenseCategories = resp.ok ? await resp.json() : [];
        renderCategoryTable('expense', expenseCategories);
        updateCategorySelects();
    }

    async function loadIncomeCategories() {
        const resp = await apiFetch('/api/income-categories');
        incomeCategories = resp.ok ? await resp.json() : [];
        renderCategoryTable('income', incomeCategories);
        updateCategorySelects();
    }

    async function submitCategory(type) {
        const isExpense = type === 'expense';
        const nameInput = document.getElementById(`${type}CategoryName`);
        const descriptionInput = document.getElementById(`${type}CategoryDescription`);
        const payload = {
            name: nameInput.value.trim(),
            description: descriptionInput.value.trim() || null
        };
        if (!payload.name) {
            return showMessage('Введите название категории', 'warning');
        }
        const url = isExpense ? '/api/expense-categories' : '/api/income-categories';
        const resp = await apiFetch(url, { method: 'POST', body: JSON.stringify(payload) });
        if (resp.ok) {
            nameInput.value = '';
            descriptionInput.value = '';
            await (isExpense ? loadExpenseCategories() : loadIncomeCategories());
            showMessage('Категория сохранена', 'success');
        } else {
            showMessage(await resp.text() || 'Не удалось сохранить категорию', 'danger');
        }
    }

    function renderCategoryTable(type, categories) {
        const tbody = document.getElementById(`${type}CategoryTable`);
        tbody.innerHTML = '';
        categories.forEach(category => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${escapeHtml(category.name)}</td>
                <td>${escapeHtml(category.description || '')}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-secondary me-2" data-action="edit">Редактировать</button>
                    <button class="btn btn-sm btn-outline-danger" data-action="delete">Удалить</button>
                </td>
            `;
            row.querySelector('[data-action="edit"]').addEventListener('click', async () => {
                const newName = prompt('Новое название категории', category.name);
                if (newName === null || !newName.trim()) return;
                const newDescription = prompt('Новое описание', category.description || '') ?? '';
                const payload = { name: newName.trim(), description: newDescription.trim() || null };
                const url = type === 'expense'
                    ? `/api/expense-categories/${category.id}`
                    : `/api/income-categories/${category.id}`;
                const resp = await apiFetch(url, { method: 'PUT', body: JSON.stringify(payload) });
                if (resp.ok) {
                    await (type === 'expense' ? loadExpenseCategories() : loadIncomeCategories());
                    showMessage('Категория обновлена', 'success');
                } else {
                    showMessage(await resp.text() || 'Не удалось обновить категорию', 'danger');
                }
            });
            row.querySelector('[data-action="delete"]').addEventListener('click', async () => {
                if (!confirm('Удалить категорию?')) return;
                const url = type === 'expense'
                    ? `/api/expense-categories/${category.id}`
                    : `/api/income-categories/${category.id}`;
                const resp = await apiFetch(url, { method: 'DELETE' });
                if (resp.ok) {
                    await (type === 'expense' ? loadExpenseCategories() : loadIncomeCategories());
                    showMessage('Категория удалена', 'success');
                } else {
                    showMessage(await resp.text() || 'Не удалось удалить категорию', 'danger');
                }
            });
            tbody.appendChild(row);
        });
    }

    function toggleBatchSection(type) {
        const isExpense = type === 'expense';
        document.getElementById('expenseBatchSection').style.display = isExpense ? 'block' : 'none';
        document.getElementById('incomeBatchSection').style.display = isExpense ? 'none' : 'block';
        document.getElementById('toggleExpenses').classList.toggle('active', isExpense);
        document.getElementById('toggleIncomes').classList.toggle('active', !isExpense);
    }

    function addBatchRow(type) {
        const tbody = document.getElementById(`${type}BatchTable`);
        const row = document.createElement('tr');
        row.innerHTML = `
            <td><input type="text" class="form-control title" placeholder="Название" required></td>
            <td><input type="text" class="form-control description" placeholder="Описание"></td>
            <td><input type="number" min="0" step="0.01" class="form-control amount" placeholder="0.00" required></td>
            <td><select class="form-select category" required></select></td>
            <td><input type="date" class="form-control date"></td>
            <td><input type="month" class="form-control period"></td>
            <td class="text-end"><button class="btn btn-sm btn-outline-danger remove-row">✕</button></td>
        `;
        row.querySelector('.remove-row').addEventListener('click', () => row.remove());
        tbody.appendChild(row);
        updateCategorySelects();
    }

    function updateCategorySelects() {
        updateSelectForType('expense', expenseCategories);
        updateSelectForType('income', incomeCategories);
    }

    function updateSelectForType(type, categories) {
        const options = ['<option value="">Выберите...</option>',
            ...categories.map(cat => `<option value="${cat.id}">${escapeHtml(cat.name)}</option>`)
        ].join('');
        document.querySelectorAll(`#${type}BatchTable select.category`).forEach(select => {
            const current = select.value;
            select.innerHTML = options;
            if (current) select.value = current;
        });
    }

    function applyDefaultMonth(type) {
        const defaultMonth = document.getElementById(`${type}DefaultMonth`).value;
        if (!defaultMonth) {
            return showMessage('Укажите общий месяц', 'warning');
        }
        document.querySelectorAll(`#${type}BatchTable input.period`).forEach(input => {
            input.value = defaultMonth;
        });
        showMessage('Месяц проставлен для всех записей', 'info');
    }

    async function saveBatch(type) {
        const tbody = document.getElementById(`${type}BatchTable`);
        const rows = Array.from(tbody.querySelectorAll('tr'));
        if (!rows.length) {
            return showMessage('Добавьте хотя бы одну запись', 'warning');
        }
        const records = [];
        for (const row of rows) {
            const title = row.querySelector('.title').value.trim();
            const amountValue = row.querySelector('.amount').value;
            const categoryId = row.querySelector('.category').value;
            if (!title || !amountValue || !categoryId) {
                return showMessage('Заполните название, сумму и категорию у каждой записи', 'danger');
            }
            const record = {
                title,
                description: row.querySelector('.description').value.trim() || null,
                amount: parseFloat(amountValue),
                categoryId: Number(categoryId),
            };
            const dateValue = row.querySelector('.date').value;
            const periodValue = row.querySelector('.period').value;
            if (type === 'expense') {
                record.expenseDate = dateValue || null;
                record.period = periodValue || null;
            } else {
                record.incomeDate = dateValue || null;
                record.period = periodValue || null;
            }
            records.push(record);
        }

        const payload = {};
        const defaultMonth = document.getElementById(`${type}DefaultMonth`).value;
        if (defaultMonth) payload.defaultPeriod = defaultMonth;
        if (type === 'expense') {
            payload.expenses = records;
        } else {
            payload.incomes = records;
        }
        const url = type === 'expense' ? '/api/expenses/batch' : '/api/incomes/batch';
        const resp = await apiFetch(url, { method: 'POST', body: JSON.stringify(payload) });
        if (resp.ok) {
            tbody.innerHTML = '';
            addBatchRow(type);
            showMessage(`${type === 'expense' ? 'Расходы' : 'Доходы'} сохранены`, 'success');
            await loadDashboard();
        } else {
            const text = await resp.text();
            showMessage(text || 'Не удалось сохранить записи', 'danger');
        }
    }

    async function loadBudgetData(month) {
        try {
            const resp = await apiFetch(`/api/budgets/${month}`);
            if (!resp.ok) throw new Error(await resp.text());
            const budget = await resp.json();
            document.getElementById('plannedIncome').value = toInputValue(budget.plannedIncome);
            document.getElementById('plannedExpense').value = toInputValue(budget.plannedExpense);
            document.getElementById('savingsGoal').value = toInputValue(budget.savingsGoal);
            document.getElementById('budgetNotes').value = budget.notes || '';
            renderBudgetCards(budget);
        } catch (e) {
            renderBudgetCards(null);
            document.getElementById('plannedIncome').value = '';
            document.getElementById('plannedExpense').value = '';
            document.getElementById('savingsGoal').value = '';
            document.getElementById('budgetNotes').value = '';
            if (e.message) showMessage(e.message, 'danger');
        }
    }

    async function saveBudget() {
        const month = document.getElementById('budgetMonth').value;
        if (!month) return showMessage('Выберите месяц для бюджета', 'warning');
        const payload = {
            month,
            plannedIncome: parseOrNull(document.getElementById('plannedIncome').value),
            plannedExpense: parseOrNull(document.getElementById('plannedExpense').value),
            savingsGoal: parseOrNull(document.getElementById('savingsGoal').value),
            notes: document.getElementById('budgetNotes').value.trim() || null
        };
        const resp = await apiFetch('/api/budgets', { method: 'POST', body: JSON.stringify(payload) });
        if (resp.ok) {
            const budget = await resp.json();
            renderBudgetCards(budget);
            showMessage('Бюджет сохранен', 'success');
            await loadDashboard();
        } else {
            showMessage(await resp.text() || 'Не удалось сохранить бюджет', 'danger');
        }
    }

    function renderBudgetCards(budget) {
        const container = document.getElementById('budgetSummaryCards');
        if (!budget) {
            container.innerHTML = '<div class="col-12 text-muted">Бюджет для выбранного месяца не найден</div>';
            return;
        }
        container.innerHTML = `
            <div class="col-md-3">
                <div class="card border-0 shadow-sm h-100">
                    <div class="card-body">
                        <p class="text-muted mb-1">Доходы</p>
                        <h5 class="fw-bold text-success mb-1">${formatCurrency(budget.actualIncome)}</h5>
                        <small class="text-muted">План: ${formatCurrency(budget.plannedIncome)}</small>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card border-0 shadow-sm h-100">
                    <div class="card-body">
                        <p class="text-muted mb-1">Расходы</p>
                        <h5 class="fw-bold text-danger mb-1">${formatCurrency(budget.actualExpense)}</h5>
                        <small class="text-muted">План: ${formatCurrency(budget.plannedExpense)}</small>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card border-0 shadow-sm h-100">
                    <div class="card-body">
                        <p class="text-muted mb-1">Баланс</p>
                        <h5 class="fw-bold ${budget.actualBalance >= 0 ? 'text-success' : 'text-danger'} mb-1">${formatCurrency(budget.actualBalance)}</h5>
                        <small class="text-muted">План: ${formatCurrency(budget.plannedBalance)}</small>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card border-0 shadow-sm h-100">
                    <div class="card-body">
                        <p class="text-muted mb-1">Накопления</p>
                        <h5 class="fw-bold ${budget.savingsProgress >= 0 ? 'text-success' : 'text-danger'} mb-1">${formatCurrency(budget.savingsProgress)}</h5>
                        <small class="text-muted">Цель: ${formatCurrency(budget.savingsGoal)}</small>
                    </div>
                </div>
            </div>
        `;
    }

    async function loadDashboard() {
        const params = new URLSearchParams();
        const from = document.getElementById('filterFrom').value;
        const to = document.getElementById('filterTo').value;
        const month = document.getElementById('filterMonth').value;
        if (month && (from || to)) {
            showMessage('Укажите либо месяц, либо диапазон дат', 'warning');
            return;
        }
        if (from) params.set('from', from);
        if (to) params.set('to', to);
        if (month) params.set('month', month);

        const url = `/api/budgets/dashboard${params.toString() ? `?${params}` : ''}`;
        const resp = await apiFetch(url);
        if (!resp.ok) {
            showMessage(await resp.text() || 'Не удалось загрузить дашборд', 'danger');
            return;
        }
        const data = await resp.json();
        renderDashboardSummary(data);
        renderCharts(data);
    }

    function renderDashboardSummary(data) {
        const container = document.getElementById('dashboardSummary');
        container.innerHTML = `
            <div class="col-md-3">
                <div class="card border-0 shadow-sm h-100 text-center">
                    <div class="card-body">
                        <p class="text-muted mb-1">Всего доходов</p>
                        <h4 class="fw-bold text-success">${formatCurrency(data.totalIncome)}</h4>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card border-0 shadow-sm h-100 text-center">
                    <div class="card-body">
                        <p class="text-muted mb-1">Всего расходов</p>
                        <h4 class="fw-bold text-danger">${formatCurrency(data.totalExpenses)}</h4>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card border-0 shadow-sm h-100 text-center">
                    <div class="card-body">
                        <p class="text-muted mb-1">Чистый баланс</p>
                        <h4 class="fw-bold ${data.netBalance >= 0 ? 'text-success' : 'text-danger'}">${formatCurrency(data.netBalance)}</h4>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card border-0 shadow-sm h-100 text-center">
                    <div class="card-body">
                        <p class="text-muted mb-1">Прогресс накоплений</p>
                        <h4 class="fw-bold">${formatCurrency(data.savingsProgress)}</h4>
                        <small class="text-muted">Цель: ${formatCurrency(data.totalSavingsGoal)}</small>
                    </div>
                </div>
            </div>
        `;
    }

    function renderCharts(data) {
        renderPieChart('expenseByCategoryChart', data.expenseSummary?.totalsByCategory || [], 'Расходы по категориям');
        renderPieChart('incomeByCategoryChart', data.incomeSummary?.totalsByCategory || [], 'Доходы по категориям');
        renderCashFlowChart(data.budgets || []);
        renderCategoryTrends(data.expenseSummary?.categoryMonthlyTotals || []);
    }

    function renderPieChart(elementId, totals, label) {
        const ctx = document.getElementById(elementId);
        if (!ctx) return;
        const labels = totals.map(item => item.categoryName);
        const values = totals.map(item => Number(item.totalAmount || 0));
        destroyChart(elementId);
        chartInstances[elementId] = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels,
                datasets: [{
                    label,
                    data: values,
                    backgroundColor: COLOR_PALETTE,
                    borderWidth: 0
                }]
            },
            options: {
                plugins: {
                    legend: { position: 'bottom' }
                }
            }
        });
    }

    function renderCashFlowChart(budgets) {
        const ctx = document.getElementById('cashFlowChart');
        if (!ctx) return;
        const labels = budgets.map(b => formatMonthLabel(b.month));
        const incomes = budgets.map(b => Number(b.actualIncome || 0));
        const expenses = budgets.map(b => Number(b.actualExpense || 0));
        destroyChart('cashFlowChart');
        chartInstances.cashFlowChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels,
                datasets: [
                    {
                        label: 'Доходы',
                        data: incomes,
                        backgroundColor: 'rgba(35, 134, 54, 0.7)'
                    },
                    {
                        label: 'Расходы',
                        data: expenses,
                        backgroundColor: 'rgba(215, 58, 74, 0.7)'
                    }
                ]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { callback: value => formatCurrency(value) }
                    }
                }
            }
        });
    }

    function renderCategoryTrends(categoryTotals) {
        categoryTrendSource = new Map();
        const months = new Set();
        categoryTotals.forEach(cat => {
            categoryTrendSource.set(String(cat.categoryId), {
                name: cat.categoryName,
                totals: cat.monthlyTotals || []
            });
            (cat.monthlyTotals || []).forEach(total => months.add(total.period));
        });
        categoryTrendLabels = Array.from(months).sort();

        const container = document.getElementById('categoryToggleContainer');
        container.innerHTML = '';
        let index = 0;
        categoryTrendSource.forEach((value, key) => {
            const id = `category-toggle-${key}`;
            const wrapper = document.createElement('div');
            wrapper.className = 'form-check form-check-inline';
            wrapper.innerHTML = `
                <input class="form-check-input category-toggle" type="checkbox" id="${id}" data-category="${key}" checked>
                <label class="form-check-label" for="${id}">${escapeHtml(value.name)}</label>
            `;
            container.appendChild(wrapper);
            index++;
        });

        container.querySelectorAll('.category-toggle').forEach(input => {
            input.addEventListener('change', updateCategoryTrendChart);
        });
        updateCategoryTrendChart();
    }

    function updateCategoryTrendChart() {
        const selected = Array.from(document.querySelectorAll('.category-toggle:checked'))
            .map(input => input.dataset.category);
        const datasets = [];
        selected.forEach((key, idx) => {
            const entry = categoryTrendSource.get(key);
            if (!entry) return;
            const dataByMonth = new Map(entry.totals.map(total => [total.period, Number(total.totalAmount || 0)]));
            const data = categoryTrendLabels.map(month => dataByMonth.get(month) || 0);
            datasets.push({
                label: entry.name,
                data,
                borderColor: COLOR_PALETTE[idx % COLOR_PALETTE.length],
                backgroundColor: COLOR_PALETTE[idx % COLOR_PALETTE.length],
                tension: 0.3,
                fill: false
            });
        });

        const ctx = document.getElementById('categoryTrendChart');
        if (!ctx) return;
        destroyChart('categoryTrendChart');
        chartInstances.categoryTrendChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: categoryTrendLabels.map(formatMonthLabel),
                datasets
            },
            options: {
                responsive: true,
                interaction: { mode: 'index', intersect: false },
                stacked: false,
                plugins: { legend: { position: 'bottom' } },
                scales: {
                    y: { ticks: { callback: value => formatCurrency(value) } }
                }
            }
        });
    }

    function destroyChart(key) {
        if (chartInstances[key]) {
            chartInstances[key].destroy();
            delete chartInstances[key];
        }
    }

    function toInputValue(value) {
        return value == null ? '' : Number(value).toFixed(2);
    }

    function parseOrNull(value) {
        if (!value) return null;
        const num = parseFloat(value);
        return Number.isNaN(num) ? null : num;
    }

    function formatCurrency(value) {
        const num = Number(value || 0);
        return num.toLocaleString('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 2 });
    }

    function formatMonthLabel(period) {
        if (!period) return '';
        const [year, month] = period.split('-');
        const date = new Date(Number(year), Number(month) - 1, 1);
        return date.toLocaleDateString('ru-RU', { month: 'short', year: 'numeric' });
    }
})();
