const API_BASE_URL = window.FINANCE_API_BASE_URL || 'http://localhost:8081/api';
window.FINANCE_API_BASE_URL = API_BASE_URL;
const AUTH_TOKEN_KEY = 'finance_auth_token';
const AUTH_USER_KEY = 'finance_auth_user';

async function requestApi(path, options = {}) {
  const headers = new Headers(options.headers || {});
  const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (options.body) headers.set('Content-Type', 'application/json');

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  if (response.status === 204) return null;

  const contentType = response.headers.get('content-type') || '';
  const body = contentType.includes('application/json') ? await response.json() : null;
  if (!response.ok) {
    throw new Error(body?.detail || body?.message || body?.error || `Request failed (${response.status}).`);
  }
  return body;
}

window.askAIAdvisor = function(message, history = []) {
  return requestApi('/ai/chat', {
    method: 'POST',
    body: JSON.stringify({ message, history })
  });
};

function showAuthError(message) {
  const error = document.getElementById('authError');
  error.textContent = message || '';
  error.classList.toggle('visible', Boolean(message));
}

function showPaymentError(message) {
  const error = document.getElementById('paymentRequestError');
  error.textContent = message || '';
  error.classList.toggle('visible', Boolean(message));
}

window.refreshBackendGamification = async function(syncBudgets = false) {
  if (!sessionStorage.getItem(AUTH_TOKEN_KEY)) return;
  try {
    if (syncBudgets) {
      const budgets = window.financeDB.data.categories
        .filter(category => Number(category.monthlyBudget) > 0)
        .map(category => requestApi('/budgets', {
          method: 'POST',
          body: JSON.stringify({
            categoryKey: category.id,
            categoryName: category.name,
            monthlyBudget: Number(category.monthlyBudget),
            color: category.color,
            icon: category.icon
          })
        }));
      await Promise.all(budgets);
    }
    const goals = await requestApi('/goals');
    goals.forEach(savedGoal => {
      const localGoal = window.financeDB.data.goals.find(goal => goal.title === savedGoal.title);
      if (localGoal) localGoal.backendId = savedGoal.id;
    });
    const metrics = await requestApi('/dashboard/gamification');
    window.backendGamification = metrics;
    window.gamificationDataNote = window.financeDB.data.transactions.length
      ? 'Using saved dashboard transactions; backend metrics are refreshed when connected.'
      : 'Live score synced with your account. Missing inputs are scored neutrally.';
    renderDashboard();
    renderGoals();
  } catch (error) {
    window.backendGamification = null;
    window.gamificationDataNote = `Showing local calculations; backend sync unavailable: ${error.message}`;
    renderDashboard();
  }
};

function refreshAccountDashboard(user) {
  chatHistory = [];
  window.backendGamification = null;
  window.gamificationDataNote = 'Calculating from your saved transactions and budgets.';
  window.financeDB = new FinanceDataManager();
  window.financeDB.data.user.name = user.name;
  window.financeDB.saveData();
  if (window.aiAdvisor) window.aiAdvisor.db = window.financeDB;
  if (window.financeCharts) window.financeCharts.db = window.financeDB;
  renderDashboard();
  renderTransactions();
  renderGoals();
  renderSubscriptions();
  renderTaxSavings();
  renderInsights();
  if (window.financeCharts) window.financeCharts.updateAll();
  window.refreshBackendGamification(true);
}

function showDashboard(user) {
  document.body.classList.add('authenticated');
  document.getElementById('authScreen').hidden = true;
  document.getElementById('appContainer').hidden = false;
  document.getElementById('signedInLabel').textContent = user.email;
  const welcomeTitle = document.getElementById('welcomeTitle');
  if (welcomeTitle) {
    const firstName = (user.name || '').trim().split(/\s+/)[0];
    welcomeTitle.textContent = `Good morning${firstName ? `, ${firstName}` : ''}`;
  }
  refreshAccountDashboard(user);
  if (window.lucide) lucide.createIcons();
}

function storeSession(payload) {
  sessionStorage.setItem(AUTH_TOKEN_KEY, payload.token);
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(payload.user));
  showDashboard(payload.user);
}

function setupAuth() {
  const form = document.getElementById('authForm');
  const nameField = document.getElementById('authNameField');
  const nameInput = document.getElementById('authName');
  const passwordInput = document.getElementById('authPassword');
  const submitButton = document.getElementById('authSubmit');
  const modeToggle = document.getElementById('authModeToggle');
  let registering = false;

  modeToggle.addEventListener('click', () => {
    registering = !registering;
    nameField.hidden = !registering;
    nameInput.required = registering;
    passwordInput.autocomplete = registering ? 'new-password' : 'current-password';
    submitButton.textContent = registering ? 'Create account' : 'Sign in';
    modeToggle.textContent = registering ? 'I already have an account' : 'Create a new account';
    document.getElementById('authTitle').textContent = registering ? 'Start with a clear view.' : 'Your money, in focus.';
    showAuthError('');
  });

  form.addEventListener('submit', async event => {
    event.preventDefault();
    showAuthError('');
    submitButton.disabled = true;
    submitButton.textContent = registering ? 'Creating account…' : 'Signing in…';
    const payload = {
      name: nameInput.value.trim(),
      email: document.getElementById('authEmail').value.trim(),
      password: passwordInput.value
    };

    try {
      const result = await requestApi(registering ? '/auth/register' : '/auth/login', {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      storeSession(result);
    } catch (error) {
      showAuthError(error.message || 'Could not connect to the finance backend.');
    } finally {
      submitButton.disabled = false;
      submitButton.textContent = registering ? 'Create account' : 'Sign in';
    }
  });

  document.getElementById('logoutBtn').addEventListener('click', async () => {
    try {
      await requestApi('/auth/logout', { method: 'POST' });
    } catch (error) {
      console.warn('Sign-out request could not reach the backend:', error.message);
    }
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(AUTH_USER_KEY);
    chatHistory = [];
    document.body.classList.remove('authenticated');
    document.getElementById('appContainer').hidden = true;
    document.getElementById('authScreen').hidden = false;
    form.reset();
    showAuthError('');
  });

  const modal = document.getElementById('paymentRequestModal');
  const requestForm = document.getElementById('paymentRequestForm');
  const history = document.getElementById('paymentRequestHistory');
  const requestButton = document.getElementById('sendPaymentRequestBtn');

  async function loadPaymentHistory() {
    history.textContent = 'Loading…';
    try {
      const requests = await requestApi('/payment-requests');
      history.replaceChildren();
      if (!requests.length) {
        history.textContent = 'No requests yet.';
        return;
      }
      requests.slice(0, 5).forEach(item => {
        const row = document.createElement('div');
        row.className = 'payment-history-row';
        const recipient = document.createElement('span');
        recipient.textContent = `${item.recipientName} · ${item.recipientEmail}`;
        const amount = document.createElement('strong');
        amount.textContent = `₹${Number(item.amount).toLocaleString('en-IN')}`;
        row.append(recipient, amount);
        history.append(row);
      });
    } catch (error) {
      history.textContent = error.message;
    }
  }

  document.getElementById('openPaymentRequestBtn').addEventListener('click', () => {
    showPaymentError('');
    modal.classList.add('active');
    loadPaymentHistory();
  });
  document.getElementById('closePaymentRequestBtn').addEventListener('click', () => modal.classList.remove('active'));
  modal.addEventListener('click', event => {
    if (event.target === modal) modal.classList.remove('active');
  });

  requestForm.addEventListener('submit', async event => {
    event.preventDefault();
    showPaymentError('');
    requestButton.disabled = true;
    requestButton.textContent = 'Sending…';
    const payload = {
      recipientName: document.getElementById('paymentRecipientName').value.trim(),
      recipientEmail: document.getElementById('paymentRecipientEmail').value.trim(),
      amount: Number(document.getElementById('paymentAmount').value),
      note: document.getElementById('paymentNote').value.trim()
    };
    try {
      await requestApi('/payment-requests', { method: 'POST', body: JSON.stringify(payload) });
      requestForm.reset();
      if (window.showToast) showToast('Payment request emailed.', 'success');
      await loadPaymentHistory();
    } catch (error) {
      showPaymentError(error.message || 'Could not send the request.');
    } finally {
      requestButton.disabled = false;
      requestButton.innerHTML = '<i data-lucide="send"></i> Email request';
      if (window.lucide) lucide.createIcons();
    }
  });
}

document.addEventListener('DOMContentLoaded', async () => {
  setupAuth();
  const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
  if (!token) {
    document.getElementById('authScreen').hidden = false;
    if (window.lucide) lucide.createIcons();
    return;
  }
  try {
    const user = await requestApi('/auth/me');
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
    showDashboard(user);
  } catch (error) {
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(AUTH_USER_KEY);
    showAuthError(error.message);
    document.getElementById('authScreen').hidden = false;
  }
  if (window.lucide) lucide.createIcons();
});