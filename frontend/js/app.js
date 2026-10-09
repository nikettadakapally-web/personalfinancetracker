/**
 * Main Application Logic & Executive UI Event Controller
 */

document.addEventListener('DOMContentLoaded', () => {
  document.body.classList.toggle('dark-mode', localStorage.getItem('theme_mode') !== 'light');

  // Initialize Lucide Icons if loaded
  if (window.lucide) {
    lucide.createIcons();
  }

  // Initialize Charts
  window.financeCharts.init();

  // Initial UI Render
  renderDashboard();
  renderTransactions();
  renderGoals();
  renderSubscriptions();
  renderTaxSavings();
  renderInsights();
  setupChatbot();
  setupEventListeners();
});

let chatHistory = [];
let chatPending = false;

// Re-render all dashboard components
function renderDashboard() {
  const summary = window.financeDB.calculateSummary();
  const sym = window.financeDB.getCurrency();

  // Top KPIs
  const totalBalanceEl = document.getElementById('kpiTotalBalance');
  const monthlySpendingEl = document.getElementById('kpiMonthlySpending');
  const savingsEl = document.getElementById('kpiSavings');
  const savingsRateBadge = document.getElementById('kpiSavingsRateBadge');

  const hasTransactions = window.financeDB.data.transactions.length > 0;
  if (totalBalanceEl) {
    totalBalanceEl.textContent = hasTransactions ? `${sym} ${summary.totalBalance.toLocaleString()}` : 'â€”';
  }
  if (monthlySpendingEl) {
    monthlySpendingEl.textContent = hasTransactions ? `${sym} ${summary.totalExpense.toLocaleString()}` : 'â€”';
  }
  if (savingsEl) {
    savingsEl.textContent = hasTransactions ? `${sym} ${summary.savings.toLocaleString()}` : 'â€”';
  }
  if (savingsRateBadge) {
    savingsRateBadge.innerHTML = hasTransactions
      ? `<i data-lucide="trending-up"></i> ${summary.savingsRate}% Income Saved`
      : 'Add transactions to see your savings rate';
  }

  // Render Executive Wealth & Health Bar
  renderExecutiveWealthBar();
  renderGamification();
  renderMonthlyBudgets();

  // Update Account Balances in Overview List
  renderAccountsList();

  // Update 50/30/20 Rule Progress
  render50_30_20();

  // Re-render charts
  if (window.financeCharts) {
    window.financeCharts.updateAll();
  }
}

function renderGamification() {
  const localMetrics = window.financeDB.calculateGamification();
  const hasLocalTransactions = window.financeDB.data.transactions.length > 0;
  const metrics = window.backendGamification && !hasLocalTransactions
    ? window.backendGamification
    : localMetrics;
  const hasScoringData = metrics.savingsRate !== null || metrics.budgetAdherence !== null || metrics.debtToIncome !== null;
  const score = document.getElementById('budgetHealthScore');
  const scoreLabel = document.getElementById('healthScoreLabel');
  const streak = document.getElementById('noSpendStreak');
  const savings = document.getElementById('scoreSavings');
  const budget = document.getElementById('scoreBudget');
  const debt = document.getElementById('scoreDebt');
  const note = document.getElementById('gamificationDataNote');

  if (score) score.textContent = hasScoringData ? metrics.score : 'â€”';
  if (scoreLabel) {
    scoreLabel.textContent = !hasScoringData ? 'Add transactions or budgets to calculate your score'
      : metrics.score >= 80 ? 'Excellent money habits'
      : metrics.score >= 60 ? 'Good progress â€” keep it up'
        : metrics.score >= 40 ? 'Room to strengthen your habits' : 'A few changes can improve your score';
  }
  if (streak) streak.textContent = hasScoringData ? metrics.noSpendStreak : 'â€”';
  if (savings) savings.textContent = metrics.savingsRate === null ? 'Add income data' : `${metrics.savingsRate}%`;
  if (budget) budget.textContent = metrics.budgetAdherence === null ? 'Add budgets' : `${metrics.budgetAdherence}%`;
  if (debt) debt.textContent = metrics.debtToIncome === null ? 'Add income data' : `${metrics.debtToIncome}%`;
  if (note) {
    note.textContent = window.gamificationDataNote
      || 'Score weights: savings rate 40%, budget adherence 35%, debt-to-income 25%. Missing inputs are scored neutrally.';
  }
}

function renderMonthlyBudgets() {
  const list = document.getElementById('monthlyBudgetList');
  if (!list) return;

  const categories = window.financeDB.data.categories || [];
  const now = new Date();
  const monthKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  const monthLabel = now.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
  const spentByCategory = new Map();

  (window.financeDB.data.transactions || []).forEach(transaction => {
    if (transaction.type !== 'expense' || String(transaction.date || '').slice(0, 7) !== monthKey) return;
    const categoryId = String(transaction.category || '');
    spentByCategory.set(categoryId, (spentByCategory.get(categoryId) || 0) + (Number(transaction.amount) || 0));
  });

  const currency = window.financeDB.getCurrency();
  const budgetedCategories = categories.filter(category => Number(category.monthlyBudget) > 0);
  const totalSpent = budgetedCategories.reduce(
    (total, category) => total + (spentByCategory.get(String(category.id)) || 0),
    0
  );
  const totalBudget = budgetedCategories.reduce((total, category) => total + Number(category.monthlyBudget), 0);
  const spentTotal = document.getElementById('budgetSpentTotal');
  const remainingTotal = document.getElementById('budgetRemainingTotal');
  const monthElement = document.getElementById('budgetMonthLabel');

  if (monthElement) monthElement.textContent = `Tracking ${monthLabel}`;
  if (spentTotal) spentTotal.textContent = `${currency}${totalSpent.toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;
  if (remainingTotal) {
    remainingTotal.textContent = totalBudget > 0
      ? window.financeDB.formatMoney(totalBudget - totalSpent)
      : 'Set category limits';
    remainingTotal.classList.toggle('budget-over-limit', totalBudget > 0 && totalSpent > totalBudget);
  }

  list.innerHTML = categories.map(category => {
    const limit = Math.max(0, Number(category.monthlyBudget) || 0);
    const spent = spentByCategory.get(String(category.id)) || 0;
    const percentage = limit > 0 ? (spent / limit) * 100 : 0;
    const progress = Math.min(100, percentage);
    const overBudget = limit > 0 && spent > limit;
    const detail = limit === 0
      ? `${window.financeDB.formatMoney(spent)} spent Â· no limit set`
      : overBudget
        ? `${window.financeDB.formatMoney(spent)} spent Â· ${window.financeDB.formatMoney(spent - limit)} over`
        : `${window.financeDB.formatMoney(spent)} of ${window.financeDB.formatMoney(limit)} Â· ${Math.round(percentage)}%`;
    const label = escapeHtml(category.name);
    const categoryId = escapeHtml(String(category.id));

    return `
      <div class="monthly-budget-row${overBudget ? ' is-over-budget' : ''}">
        <div class="monthly-budget-heading">
          <span class="monthly-budget-name">
            <span class="monthly-budget-dot" style="--budget-color: ${category.color}"></span>
            ${label}
          </span>
          <label class="budget-limit-field">
            <span class="visually-hidden">Monthly limit for ${label}</span>
            <span>${escapeHtml(currency)}</span>
            <input name="budget-${categoryId}" type="number" min="0" step="1" value="${limit || ''}" placeholder="0" inputmode="numeric">
          </label>
        </div>
        <div class="monthly-budget-track" role="progressbar" aria-label="${label} budget used" aria-valuemin="0" aria-valuemax="${limit || Math.max(spent, 1)}" aria-valuenow="${Math.min(spent, limit || spent)}">
          <span style="width: ${progress}%; --budget-color: ${category.color}"></span>
        </div>
        <div class="monthly-budget-detail">${detail}</div>
      </div>
    `;
  }).join('');
}

// Render Executive Wealth & Financial Health Bar
function renderExecutiveWealthBar() {
  const summary = window.financeDB.calculateSummary();
  const sym = window.financeDB.getCurrency();
  const runway = window.financeDB.calculateRunway();
  const taxTotal = window.financeDB.getTaxSavingsTotal();

  const barLiquidAssets = document.getElementById('barLiquidAssets');
  const barRunwayMeta = document.getElementById('barRunwayMeta');
  const barEmergencyVal = document.getElementById('barEmergencyVal');
  const barEmergencyProgress = document.getElementById('barEmergencyProgress');
  const barTaxVal = document.getElementById('barTaxVal');
  const barTaxProgress = document.getElementById('barTaxProgress');

  const hasTransactions = window.financeDB.data.transactions.length > 0;
  if (barLiquidAssets) {
    barLiquidAssets.textContent = hasTransactions ? `${sym} ${summary.totalBalance.toLocaleString()}` : 'â€”';
  }
  if (barRunwayMeta) {
    barRunwayMeta.textContent = runway.months === null ? 'Add expenses to calculate your runway' : `${runway.months} Months Living Runway (${runway.status})`;
  }

  // Emergency Fund Goal (ID: g1)
  const emergencyGoal = window.financeDB.data.goals.find(g => /emergency/i.test(g.title));
  const emergencyPct = emergencyGoal && emergencyGoal.target > 0
    ? Math.min(100, Math.round((emergencyGoal.current / emergencyGoal.target) * 100))
    : 0;
  if (barEmergencyVal) {
    barEmergencyVal.textContent = emergencyGoal
      ? `${sym} ${emergencyGoal.current.toLocaleString()} / ${sym} ${emergencyGoal.target.toLocaleString()}`
      : 'Add an emergency goal';
  }
  const emergencyAction = document.getElementById('addEmergencyGoalBtn');
  if (emergencyAction) {
    emergencyAction.innerHTML = emergencyGoal
      ? '<i data-lucide="plus"></i> Add funds'
      : '<i data-lucide="plus"></i> Add emergency goal';
    if (window.lucide) lucide.createIcons({ nodes: [emergencyAction] });
  }
  if (barEmergencyProgress) {
    barEmergencyProgress.style.width = `${emergencyPct}%`;
  }

  // Tax 80C Progress (Limit â‚¹1,50,000)
  const taxLimit = 150000;
  const taxPct = Math.min(100, Math.round((taxTotal / taxLimit) * 100));
  if (barTaxVal) {
    barTaxVal.textContent = taxTotal
      ? `${sym} ${taxTotal.toLocaleString()} / ${sym} ${taxLimit.toLocaleString()}`
      : 'Add tax-saving entries';
  }
  if (barTaxProgress) {
    barTaxProgress.style.width = `${taxPct}%`;
  }
}

// Render Accounts summary cards
function renderAccountsList() {
  const container = document.getElementById('accountsListContainer');
  if (!container) return;

  const accounts = window.financeDB.data.accounts;
  const sym = window.financeDB.getCurrency();

  if (accounts.length === 0) {
    container.innerHTML = '<div class="empty-state"><i data-lucide="wallet"></i><p>Account balances will appear here as you add transactions.</p></div>';
    if (window.lucide) lucide.createIcons();
    return;
  }

  container.innerHTML = accounts.map(acc => `
    <div class="account-card" style="border-left: 4px solid ${acc.color};">
      <div class="account-info">
        <div class="account-icon-wrap" style="background: ${acc.color}15; color: ${acc.color};">
          <i data-lucide="${acc.icon || 'credit-card'}"></i>
        </div>
        <div>
          <h4 class="account-name">${acc.name}</h4>
          <span class="account-type-badge">${acc.type.toUpperCase()}</span>
        </div>
      </div>
      <div class="account-balance ${acc.balance < 0 ? 'negative' : ''}">
        ${sym} ${acc.balance.toLocaleString()}
      </div>
    </div>
  `).join('');

  if (window.lucide) lucide.createIcons();
}

// Render 50/30/20 breakdown
function render50_30_20() {
  const b = window.aiAdvisor.get50_30_20_Breakdown();
  const sym = window.financeDB.getCurrency();

  const needsBar = document.getElementById('ruleNeedsBar');
  const wantsBar = document.getElementById('ruleWantsBar');
  const savingsBar = document.getElementById('ruleSavingsBar');

  const needsVal = document.getElementById('ruleNeedsVal');
  const wantsVal = document.getElementById('ruleWantsVal');
  const savingsVal = document.getElementById('ruleSavingsVal');

  if (needsBar) needsBar.style.width = `${Math.min(100, b.needs.pct)}%`;
  if (wantsBar) wantsBar.style.width = `${Math.min(100, b.wants.pct)}%`;
  if (savingsBar) savingsBar.style.width = `${Math.min(100, b.savings.pct)}%`;

  if (needsVal) needsVal.textContent = b.needs.pct === null ? 'Add income to calculate' : `${sym}${Math.round(b.needs.actual).toLocaleString()} (${b.needs.pct}% / 50%)`;
  if (wantsVal) wantsVal.textContent = b.wants.pct === null ? 'Add income to calculate' : `${sym}${Math.round(b.wants.actual).toLocaleString()} (${b.wants.pct}% / 30%)`;
  if (savingsVal) savingsVal.textContent = b.savings.pct === null ? 'Add income to calculate' : `${sym}${Math.round(b.savings.actual).toLocaleString()} (${b.savings.pct}% / 20%)`;
}

// Render Transactions List
function renderTransactions(filterCategory = 'all', searchQuery = '') {
  const container = document.getElementById('recentTransactionsList');
  if (!container) return;

  let txs = window.financeDB.getTransactions();
  const sym = window.financeDB.getCurrency();

  if (filterCategory !== 'all') {
    txs = txs.filter(t => t.category === filterCategory);
  }
  if (searchQuery) {
    const q = searchQuery.toLowerCase();
    txs = txs.filter(t => t.title.toLowerCase().includes(q) || t.merchant.toLowerCase().includes(q) || t.note.toLowerCase().includes(q));
  }

  if (txs.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <i data-lucide="receipt"></i>
        <p>${window.financeDB.data.transactions.length ? 'No transactions found for this filter.' : 'No transactions yet. Add one to start your ledger.'}</p>
      </div>
    `;
    if (window.lucide) lucide.createIcons();
    return;
  }

  container.innerHTML = txs.map(tx => {
    const cat = window.financeDB.getCategory(tx.category);
    const isIncome = tx.type === 'income';
    const isDiscretionary = !isIncome && window.financeDB.isDiscretionaryTransaction(tx);
    const amountPrefix = isIncome ? '+' : '-';
    const amountClass = isIncome ? 'income-amount' : 'expense-amount';
    const dateFormatted = new Date(tx.date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });

    return `
      <div class="tx-item" data-id="${tx.id}">
        <div class="tx-left">
          <div class="tx-cat-icon" style="background-color: ${cat.color}20; color: ${cat.color};">
            <i data-lucide="${cat.icon || 'tag'}"></i>
          </div>
          <div class="tx-details">
            <div class="tx-title-wrap">
              <span class="tx-title">${escapeHtml(tx.title)}</span>
              ${tx.isRecurring ? '<span class="recurring-tag"><i data-lucide="repeat"></i> Auto</span>' : ''}
              ${isDiscretionary ? '<span class="discretionary-tag">Discretionary</span>' : ''}
            </div>
            <span class="tx-subtitle">${cat.name} â€¢ ${dateFormatted}</span>
          </div>
        </div>
        <div class="tx-right">
          <span class="tx-amount ${amountClass}">${amountPrefix} ${sym} ${tx.amount.toLocaleString()}</span>
          <button class="tx-delete-btn" onclick="handleDeleteTx('${tx.id}')" title="Delete transaction">
            <i data-lucide="trash-2"></i>
          </button>
        </div>
      </div>
    `;
  }).join('');

  if (window.lucide) lucide.createIcons();
}

// Render Goals List
function renderGoals() {
  const container = document.getElementById('goalsListContainer');
  if (!container) return;

  const goals = window.financeDB.data.goals || [];
  const sym = window.financeDB.getCurrency();
  const metrics = window.financeDB.calculateGamification();
  const incompleteGoals = goals.filter(goal => Number(goal.current) < Number(goal.target)).length;
  const monthlyContribution = incompleteGoals ? metrics.monthlySavings / incompleteGoals : 0;

  if (goals.length === 0) {
    container.innerHTML = '<div class="empty-state"><i data-lucide="target"></i><p>No goals yet. Add one when you are ready to track a target.</p></div>';
    if (window.lucide) lucide.createIcons();
    return;
  }

  container.innerHTML = goals.map(g => {
    const target = Number(g.target) || 0;
    const current = Number(g.current) || 0;
    const pct = target > 0 ? Math.min(100, Math.round((current / target) * 100)) : 0;
    const remaining = Math.max(0, target - current);
    const hasLocalTransactions = window.financeDB.data.transactions.length > 0;
    const backendGoal = hasLocalTransactions
      ? null
      : (window.backendGamification?.goals || []).find(goal => goal.title === g.title);
    const estimate = backendGoal ? backendGoal.estimatedCompletionDate
      : remaining === 0 ? new Date().toISOString().slice(0, 10)
        : monthlyContribution > 0
          ? (() => {
            const date = new Date();
            date.setDate(1);
            date.setMonth(date.getMonth() + Math.ceil(remaining / monthlyContribution));
            return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-01`;
          })()
          : null;
    const estimateLabel = estimate
      ? new Date(`${estimate}T00:00:00`).toLocaleDateString('en-US', { month: 'short', year: 'numeric' })
      : 'Add savings to estimate';
    return `
      <div class="goal-card">
        <div class="goal-header">
          <div class="goal-title-wrap">
            <div class="goal-icon" style="background: ${g.color}15; color: ${g.color};">
              <i data-lucide="${g.icon || 'target'}"></i>
            </div>
            <div>
              <h4 class="goal-title">${escapeHtml(g.title)}</h4>
              <span class="goal-deadline">Deadline: ${new Date(`${g.deadline}T00:00:00`).toLocaleDateString('en-US', { month: 'short', year: 'numeric' })} Â· Est. ${estimateLabel}</span>
            </div>
          </div>
          <button type="button" class="btn-sm btn-outline goal-deposit-btn" data-goal-id="${g.id}">
            <i data-lucide="plus"></i> Deposit
          </button>
        </div>
        <div class="goal-progress-wrap">
          <div class="goal-amounts">
            <span>${sym} ${current.toLocaleString()}</span>
            <span class="goal-target">${sym} ${target.toLocaleString()} (${pct}%)</span>
          </div>
          <div class="progress-bar-bg">
            <div class="progress-bar-fill" style="width: ${pct}%; background: ${g.color};"></div>
          </div>
        </div>
      </div>
    `;
  }).join('');

  if (window.lucide) lucide.createIcons();
}

// Render Subscriptions Tracker
function renderSubscriptions() {
  const container = document.getElementById('subsListContainer');
  if (!container) return;

  const subs = window.financeDB.data.subscriptions || [];
  const sym = window.financeDB.getCurrency();

  if (subs.length === 0) {
    container.innerHTML = '<div class="empty-state"><i data-lucide="repeat"></i><p>No subscriptions added yet.</p></div>';
    if (window.lucide) lucide.createIcons();
    return;
  }

  container.innerHTML = subs.map(s => `
    <div class="sub-item ${s.active ? '' : 'sub-paused'}">
      <div class="sub-info">
        <h4 class="sub-name">${escapeHtml(s.name)}</h4>
        <span class="sub-freq">${s.frequency} â€¢ Next: ${s.nextDue}</span>
        <span class="sub-usage-pill ${s.usageRate.includes('Unused') || s.usageRate.includes('Low') ? 'pill-warning' : 'pill-info'}">
          ${escapeHtml(s.usageRate)}
        </span>
      </div>
      <div class="sub-action">
        <span class="sub-cost">${sym} ${s.amount.toLocaleString()}</span>
        <button type="button" class="btn-sub-toggle" onclick="toggleSubscription('${s.id}')" aria-pressed="${Boolean(s.active)}">
          ${s.active ? 'Active' : 'Paused'}
        </button>
      </div>
    </div>
  `).join('');
}

// Render Tax Savings Sec 80C
function renderTaxSavings() {
  const container = document.getElementById('taxSavingsListContainer');
  if (!container) return;

  const taxItems = window.financeDB.getTaxSavingsList();
  const sym = window.financeDB.getCurrency();

  if (taxItems.length === 0) {
    container.innerHTML = '<div class="empty-state"><i data-lucide="file-check-2"></i><p>No tax-saving entries yet.</p></div>';
    if (window.lucide) lucide.createIcons();
    return;
  }

  container.innerHTML = taxItems.map(t => `
    <div class="tax-item">
      <div>
        <div class="tax-title">${escapeHtml(t.name)}</div>
        <div class="tax-category">${t.category}</div>
      </div>
      <div class="tax-amount">${sym} ${t.amount.toLocaleString()}</div>
    </div>
  `).join('');
}

// Render Dashboard AI Insights Card
function renderInsights() {
  const container = document.getElementById('aiInsightsList');
  if (!container) return;

  const insights = window.aiAdvisor.generateDashboardInsights();

  if (insights.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <i data-lucide="sparkles"></i>
        <p>Add transactions, budgets, or subscriptions to see personalized insights.</p>
        <button type="button" class="btn-sm btn-outline insight-chat-btn">Chat with the advisor</button>
      </div>
    `;
    if (window.lucide) lucide.createIcons();
    return;
  }

  container.innerHTML = insights.map(ins => `
    <div class="insight-item insight-${ins.type}" role="button" tabindex="0" data-prompt="${ins.actionPrompt}">
      <div class="insight-icon">
        <i data-lucide="${ins.icon}"></i>
      </div>
      <div class="insight-content">
        <h5 class="insight-title">${ins.title}</h5>
        <p class="insight-text">${ins.text}</p>
        <span class="insight-action-link">
          ${ins.actionText} â†’
        </span>
      </div>
    </div>
  `).join('');

  if (window.lucide) lucide.createIcons();
}

// Delete transaction handler
window.handleDeleteTx = function(id) {
  if (confirm('Delete this transaction?')) {
    window.financeDB.deleteTransaction(id);
    window.backendGamification = null;
    renderDashboard();
    renderTransactions();
    renderInsights();
    showToast('Transaction removed', 'info');
  }
};

// Deposit into Savings Goal
window.openDepositModal = function(goalId, goalTitle) {
  const amountStr = prompt(`Enter amount (â‚¹) to deposit into "${goalTitle}":`);
  if (amountStr) {
    const amount = parseFloat(amountStr);
    if (!isNaN(amount) && amount > 0) {
      window.financeDB.contributeToGoal(goalId, amount);
      renderGoals();
      renderDashboard();
      showToast(`Added â‚¹${amount.toLocaleString()} to ${goalTitle}! ðŸŽ¯`, 'success');
    }
  }
};

// Toggle Subscription Active / Paused
window.toggleSubscription = function(subId) {
  const sub = window.financeDB.data.subscriptions.find(s => s.id === subId);
  if (sub) {
    sub.active = !sub.active;
    window.financeDB.saveData();
    renderSubscriptions();
    renderDashboard();
    renderInsights();
    showToast(`${sub.name} is now ${sub.active ? 'Active' : 'Paused'}`, 'info');
    if (sub.backendId) {
      const token = sessionStorage.getItem('finance_auth_token');
      fetch(`${window.FINANCE_API_BASE_URL || 'http://localhost:8081/api'}/subscriptions/${sub.backendId}/active?active=${sub.active}`, {
        method: 'PUT', /*
        headers: token ? { Authorization: `Bearer ${token}` } : {}
        */
        headers: { Authorization: 'Bearer ' + token }
      }).then(response => {
        if (!response.ok) throw new Error(`Subscription update failed (${response.status}).`);
      }).catch(error => {
        console.warn('Spring Boot subscription status sync failed:', error.message);
        showToast('Saved on this device, but the backend did not update.', 'info');
      });
    }
  }
};

// Global Toast Notifications
function showToast(message, type = 'success') {
  let toastContainer = document.getElementById('toastContainer');
  if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.id = 'toastContainer';
    toastContainer.className = 'toast-container';
    document.body.appendChild(toastContainer);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  const icon = document.createElement('i');
  icon.setAttribute('data-lucide', type === 'success' ? 'check-circle' : 'info');
  const text = document.createElement('span');
  text.textContent = message;
  toast.append(icon, text);
  toastContainer.appendChild(toast);
  if (window.lucide) lucide.createIcons();

  setTimeout(() => {
    toast.classList.add('toast-fade');
    setTimeout(() => toast.remove(), 300);
  }, 3200);
}

// Render Executive Financial Audit Report Modal
function openReportModal() {
  const modal = document.getElementById('reportModal');
  const body = document.getElementById('reportModalBody');
  if (!modal || !body) return;

  const summary = window.financeDB.calculateSummary();
  const sym = window.financeDB.getCurrency();
  const runway = window.financeDB.calculateRunway();
  const taxTotal = window.financeDB.getTaxSavingsTotal();
  const b50 = window.aiAdvisor.get50_30_20_Breakdown();
  const hasTransactions = window.financeDB.data.transactions.length > 0;

  body.innerHTML = `
    <div class="report-container">
      <div class="report-section">
        <div class="report-section-title">Portfolio Overview</div>
        <div class="report-grid">
          <div class="report-box">
            <div class="report-box-label">Total Liquid Capital</div>
            <div class="report-box-val">${hasTransactions ? `${sym} ${summary.totalBalance.toLocaleString()}` : 'â€”'}</div>
          </div>
          <div class="report-box">
            <div class="report-box-label">Monthly Burn Rate</div>
            <div class="report-box-val">${hasTransactions ? `${sym} ${summary.totalExpense.toLocaleString()}` : 'â€”'}</div>
          </div>
          <div class="report-box">
            <div class="report-box-label">Emergency Runway</div>
            <div class="report-box-val" style="color: ${runway.color}">${runway.months === null ? 'â€”' : `${runway.months} Months`}</div>
          </div>
          <div class="report-box">
            <div class="report-box-label">Tax 80C Deductions</div>
            <div class="report-box-val">${sym} ${taxTotal.toLocaleString()}</div>
          </div>
        </div>
      </div>

      <div class="report-section">
        <div class="report-section-title">50 / 30 / 20 Budget Ratio Breakdown</div>
        <table class="report-table">
          <thead>
            <tr>
              <th>Bucket</th>
              <th>Actual Spend</th>
              <th>Target Cap</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Needs (Rent, Bills, Food)</td>
              <td>${b50.needs.pct === null ? 'Add income data' : `${sym} ${Math.round(b50.needs.actual).toLocaleString()} (${b50.needs.pct}%)`}</td>
              <td>50%</td>
              <td>${b50.needs.pct === null ? 'â€”' : b50.needs.pct <= 50 ? 'âœ… Healthy' : 'âš ï¸ Exceeded'}</td>
            </tr>
            <tr>
              <td>Wants (Shopping, Dining)</td>
              <td>${b50.wants.pct === null ? 'Add income data' : `${sym} ${Math.round(b50.wants.actual).toLocaleString()} (${b50.wants.pct}%)`}</td>
              <td>30%</td>
              <td>${b50.wants.pct === null ? 'â€”' : b50.wants.pct <= 30 ? 'âœ… Healthy' : 'âš ï¸ Exceeded'}</td>
            </tr>
            <tr>
              <td>Savings & SIPs</td>
              <td>${b50.savings.pct === null ? 'Add income data' : `${sym} ${Math.round(b50.savings.actual).toLocaleString()} (${b50.savings.pct}%)`}</td>
              <td>Min 20%</td>
              <td>${b50.savings.pct === null ? 'â€”' : b50.savings.pct >= 20 ? 'ðŸ† Strong' : 'âš ï¸ Needs Push'}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="report-section">
        <div class="report-section-title">AI Executive Summary</div>
        <p style="font-size: 13px; color: var(--text-secondary); line-height: 1.6;">
          ${hasTransactions ? `Your recorded transactions show ${sym} ${summary.totalIncome.toLocaleString()} in income and ${sym} ${summary.totalExpense.toLocaleString()} in expenses.` : 'Add transactions to create a report based on your own financial activity.'}
          ${taxTotal ? ` You have logged ${sym} ${taxTotal.toLocaleString()} in tax-saving entries.` : ' No tax-saving entries have been added.'}
        </p>
      </div>
    </div>
  `;

  modal.classList.add('active');
  if (window.lucide) lucide.createIcons();
}

// Trigger chatbot with an automated prompt
window.triggerAdvisorChat = function(promptText) {
  const chatDrawer = document.getElementById('aiChatbotWidget');
  if (chatDrawer && chatDrawer.classList.contains('minimized')) {
    chatDrawer.classList.remove('minimized');
  }
  handleUserChatSubmit(promptText);
};

// Setup AI Chatbot Widget
function setupChatbot() {
  const chatWidget = document.getElementById('aiChatbotWidget');
  const chatToggleBtn = document.getElementById('chatToggleBtn');
  const chatCloseBtn = document.getElementById('chatCloseBtn');
  const chatForm = document.getElementById('chatInputForm');
  const chatInput = document.getElementById('chatInput');

  if (chatToggleBtn && chatWidget) {
    chatToggleBtn.addEventListener('click', () => {
      chatWidget.classList.toggle('minimized');
      if (!chatWidget.classList.contains('minimized') && chatInput) {
        chatInput.focus();
      }
    });
  }

  if (chatCloseBtn && chatWidget) {
    chatCloseBtn.addEventListener('click', () => {
      chatWidget.classList.add('minimized');
    });
  }

  if (chatForm) {
    chatForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const text = chatInput.value.trim();
      if (text) {
        chatInput.value = '';
        handleUserChatSubmit(text);
      }
    });
  }

  const defaultChips = [
    'How is your day?',
    'Tell me a joke',
    "I'm bored",
    'Show my financial summary'
  ];

  renderChatChips(defaultChips);
}

function renderChatChips(chips) {
  const container = document.getElementById('chatQuickChips');
  if (!container) return;

  container.replaceChildren();
  chips.forEach(chip => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'chat-chip';
    button.textContent = chip;
    button.addEventListener('click', () => window.handleUserChatSubmit(chip));
    container.appendChild(button);
  });
}

// Handle User Chat submission & typing response
window.handleUserChatSubmit = async function(userMessage) {
  const messagesContainer = document.getElementById('chatMessages');
  if (!messagesContainer || !userMessage || chatPending) return;

  const userBubble = document.createElement('div');
  userBubble.className = 'chat-message user-msg';
  userBubble.innerHTML = `<div class="msg-bubble">${escapeHtml(userMessage)}</div>`;
  messagesContainer.appendChild(userBubble);
  messagesContainer.scrollTop = messagesContainer.scrollHeight;

  const typingIndicator = document.createElement('div');
  typingIndicator.className = 'chat-message bot-msg typing-msg';
  typingIndicator.innerHTML = `
    <div class="bot-avatar"><img class="ai-bot-logo" src="assets/ai-bot-logo.svg" alt=""></div>
    <div class="msg-bubble typing-dots">
      <span></span><span></span><span></span>
    </div>
  `;
  messagesContainer.appendChild(typingIndicator);
  if (window.lucide) lucide.createIcons();
  messagesContainer.scrollTop = messagesContainer.scrollHeight;

  const chatInput = document.getElementById('chatInput');
  const sendButton = document.querySelector('#chatInputForm button[type="submit"]');
  chatPending = true;
  if (chatInput) chatInput.disabled = true;
  if (sendButton) sendButton.disabled = true;
  try {
    const response = await window.askAIAdvisor(userMessage, chatHistory);
    chatHistory.push({ role: 'user', content: userMessage }, { role: 'assistant', content: response.reply });
    chatHistory = chatHistory.slice(-40);
    const botBubble = document.createElement('div');
    botBubble.className = 'chat-message bot-msg';
    botBubble.innerHTML = `
      <div class="bot-avatar"><img class="ai-bot-logo" src="assets/ai-bot-logo.svg" alt=""></div>
      <div class="msg-bubble">
        <div class="bot-text">${formatMarkdown(response.reply)}</div>
      </div>
    `;
    messagesContainer.appendChild(botBubble);
    if (window.lucide) lucide.createIcons();
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
  } catch (error) {
    const errorBubble = document.createElement('div');
    errorBubble.className = 'chat-message bot-msg';
    errorBubble.innerHTML = `
      <div class="bot-avatar"><img class="ai-bot-logo" src="assets/ai-bot-logo.svg" alt=""></div>
      <div class="msg-bubble"><div class="bot-text">I couldn't reach the advisor: ${escapeHtml(error.message)} Please try again.</div></div>
    `;
    messagesContainer.appendChild(errorBubble);
    if (window.lucide) lucide.createIcons();
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
  } finally {
    typingIndicator.remove();
    chatPending = false;
    if (chatInput) chatInput.disabled = false;
    if (sendButton) sendButton.disabled = false;
  }
};

function formatMarkdown(text) {
  return escapeHtml(text)
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/_(.*?)_/g, '<em>$1</em>')
    .replace(/â€¢ (.*?)(?=\n|$)/g, '<div class="list-bullet"><span class="dot">â€¢</span> <span>$1</span></div>')
    .replace(/\n\n/g, '<br><br>')
    .replace(/\n/g, '<br>');
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// Setup Event Listeners (Modals, Filters, Forms)
function setupEventListeners() {
  // 1. Add Transaction Modal
  const openAddTxModalBtn = document.getElementById('openAddTxModalBtn');
  const closeAddTxModalBtn = document.getElementById('closeAddTxModalBtn');
  const addTxModal = document.getElementById('addTxModal');
  const addTxForm = document.getElementById('addTxForm');
  const txType = document.getElementById('txType');
  const txCategory = document.getElementById('txCategory');
  const discretionaryField = document.getElementById('discretionaryField');
  const txDiscretionary = document.getElementById('txDiscretionary');
  const updateDiscretionaryField = () => {
    const isExpense = txType.value === 'expense';
    discretionaryField.hidden = !isExpense;
    if (!isExpense) txDiscretionary.checked = false;
  };

  const openTransactionModal = () => {
    if (!addTxModal) return;
    addTxModal.classList.add('active');
    updateDiscretionaryField();
  };

  if (openAddTxModalBtn && addTxModal) {
    openAddTxModalBtn.addEventListener('click', openTransactionModal);
  }
  const addWealthTransactionBtn = document.getElementById('addWealthTransactionBtn');
  if (addWealthTransactionBtn) addWealthTransactionBtn.addEventListener('click', openTransactionModal);
  txType.addEventListener('change', updateDiscretionaryField);
  if (closeAddTxModalBtn && addTxModal) {
    closeAddTxModalBtn.addEventListener('click', () => addTxModal.classList.remove('active'));
  }

  if (addTxForm) {
    addTxForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const title = document.getElementById('txTitle').value.trim();
      const amount = parseFloat(document.getElementById('txAmount').value);
      const type = document.getElementById('txType').value;
      const category = document.getElementById('txCategory').value;
      const account = document.getElementById('txAccount').value;
      const date = document.getElementById('txDate').value;
      const note = document.getElementById('txNote').value.trim();
      const isRecurring = document.getElementById('txRecurring').checked;
      const discretionary = type === 'expense' && txDiscretionary.checked;

      if (!title || isNaN(amount) || amount <= 0) return;

      window.financeDB.addTransaction({ title, amount, type, category, account, date, note, isRecurring, discretionary });
      window.backendGamification = null;
      addTxForm.reset();
      addTxModal.classList.remove('active');
      renderDashboard();
      renderTransactions();
      renderInsights();
      showToast('Transaction recorded successfully!', 'success');
    });
  }

  // 2. Add Goal Modal
  const openAddGoalModalBtn = document.getElementById('openAddGoalModalBtn');
  const openAddGoalBtn2 = document.getElementById('openAddGoalBtn2');
  const closeAddGoalModalBtn = document.getElementById('closeAddGoalModalBtn');
  const addGoalModal = document.getElementById('addGoalModal');
  const addGoalForm = document.getElementById('addGoalForm');

  const openGoalModal = () => {
    if (addGoalModal) {
      addGoalModal.classList.add('active');
    }
  };

  if (openAddGoalModalBtn) openAddGoalModalBtn.addEventListener('click', openGoalModal);
  if (openAddGoalBtn2) openAddGoalBtn2.addEventListener('click', openGoalModal);
  const addEmergencyGoalBtn = document.getElementById('addEmergencyGoalBtn');
  if (addEmergencyGoalBtn) {
    addEmergencyGoalBtn.addEventListener('click', () => {
      const emergencyGoal = window.financeDB.data.goals.find(goal => /emergency/i.test(goal.title));
      if (emergencyGoal) {
        window.openDepositModal(emergencyGoal.id, emergencyGoal.title);
      } else {
        openGoalModal();
      }
    });
  }
  if (closeAddGoalModalBtn && addGoalModal) {
    closeAddGoalModalBtn.addEventListener('click', () => addGoalModal.classList.remove('active'));
  }

  if (addGoalForm) {
    addGoalForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const title = document.getElementById('goalTitle').value.trim();
      const target = parseFloat(document.getElementById('goalTarget').value);
      const current = parseFloat(document.getElementById('goalCurrent').value || 0);
      const deadline = document.getElementById('goalDeadline').value;

      if (!title || isNaN(target) || target <= 0) return;

      window.financeDB.addGoal({ title, target, current, deadline });
      window.backendGamification = null;
      addGoalForm.reset();
      addGoalModal.classList.remove('active');
      renderGoals();
      renderDashboard();
      showToast(`Goal "${title}" created! ðŸŽ¯`, 'success');
    });
  }

  // 3. Add Subscription Modal
  const openAddSubModalBtn = document.getElementById('openAddSubModalBtn');
  const closeAddSubModalBtn = document.getElementById('closeAddSubModalBtn');
  const addSubModal = document.getElementById('addSubModal');
  const addSubForm = document.getElementById('addSubForm');

  if (openAddSubModalBtn && addSubModal) {
    openAddSubModalBtn.addEventListener('click', () => addSubModal.classList.add('active'));
  }
  if (closeAddSubModalBtn && addSubModal) {
    closeAddSubModalBtn.addEventListener('click', () => addSubModal.classList.remove('active'));
  }

  if (addSubForm) {
    addSubForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const name = document.getElementById('subName').value.trim();
      const amount = parseFloat(document.getElementById('subAmount').value);
      const frequency = document.getElementById('subFrequency').value;
      const usageRate = document.getElementById('subUsageRate').value;
      const nextDue = document.getElementById('subNextDue').value;
      const category = document.getElementById('subCategory').value;

      if (!name || isNaN(amount) || amount <= 0) return;

      window.financeDB.addSubscription({ name, amount, frequency, usageRate, nextDue, category });
      addSubForm.reset();
      addSubModal.classList.remove('active');
      renderSubscriptions();
      renderDashboard();
      showToast(`Subscription "${name}" added! ðŸ”„`, 'success');
    });
  }

  // 4. Add Tax Saving Modal
  const openAddTaxModalBtn = document.getElementById('openAddTaxModalBtn');
  const openAddTaxModalBtn2 = document.getElementById('openAddTaxModalBtn2');
  const closeAddTaxModalBtn = document.getElementById('closeAddTaxModalBtn');
  const addTaxModal = document.getElementById('addTaxModal');
  const addTaxForm = document.getElementById('addTaxForm');

  const openTaxModal = () => {
    if (addTaxModal) addTaxModal.classList.add('active');
  };

  if (openAddTaxModalBtn) openAddTaxModalBtn.addEventListener('click', openTaxModal);
  if (openAddTaxModalBtn2) openAddTaxModalBtn2.addEventListener('click', openTaxModal);
  if (closeAddTaxModalBtn && addTaxModal) {
    closeAddTaxModalBtn.addEventListener('click', () => addTaxModal.classList.remove('active'));
  }

  if (addTaxForm) {
    addTaxForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const name = document.getElementById('taxName').value.trim();
      const amount = parseFloat(document.getElementById('taxAmount').value);
      const category = document.getElementById('taxCategory').value;

      if (!name || isNaN(amount) || amount <= 0) return;

      window.financeDB.addTaxSaving({ name, amount, category });
      addTaxForm.reset();
      addTaxModal.classList.remove('active');
      renderTaxSavings();
      renderExecutiveWealthBar();
      showToast(`Logged â‚¹${amount.toLocaleString()} in ${category}! ðŸ“„`, 'success');
    });
  }

  // 5. Executive Audit Report Modal
  const openReportModalBtn = document.getElementById('openReportModalBtn');
  const closeReportModalBtn = document.getElementById('closeReportModalBtn');
  const closeReportBtn2 = document.getElementById('closeReportBtn2');
  const reportModal = document.getElementById('reportModal');
  const printReportBtn = document.getElementById('printReportBtn');
  const insightsContainer = document.getElementById('aiInsightsList');
  const goalsContainer = document.getElementById('goalsListContainer');

  if (openReportModalBtn) {
    openReportModalBtn.addEventListener('click', openReportModal);
  }
  if (closeReportModalBtn && reportModal) {
    closeReportModalBtn.addEventListener('click', () => reportModal.classList.remove('active'));
  }
  if (closeReportBtn2 && reportModal) {
    closeReportBtn2.addEventListener('click', () => reportModal.classList.remove('active'));
  }
  if (printReportBtn) printReportBtn.addEventListener('click', () => window.print());
  if (insightsContainer) {
    insightsContainer.addEventListener('click', event => {
      if (event.target.closest('.insight-chat-btn')) window.triggerAdvisorChat('What can you help me with?');
      const insight = event.target.closest('.insight-item');
      if (insight) window.triggerAdvisorChat(insight.dataset.prompt);
    });
    insightsContainer.addEventListener('keydown', event => {
      if (event.target.matches('.insight-item') && (event.key === 'Enter' || event.key === ' ')) {
        event.preventDefault();
        window.triggerAdvisorChat(event.target.dataset.prompt);
      }
    });
  }
  if (goalsContainer) {
    goalsContainer.addEventListener('click', event => {
      const button = event.target.closest('.goal-deposit-btn');
      if (!button) return;
      const goal = window.financeDB.data.goals.find(item => item.id === button.dataset.goalId);
      if (goal) window.openDepositModal(goal.id, goal.title);
    });
  }

  document.querySelectorAll('.modal-overlay').forEach(modal => {
    modal.addEventListener('click', event => {
      if (event.target === modal) modal.classList.remove('active');
    });
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape') {
      document.querySelectorAll('.modal-overlay.active').forEach(modal => modal.classList.remove('active'));
    }
  });

  // Transaction Filters & Search
  const txFilterSelect = document.getElementById('txCategoryFilter');
  const txSearchInput = document.getElementById('txSearchInput');

  if (txFilterSelect) {
    txFilterSelect.addEventListener('change', (e) => {
      renderTransactions(e.target.value, txSearchInput ? txSearchInput.value : '');
    });
  }
  if (txSearchInput) {
    txSearchInput.addEventListener('input', (e) => {
      const category = txFilterSelect ? txFilterSelect.value : 'all';
      renderTransactions(category, e.target.value);
    });
  }

  const monthlyBudgetForm = document.getElementById('monthlyBudgetForm');
  if (monthlyBudgetForm) {
    monthlyBudgetForm.addEventListener('submit', event => {
      event.preventDefault();
      const formData = new FormData(monthlyBudgetForm);
      const updates = window.financeDB.data.categories.map(category => ({
        category,
        value: Number(formData.get(`budget-${category.id}`))
      }));
      if (updates.some(({ value }) => !Number.isFinite(value) || value < 0)) {
        showToast('Enter a valid non-negative limit for each category.', 'info');
        return;
      }

      updates.forEach(({ category, value }) => {
        category.monthlyBudget = value;
      });
      window.financeDB.saveData();
      renderMonthlyBudgets();
      renderGamification();
      renderInsights();
      showToast('Monthly budgets saved.', 'success');
    });
  }

  // Export CSV Button
  const exportBtn = document.getElementById('exportCsvBtn');
  if (exportBtn) {
    exportBtn.addEventListener('click', exportTransactionsCSV);
  }

  // Dark/Light Theme Toggle
  const themeToggleBtn = document.getElementById('themeToggleBtn');
  if (themeToggleBtn) {
    themeToggleBtn.addEventListener('click', () => {
      document.body.classList.toggle('dark-mode');
      const isDark = document.body.classList.contains('dark-mode');
      localStorage.setItem('theme_mode', isDark ? 'dark' : 'light');
      window.financeCharts.updateAll();
    });
  }
}

// Export CSV utility
function exportTransactionsCSV() {
  const txs = window.financeDB.getTransactions();
  if (txs.length === 0) {
    alert('No transactions to export.');
    return;
  }

  let csvContent = 'data:text/csv;charset=utf-8,';
  csvContent += 'ID,Date,Title,Category,Type,Amount (INR),Account,Note,IsRecurring,Discretionary\r\n';

  txs.forEach(t => {
    const row = [
      t.id,
      t.date,
      `"${t.title.replace(/"/g, '""')}"`,
      t.category,
      t.type,
      t.amount,
      t.account,
      `"${(t.note || '').replace(/"/g, '""')}"`,
      t.isRecurring,
      window.financeDB.isDiscretionaryTransaction(t)
    ].join(',');
    csvContent += row + '\r\n';
  });

  const encodedUri = encodeURI(csvContent);
  const link = document.createElement('a');
  link.setAttribute('href', encodedUri);
  link.setAttribute('download', `Executive_Expense_Audit_${new Date().toISOString().split('T')[0]}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  showToast('Exported Executive Financial Audit CSV!', 'success');
}

