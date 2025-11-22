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

    const managementState = {
        expense: {
            records: [],
            selected: new Set(),
            modal: null,
            bulkModal: null,
            config: {
                endpoint: '/api/expenses',
                bulkEndpoint: '/api/expenses/bulk',
                bulkDeleteEndpoint: '/api/expenses/bulk-delete',
                dateField: 'expenseDate',
                manageForm: 'expenseManageFilterForm',
                manageFrom: 'expenseManageFrom',
                manageTo: 'expenseManageTo',
                manageCategories: 'expenseManageCategories',
                manageBody: 'expenseManageBody',
                selectAll: 'expenseSelectAll',
                bulkDeleteBtn: 'expenseBulkDelete',
                bulkCategoryBtn: 'expenseBulkCategoryBtn',
                bulkCategorySelect: 'bulkExpenseCategory',
                editModalId: 'editExpenseModal',
                bulkModalId: 'expenseBulkCategoryModal',
                editForm: 'editExpenseForm',
                bulkForm: 'expenseBulkCategoryForm',
                editFields: {
                    id: 'editExpenseId',
                    title: 'editExpenseTitle',
                    amount: 'editExpenseAmount',
                    category: 'editExpenseCategory',
                    date: 'editExpenseDate',
                    period: 'editExpensePeriod',
                    description: 'editExpenseDescription'
                },
                labels: {
                    updated: 'Трата обновлена',
                    deleted: 'Трата удалена',
                    bulkDeleted: 'Выбранные траты удалены',
                    bulkUpdated: 'Категория обновлена'
                }
            }
        },
        income: {
            records: [],
            selected: new Set(),
            modal: null,
            bulkModal: null,
            config: {
                endpoint: '/api/incomes',
                bulkEndpoint: '/api/incomes/bulk',
                bulkDeleteEndpoint: '/api/incomes/bulk-delete',
                dateField: 'incomeDate',
                manageForm: 'incomeManageFilterForm',
                manageFrom: 'incomeManageFrom',
                manageTo: 'incomeManageTo',
                manageCategories: 'incomeManageCategories',
                manageBody: 'incomeManageBody',
                selectAll: 'incomeSelectAll',
                bulkDeleteBtn: 'incomeBulkDelete',
                bulkCategoryBtn: 'incomeBulkCategoryBtn',
                bulkCategorySelect: 'bulkIncomeCategory',
                editModalId: 'editIncomeModal',
                bulkModalId: 'incomeBulkCategoryModal',
                editForm: 'editIncomeForm',
                bulkForm: 'incomeBulkCategoryForm',
                editFields: {
                    id: 'editIncomeId',
                    title: 'editIncomeTitle',
                    amount: 'editIncomeAmount',
                    category: 'editIncomeCategory',
                    date: 'editIncomeDate',
                    period: 'editIncomePeriod',
                    description: 'editIncomeDescription'
                },
                labels: {
                    updated: 'Доход обновлен',
                    deleted: 'Доход удален',
                    bulkDeleted: 'Выбранные доходы удалены',
                    bulkUpdated: 'Категория обновлена'
                }
            }
        }
    };

    function initFinanceCollapses() {
        setupCollapseToggle('analyticsSectionToggle', 'analyticsSectionCollapse', { storageKey: 'finance.analytics', defaultExpanded: true });
        setupCollapseToggle('budgetSectionToggle', 'budgetSectionCollapse', { storageKey: 'finance.budget', defaultExpanded: true });
        setupCollapseToggle('categorySectionToggle', 'categorySectionCollapse', { storageKey: 'finance.categories', defaultExpanded: true });
    }

    window.initFinancePage = async function () {
        if (!(await isAuthenticated()) && !(await refreshAccessToken())) {
            return showMessage('Сессия истекла, пожалуйста, войдите снова', 'danger', '/login.html');
        }

        initFinanceCollapses();
        setupEventHandlers();
        initManagementComponents();
        await Promise.all([loadExpenseCategories(), loadIncomeCategories()]);
        addBatchRow('expense');
        addBatchRow('income');

        const currentMonth = new Date().toISOString().slice(0, 7);
        document.getElementById('expenseDefaultMonth').value = currentMonth;
        document.getElementById('incomeDefaultMonth').value = currentMonth;
        document.getElementById('budgetMonth').value = currentMonth;
        document.getElementById('filterMonth').value = currentMonth;
        const today = new Date();
        const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
        const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0);
        document.getElementById('detailFrom').value = formatDateInput(firstDay);
        document.getElementById('detailTo').value = formatDateInput(lastDay);
        const expenseManageFrom = document.getElementById('expenseManageFrom');
        const expenseManageTo = document.getElementById('expenseManageTo');
        const incomeManageFrom = document.getElementById('incomeManageFrom');
        const incomeManageTo = document.getElementById('incomeManageTo');
        if (expenseManageFrom) expenseManageFrom.value = formatDateInput(firstDay);
        if (expenseManageTo) expenseManageTo.value = formatDateInput(lastDay);
        if (incomeManageFrom) incomeManageFrom.value = formatDateInput(firstDay);
        if (incomeManageTo) incomeManageTo.value = formatDateInput(lastDay);

        await loadBudgetData(currentMonth);
        await loadDashboard();
        await loadExpenseDetails();
        await loadManagementRecords('expense');
        await loadManagementRecords('income');
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

        document.getElementById('detailFilterForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await loadExpenseDetails();
        });

        ['expense', 'income'].forEach(type => {
            const state = managementState[type];
            const config = state.config;
            const filterForm = document.getElementById(config.manageForm);
            if (filterForm) {
                filterForm.addEventListener('submit', async (e) => {
                    e.preventDefault();
                    await loadManagementRecords(type);
                });
            }

            const bulkDeleteBtn = document.getElementById(config.bulkDeleteBtn);
            if (bulkDeleteBtn) {
                bulkDeleteBtn.addEventListener('click', () => bulkDelete(type));
            }

            const bulkCategoryBtn = document.getElementById(config.bulkCategoryBtn);
            if (bulkCategoryBtn) {
                bulkCategoryBtn.addEventListener('click', () => openBulkCategoryModal(type));
            }

            const bulkForm = document.getElementById(config.bulkForm);
            if (bulkForm) {
                bulkForm.addEventListener('submit', async (e) => {
                    e.preventDefault();
                    await submitBulkCategory(type);
                });
            }

            const editForm = document.getElementById(config.editForm);
            if (editForm) {
                editForm.addEventListener('submit', async (e) => {
                    e.preventDefault();
                    await submitEditForm(type);
                });
            }

            const selectAll = document.getElementById(config.selectAll);
            if (selectAll) {
                selectAll.addEventListener('change', (e) => toggleSelectAll(type, e.target.checked));
            }
        });
    }

    function initManagementComponents() {
        if (typeof bootstrap === 'undefined') {
            return;
        }
        Object.values(managementState).forEach(state => {
            const editEl = document.getElementById(state.config.editModalId);
            if (editEl) {
                state.modal = bootstrap.Modal.getOrCreateInstance(editEl);
            }
            const bulkEl = document.getElementById(state.config.bulkModalId);
            if (bulkEl) {
                state.bulkModal = bootstrap.Modal.getOrCreateInstance(bulkEl);
            }
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
        updateAnalyticsCategoryFilters();
        updateManagementCategoryFilters();
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

    function updateAnalyticsCategoryFilters() {
        updateMultiSelectOptions('expenseFilterCategories', expenseCategories);
        updateMultiSelectOptions('incomeFilterCategories', incomeCategories);
        updateMultiSelectOptions('detailCategories', expenseCategories);
    }

    function updateManagementCategoryFilters() {
        updateMultiSelectOptions('expenseManageCategories', expenseCategories);
        updateMultiSelectOptions('incomeManageCategories', incomeCategories);
        updateSingleSelectOptions('editExpenseCategory', expenseCategories, false);
        updateSingleSelectOptions('editIncomeCategory', incomeCategories, false);
        updateSingleSelectOptions('bulkExpenseCategory', expenseCategories, true);
        updateSingleSelectOptions('bulkIncomeCategory', incomeCategories, true);
    }

    function updateSingleSelectOptions(selectId, categories, includePlaceholder = false) {
        const select = document.getElementById(selectId);
        if (!select) return;
        const previous = select.value;
        select.innerHTML = '';
        if (includePlaceholder) {
            const placeholder = document.createElement('option');
            placeholder.value = '';
            placeholder.textContent = 'Выберите категорию';
            select.appendChild(placeholder);
        }
        categories.forEach(category => {
            const option = document.createElement('option');
            option.value = String(category.id);
            option.textContent = category.name;
            select.appendChild(option);
        });
        if (previous && Array.from(select.options).some(option => option.value === previous)) {
            select.value = previous;
        }
    }

    function updateMultiSelectOptions(selectId, categories) {
        const element = document.getElementById(selectId);
        if (!element) return;

        if (element.tagName === 'SELECT') {
            const previous = new Set(Array.from(element.selectedOptions || []).map(option => option.value));
            element.innerHTML = '';
            categories.forEach(category => {
                const option = document.createElement('option');
                option.value = String(category.id);
                option.textContent = category.name;
                if (previous.size === 0 || previous.has(option.value)) {
                    option.selected = true;
                }
                element.appendChild(option);
            });
            if (!categories.length) {
                element.innerHTML = '';
                return;
            }
            if (previous.size === 0) {
                Array.from(element.options).forEach(option => option.selected = true);
            }
            return;
        }

        const previousCheckboxes = Array.from(element.querySelectorAll('input[type="checkbox"]'));
        const checkedValues = new Set(previousCheckboxes.filter(input => input.checked).map(input => input.value));
        const previousValues = new Set(previousCheckboxes.map(input => input.value));
        const shouldSelectNewByDefault = previousCheckboxes.length === 0
            || previousCheckboxes.every(input => input.checked);
        element.innerHTML = '';
        categories.forEach(category => {
            const value = String(category.id);
            const wrapper = document.createElement('div');
            wrapper.className = 'form-check';

            const input = document.createElement('input');
            input.className = 'form-check-input';
            input.type = 'checkbox';
            input.id = `${selectId}-${value}`;
            input.value = value;
            const wasExisting = previousValues.has(value);
            input.checked = checkedValues.has(value) || (!wasExisting && shouldSelectNewByDefault);

            const label = document.createElement('label');
            label.className = 'form-check-label';
            label.setAttribute('for', input.id);
            label.textContent = category.name;

            wrapper.appendChild(input);
            wrapper.appendChild(label);
            element.appendChild(wrapper);
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
        getSelectedValues('expenseFilterCategories').forEach(id => params.append('expenseCategories', id));
        getSelectedValues('incomeFilterCategories').forEach(id => params.append('incomeCategories', id));

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
        renderPieChart('expenseByCategoryChart', data.expenseSummary?.totalsByCategory || [], 'Расходы по категориям', 'expenseByCategoryLegend');
        renderPieChart('incomeByCategoryChart', data.incomeSummary?.totalsByCategory || [], 'Доходы по категориям', 'incomeByCategoryLegend');
        renderCashFlowChart(data.budgets || []);
        renderCategoryTrends(data.expenseSummary?.categoryMonthlyTotals || []);
    }

    async function loadExpenseDetails() {
        const from = document.getElementById('detailFrom').value;
        const to = document.getElementById('detailTo').value;
        if (!from || !to) {
            showMessage('Выберите даты для детализации расходов', 'warning');
            return;
        }
        if (new Date(to) < new Date(from)) {
            showMessage('Дата окончания не может быть раньше даты начала', 'warning');
            return;
        }
        const params = new URLSearchParams();
        params.set('from', from);
        params.set('to', to);
        getSelectedValues('detailCategories').forEach(id => params.append('categories', id));

        const query = params.toString();
        try {
            const [summaryResp, expensesResp] = await Promise.all([
                apiFetch(`/api/expenses/summary?${query}`),
                apiFetch(`/api/expenses?${query}`)
            ]);
            if (!summaryResp.ok) throw new Error(await summaryResp.text());
            if (!expensesResp.ok) throw new Error(await expensesResp.text());
            const summary = await summaryResp.json();
            const expenses = await expensesResp.json();
            document.getElementById('detailTotalsAmount').textContent = formatCurrency(summary.totalAmount);
            renderDetailCategories(summary);
            renderDetailExpenses(expenses);
        } catch (error) {
            document.getElementById('detailTotalsAmount').textContent = formatCurrency(0);
            renderDetailCategories(null);
            renderDetailExpenses([]);
            if (error && error.message) {
                showMessage(error.message, 'danger');
            }
        }
    }

    function renderDetailCategories(summary) {
        const tbody = document.getElementById('detailCategoryBody');
        if (!tbody) return;
        const totals = summary?.totalsByCategory || [];
        if (!totals.length) {
            tbody.innerHTML = '<tr><td colspan="3" class="text-muted">Нет данных для выбранного периода</td></tr>';
            return;
        }
        tbody.innerHTML = totals.map(item => `
            <tr>
                <td>${escapeHtml(item.categoryName)}</td>
                <td class="text-end">${formatCurrency(item.totalAmount)}</td>
                <td class="text-end">${formatPercentage(item.percentage)}</td>
            </tr>
        `).join('');
    }

    function renderDetailExpenses(expenses) {
        const container = document.getElementById('detailExpenseList');
        if (!container) return;
        if (!expenses || !expenses.length) {
            container.innerHTML = '<div class="list-group-item text-muted">Нет операций за выбранный период</div>';
            return;
        }
        container.innerHTML = '';
        expenses.forEach(record => {
            const item = document.createElement('div');
            item.className = 'list-group-item';
            const dateLabel = record.expenseDate
                ? new Date(record.expenseDate).toLocaleDateString('ru-RU')
                : formatMonthLabel(record.period);
            item.innerHTML = `
                <div>
                    <div class="fw-semibold">${escapeHtml(record.title)}</div>
                    <div class="detail-expense-meta">${escapeHtml(record.categoryName || 'Без категории')} · ${escapeHtml(dateLabel || '')}</div>
                </div>
                <div class="fw-bold text-danger">${formatCurrency(record.amount)}</div>
            `;
            container.appendChild(item);
        });
    }

    async function loadManagementRecords(type) {
        const state = managementState[type];
        const config = state.config;
        const fromInput = document.getElementById(config.manageFrom);
        const toInput = document.getElementById(config.manageTo);
        if (!fromInput || !toInput) {
            return;
        }
        const from = fromInput.value;
        const to = toInput.value;
        if (!from || !to) {
            showMessage('Выберите даты для фильтрации', 'warning');
            return;
        }
        if (new Date(to) < new Date(from)) {
            showMessage('Дата окончания не может быть раньше даты начала', 'warning');
            return;
        }

        const params = new URLSearchParams();
        params.set('from', from);
        params.set('to', to);
        getSelectedValues(config.manageCategories).forEach(id => params.append('categories', id));

        try {
            const resp = await apiFetch(`${config.endpoint}?${params.toString()}`);
            if (!resp.ok) {
                throw new Error(await resp.text());
            }
            const data = await resp.json();
            renderManagementTable(type, data);
        } catch (error) {
            renderManagementTable(type, []);
            if (error && error.message) {
                showMessage(error.message, 'danger');
            }
        }
    }

    function renderManagementTable(type, records) {
        const state = managementState[type];
        const config = state.config;
        const tbody = document.getElementById(config.manageBody);
        if (!tbody) return;

        const normalizedRecords = Array.isArray(records)
            ? records.map(record => ({ ...record, id: Number(record.id) }))
            : [];
        state.records = normalizedRecords;
        const availableIds = new Set(normalizedRecords.map(record => record.id));
        state.selected = new Set([...state.selected].filter(id => availableIds.has(id)));

        if (!normalizedRecords.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-muted text-center">Нет операций за выбранный период</td></tr>';
            updateBulkActionState(type);
            return;
        }

        tbody.innerHTML = '';
        normalizedRecords.forEach(record => {
            const tr = document.createElement('tr');
            const dateValue = record[config.dateField];
            const dateLabel = dateValue
                ? new Date(dateValue).toLocaleDateString('ru-RU')
                : (record.period ? formatMonthLabel(record.period) : '—');
            const descriptionBlock = record.description
                ? `<div class="text-muted small">${escapeHtml(record.description)}</div>`
                : '';
            const amountClass = type === 'expense' ? 'text-danger' : 'text-success';
            tr.innerHTML = `
                <td class="table-checkbox">
                    <input class="form-check-input" type="checkbox" data-id="${record.id}">
                </td>
                <td>
                    <div class="fw-semibold">${escapeHtml(record.title || '')}</div>
                    ${descriptionBlock}
                </td>
                <td>${escapeHtml(record.categoryName || 'Без категории')}</td>
                <td>${escapeHtml(dateLabel)}</td>
                <td class="text-end ${amountClass}">${formatCurrency(record.amount)}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-secondary me-2" data-action="edit">Редактировать</button>
                    <button class="btn btn-sm btn-outline-danger" data-action="delete">Удалить</button>
                </td>
            `;
            const checkbox = tr.querySelector('input[type="checkbox"]');
            checkbox.checked = state.selected.has(record.id);
            checkbox.addEventListener('change', (e) => handleRowSelection(type, record.id, e.target.checked));
            tr.querySelector('[data-action="edit"]').addEventListener('click', () => openEditModal(type, record.id));
            tr.querySelector('[data-action="delete"]').addEventListener('click', () => deleteRecord(type, record.id));
            tbody.appendChild(tr);
        });

        updateBulkActionState(type);
    }

    function handleRowSelection(type, id, checked) {
        const state = managementState[type];
        if (checked) {
            state.selected.add(id);
        } else {
            state.selected.delete(id);
        }
        updateBulkActionState(type);
    }

    function toggleSelectAll(type, checked) {
        const state = managementState[type];
        state.selected.clear();
        const checkboxes = document.querySelectorAll(`#${state.config.manageBody} input[type="checkbox"]`);
        checkboxes.forEach(checkbox => {
            checkbox.checked = checked;
            const recordId = Number(checkbox.dataset.id);
            if (checked && !Number.isNaN(recordId)) {
                state.selected.add(recordId);
            }
        });
        updateBulkActionState(type);
    }

    function updateBulkActionState(type) {
        const state = managementState[type];
        const config = state.config;
        const hasSelection = state.selected.size > 0;
        const bulkDeleteBtn = document.getElementById(config.bulkDeleteBtn);
        if (bulkDeleteBtn) bulkDeleteBtn.disabled = !hasSelection;
        const bulkCategoryBtn = document.getElementById(config.bulkCategoryBtn);
        if (bulkCategoryBtn) bulkCategoryBtn.disabled = !hasSelection;
        const selectAll = document.getElementById(config.selectAll);
        if (selectAll) {
            const total = state.records.length;
            if (!total) {
                selectAll.checked = false;
                selectAll.indeterminate = false;
            } else {
                selectAll.checked = state.selected.size === total;
                selectAll.indeterminate = hasSelection && state.selected.size < total;
            }
        }
    }

    function openEditModal(type, id) {
        const state = managementState[type];
        const config = state.config;
        const record = state.records.find(item => item.id === id);
        if (!record || !state.modal) return;
        document.getElementById(config.editFields.id).value = record.id;
        document.getElementById(config.editFields.title).value = record.title || '';
        document.getElementById(config.editFields.amount).value = toInputValue(record.amount);
        const categorySelect = document.getElementById(config.editFields.category);
        if (categorySelect) {
            categorySelect.value = record.categoryId ? String(record.categoryId) : '';
        }
        document.getElementById(config.editFields.date).value = record[config.dateField] || '';
        document.getElementById(config.editFields.period).value = record.period || '';
        document.getElementById(config.editFields.description).value = record.description || '';
        state.modal.show();
    }

    async function submitEditForm(type) {
        const state = managementState[type];
        const config = state.config;
        const id = Number(document.getElementById(config.editFields.id).value);
        const title = document.getElementById(config.editFields.title).value.trim();
        const amountValue = document.getElementById(config.editFields.amount).value;
        const categoryIdValue = document.getElementById(config.editFields.category).value;
        const description = document.getElementById(config.editFields.description).value;
        const dateValue = document.getElementById(config.editFields.date).value;
        const periodValue = document.getElementById(config.editFields.period).value;

        if (!title || !amountValue || !categoryIdValue) {
            return showMessage('Заполните название, сумму и категорию', 'danger');
        }
        const amount = parseFloat(amountValue);
        if (Number.isNaN(amount) || amount <= 0) {
            return showMessage('Сумма должна быть больше нуля', 'danger');
        }
        if (!dateValue && !periodValue) {
            return showMessage('Укажите дату или месяц', 'warning');
        }
        const categoryId = Number(categoryIdValue);
        if (Number.isNaN(categoryId)) {
            return showMessage('Выберите категорию', 'danger');
        }

        const payload = {
            title,
            description: description.trim() ? description.trim() : null,
            amount,
            categoryId
        };
        if (dateValue) {
            payload[config.dateField] = dateValue;
            payload.period = null;
        } else {
            payload[config.dateField] = null;
            payload.period = periodValue;
        }

        try {
            const resp = await apiFetch(`${config.endpoint}/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
            if (!resp.ok) {
                throw new Error(await resp.text());
            }
            state.modal?.hide();
            state.selected.delete(id);
            showMessage(config.labels.updated, 'success');
            await loadManagementRecords(type);
            await loadDashboard();
            if (type === 'expense') {
                await loadExpenseDetails();
            }
        } catch (error) {
            showMessage(error.message || 'Не удалось обновить запись', 'danger');
        }
    }

    function openBulkCategoryModal(type) {
        const state = managementState[type];
        if (!state.selected.size) {
            return;
        }
        const select = document.getElementById(state.config.bulkCategorySelect);
        if (select) {
            select.value = '';
        }
        state.bulkModal?.show();
    }

    async function submitBulkCategory(type) {
        const state = managementState[type];
        const config = state.config;
        const select = document.getElementById(config.bulkCategorySelect);
        if (!select) {
            return;
        }
        const categoryId = Number(select.value);
        if (!categoryId) {
            showMessage('Выберите категорию для обновления', 'warning');
            return;
        }
        const ids = [...state.selected];
        if (!ids.length) {
            state.bulkModal?.hide();
            return;
        }
        const payload = {
            records: ids.map(id => ({ id, categoryId }))
        };
        try {
            const resp = await apiFetch(config.bulkEndpoint, { method: 'PUT', body: JSON.stringify(payload) });
            if (!resp.ok) {
                throw new Error(await resp.text());
            }
            state.bulkModal?.hide();
            showMessage(config.labels.bulkUpdated, 'success');
            await loadManagementRecords(type);
            await loadDashboard();
            if (type === 'expense') {
                await loadExpenseDetails();
            }
        } catch (error) {
            showMessage(error.message || 'Не удалось обновить категорию', 'danger');
        }
    }

    async function bulkDelete(type) {
        const state = managementState[type];
        const config = state.config;
        const ids = [...state.selected];
        if (!ids.length) {
            return;
        }
        if (!confirm('Удалить выбранные записи?')) {
            return;
        }
        try {
            const resp = await apiFetch(config.bulkDeleteEndpoint, { method: 'POST', body: JSON.stringify({ ids }) });
            if (!resp.ok) {
                throw new Error(await resp.text());
            }
            showMessage(config.labels.bulkDeleted, 'success');
            state.selected.clear();
            await loadManagementRecords(type);
            await loadDashboard();
            if (type === 'expense') {
                await loadExpenseDetails();
            }
        } catch (error) {
            showMessage(error.message || 'Не удалось удалить записи', 'danger');
        }
    }

    async function deleteRecord(type, id) {
        const state = managementState[type];
        const config = state.config;
        if (!confirm('Удалить запись?')) {
            return;
        }
        try {
            const resp = await apiFetch(`${config.endpoint}/${id}`, { method: 'DELETE' });
            if (!resp.ok) {
                throw new Error(await resp.text());
            }
            showMessage(config.labels.deleted, 'success');
            state.selected.delete(id);
            await loadManagementRecords(type);
            await loadDashboard();
            if (type === 'expense') {
                await loadExpenseDetails();
            }
        } catch (error) {
            showMessage(error.message || 'Не удалось удалить запись', 'danger');
        }
    }

    function renderPieChart(elementId, totals, label, legendId) {
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

        if (legendId) {
            const legendContainer = document.getElementById(legendId);
            if (legendContainer) {
                if (!totals.length) {
                    legendContainer.innerHTML = '<div class="text-muted small">Нет данных для отображения</div>';
                } else {
                    legendContainer.innerHTML = totals.map(item => `
                        <div class="chart-legend-item">
                            <span>${escapeHtml(item.categoryName)}</span>
                            <span class="chart-legend-amount">${formatCurrency(item.totalAmount)} · ${formatPercentage(item.percentage)}</span>
                        </div>
                    `).join('');
                }
            }
        }
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

    function getSelectedValues(selectId) {
        const element = document.getElementById(selectId);
        if (!element) return [];
        if (element.tagName === 'SELECT') {
            return Array.from(element.selectedOptions || []).map(option => option.value).filter(Boolean);
        }
        return Array.from(element.querySelectorAll('input[type="checkbox"]:checked'))
            .map(input => input.value)
            .filter(Boolean);
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

    function formatPercentage(value) {
        const num = Number(value || 0);
        return `${num.toFixed(1)}%`;
    }

    function formatDateInput(date) {
        if (!(date instanceof Date) || Number.isNaN(date.getTime())) {
            return '';
        }
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    function formatMonthLabel(period) {
        if (!period) return '';
        const [year, month] = period.split('-');
        const date = new Date(Number(year), Number(month) - 1, 1);
        return date.toLocaleDateString('ru-RU', { month: 'short', year: 'numeric' });
    }
})();
