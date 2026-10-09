// Personal Finance Data Store & State Management
const STORAGE_KEY = 'finance_tracker_data_v1';

const DEFAULT_DATA = {
  user: {
    name: 'Niket Anand',
    currency: '₹',
    monthlyIncome: 65000,
    savingsTargetRate: 20 // 20%
  },
  accounts: [
    { id: 'acc_bank', name: 'HDFC Bank Account', type: 'bank', balance: 34200, icon: 'landmark', color: '#2563eb' },
    { id: 'acc_card', name: 'ICICI Sapphiro Card', type: 'card', balance: -5800, limit: 100000, icon: 'credit-card', color: '#8b5cf6' },
    { id: 'acc_upi', name: 'Google Pay / UPI', type: 'upi', balance: 14120, icon: 'smartphone', color: '#06b6d4' },
    { id: 'acc_cash', name: 'Physical Cash', type: 'cash', balance: 5800, icon: 'wallet', color: '#10b981' }
  ],
  categories: [
    { id: 'food', name: 'Food & Dining', icon: 'utensils', color: '#f59e0b', monthlyBudget: 0 },
    { id: 'travel', name: 'Travel & Commute', icon: 'car', color: '#3b82f6', monthlyBudget: 0 },
    { id: 'shopping', name: 'Shopping', icon: 'shopping-bag', color: '#ec4899', monthlyBudget: 0 },
    { id: 'bills', name: 'Bills & Utilities', icon: 'zap', color: '#8b5cf6', monthlyBudget: 0 },
    { id: 'entertainment', name: 'Entertainment & Subs', icon: 'tv', color: '#ef4444', monthlyBudget: 0 },
    { id: 'health', name: 'Health & Wellness', icon: 'heart-pulse', color: '#10b981', monthlyBudget: 0 },
    { id: 'investment', name: 'Investments & Mutual Funds', icon: 'trending-up', color: '#14b8a6', monthlyBudget: 0 },
    { id: 'debt', name: 'Debt Payments', icon: 'landmark', color: '#f97316', monthlyBudget: 0 },
    { id: 'others', name: 'Others & Misc', icon: 'more-horizontal', color: '#64748b', monthlyBudget: 0 }
  ],
  goals: [
    { id: 'g1', title: 'Emergency Fund (6 Months)', target: 150000, current: 95000, deadline: '2026-12-31', color: '#10b981', icon: 'shield-check' },
    { id: 'g2', title: 'Goa Holiday Trip', target: 35000, current: 22500, deadline: '2026-11-15', color: '#3b82f6', icon: 'plane' },
    { id: 'g3', title: 'New MacBook Air M3', target: 110000, current: 45000, deadline: '2027-03-01', color: '#8b5cf6', icon: 'laptop' }
  ],
  subscriptions: [
    { id: 's1', name: 'Netflix Premium (4K)', amount: 649, frequency: 'Monthly', nextDue: '2026-10-12', category: 'bills', active: true, usageRate: 'Low (2 hrs/mo)' },
    { id: 's2', name: 'Spotify Individual', amount: 119, frequency: 'Monthly', nextDue: '2026-10-18', category: 'bills', active: true, usageRate: 'High' },
    { id: 's3', name: 'Amazon Prime Annual', amount: 1499, frequency: 'Yearly', nextDue: '2026-12-05', category: 'bills', active: true, usageRate: 'High' },
    { id: 's4', name: 'Gym Membership Gold', amount: 1800, frequency: 'Monthly', nextDue: '2026-10-08', category: 'health', active: true, usageRate: 'Unused this month' },
    { id: 's5', name: 'Cloud Storage (Google One)', amount: 130, frequency: 'Monthly', nextDue: '2026-10-25', category: 'bills', active: true, usageRate: 'Medium' }
  ],
  transactions: [
    {
      id: 'tx_1',
      title: 'Zomato Food Delivery',
      merchant: 'Zomato',
      category: 'food',
      amount: 420,
      type: 'expense',
      date: '2026-10-04',
      account: 'acc_upi',
      note: 'Dinner with friends',
      isRecurring: false
    },
    {
      id: 'tx_2',
      title: 'Uber City Commute',
      merchant: 'Uber',
      category: 'travel',
      amount: 680,
      type: 'expense',
      date: '2026-10-03',
      account: 'acc_upi',
      note: 'Office to client location',
      isRecurring: false
    },
    {
      id: 'tx_3',
      title: 'Electricity Bill Payment',
      merchant: 'Bescom Power',
      category: 'bills',
      amount: 1200,
      type: 'expense',
      date: '2026-10-02',
      account: 'acc_bank',
      note: 'September power billing',
      isRecurring: true
    },
    {
      id: 'tx_4',
      title: 'Monthly Salary Credit',
      merchant: 'Employer Corp',
      category: 'others',
      amount: 65000,
      type: 'income',
      date: '2026-10-01',
      account: 'acc_bank',
      note: 'Oct monthly compensation',
      isRecurring: true
    },
    {
      id: 'tx_5',
      title: 'Blinkit Grocery Run',
      merchant: 'Blinkit',
      category: 'food',
      amount: 1450,
      type: 'expense',
      date: '2026-10-01',
      account: 'acc_upi',
      note: 'Fresh veggies and dairy',
      isRecurring: false
    },
    {
      id: 'tx_6',
      title: 'Amazon Shopping Sale',
      merchant: 'Amazon India',
      category: 'shopping',
      amount: 2890,
      type: 'expense',
      date: '2026-09-29',
      account: 'acc_card',
      note: 'Wireless headphones & cables',
      isRecurring: false
    },
    {
      id: 'tx_7',
      title: 'Weekend Dining Out',
      merchant: 'Chili’s Grill & Bar',
      category: 'food',
      amount: 2150,
      type: 'expense',
      date: '2026-09-28',
      account: 'acc_card',
      note: 'Weekend family dinner',
      isRecurring: false
    },
    {
      id: 'tx_8',
      title: 'Petrol Refuel',
      merchant: 'Indian Oil',
      category: 'travel',
      amount: 1500,
      type: 'expense',
      date: '2026-09-27',
      account: 'acc_upi',
      note: 'Full tank refuel',
      isRecurring: false
    },
    {
      id: 'tx_9',
      title: 'SIP Mutual Fund Auto-Debit',
      merchant: 'Zerodha Coin Nifty 50',
      category: 'investment',
      amount: 5000,
      type: 'expense',
      date: '2026-09-25',
      account: 'acc_bank',
      note: 'Index fund SIP investment',
      isRecurring: true
    },
    {
      id: 'tx_10',
      title: 'Netflix Subscription',
      merchant: 'Netflix',
      category: 'bills',
      amount: 649,
      type: 'expense',
      date: '2026-09-24',
      account: 'acc_card',
      note: 'Recurring OTT',
      isRecurring: true
    },
    {
      id: 'tx_11',
      title: 'Freelance Design Bonus',
      merchant: 'Upwork Client',
      category: 'others',
      amount: 15000,
      type: 'income',
      date: '2026-09-20',
      account: 'acc_bank',
      note: 'UI kit consulting work',
      isRecurring: false
    },
    {
      id: 'tx_12',
      title: 'Starbucks Coffee & Snacks',
      merchant: 'Starbucks',
      category: 'food',
      amount: 580,
      type: 'expense',
      date: '2026-09-19',
      account: 'acc_upi',
      note: 'Work from cafe session',
      isRecurring: false
    },
    {
      id: 'tx_13',
      title: 'Metro Smart Card Recharge',
      merchant: 'Namma Metro',
      category: 'travel',
      amount: 500,
      type: 'expense',
      date: '2026-09-18',
      account: 'acc_upi',
      note: 'Commute top-up',
      isRecurring: false
    },
    {
      id: 'tx_14',
      title: 'Pharmacy & Health Supplements',
      merchant: 'Apollo Pharmacy',
      category: 'health',
      amount: 980,
      type: 'expense',
      date: '2026-09-15',
      account: 'acc_cash',
      note: 'Multivitamins & first aid',
      isRecurring: false
    },
    {
      id: 'tx_15',
      title: 'Myntra Fashion Haul',
      merchant: 'Myntra',
      category: 'shopping',
      amount: 3200,
      type: 'expense',
      date: '2026-09-12',
      account: 'acc_card',
      note: 'Casual shirts and sneakers',
      isRecurring: false
    },
    {
      id: 'tx_16',
      title: 'Wi-Fi Fiber Broadband',
      merchant: 'Airtel Xstream',
      category: 'bills',
      amount: 943,
      type: 'expense',
      date: '2026-09-10',
      account: 'acc_bank',
      note: 'Monthly 200Mbps broadband',
      isRecurring: true
    },
    {
      id: 'tx_17',
      title: 'Food Delivery - Swiggy',
      merchant: 'Swiggy',
      category: 'food',
      amount: 620,
      type: 'expense',
      date: '2026-09-08',
      account: 'acc_upi',
      note: 'Late night snack',
      isRecurring: false
    },
    {
      id: 'tx_18',
      title: 'Gold Gym Membership Renewal',
      merchant: 'Cult.fit Gym',
      category: 'health',
      amount: 1800,
      type: 'expense',
      date: '2026-09-05',
      account: 'acc_card',
      note: 'Gym monthly fee',
      isRecurring: true
    }
  ]
};

Object.assign(DEFAULT_DATA, {
  user: { name: '', currency: '₹', monthlyIncome: 0, savingsTargetRate: 20 },
  accounts: [],
  goals: [],
  subscriptions: [],
  transactions: [],
  taxSavings: []
});
DEFAULT_DATA.categories.forEach(category => {
  category.monthlyBudget = 0;
});

// Data Manager class with localStorage persistence & helper queries
class FinanceDataManager {
  constructor() {
    this.data = this.loadData();
  }

  getStorageKey() {
    const user = JSON.parse(localStorage.getItem('finance_auth_user') || 'null');
    return user && user.id ? `${STORAGE_KEY}_${user.id}` : STORAGE_KEY;
  }

  loadData() {
    try {
      const stored = localStorage.getItem(this.getStorageKey());
      if (stored) {
        return this.removeDemoValues(JSON.parse(stored));
      }
    } catch (e) {
      console.warn('Could not read from localStorage, using default data', e);
    }
    const emptyData = JSON.parse(JSON.stringify(DEFAULT_DATA));
    const user = JSON.parse(localStorage.getItem('finance_auth_user') || 'null');
    if (user && user.id) emptyData.user.name = user.name;
    this.saveData(emptyData);
    return emptyData;
  }

  removeDemoValues(data) {
    const demoTransactionIds = new Set(Array.from({ length: 18 }, (_, index) => `tx_${index + 1}`));
    const demoGoalIds = new Set(['g1', 'g2', 'g3']);
    const demoSubscriptionIds = new Set(['s1', 's2', 's3', 's4', 's5']);
    const demoTaxIds = new Set(['t1', 't2', 't3', 't4']);
    const demoAccountIds = new Set(['acc_bank', 'acc_card', 'acc_upi', 'acc_cash']);
    data.transactions = (data.transactions || []).filter(item => !demoTransactionIds.has(item.id));
    data.goals = (data.goals || []).filter(item => !demoGoalIds.has(item.id));
    data.subscriptions = (data.subscriptions || []).filter(item => !demoSubscriptionIds.has(item.id));
    data.taxSavings = (data.taxSavings || []).filter(item => !demoTaxIds.has(item.id));
    data.categories = (data.categories || DEFAULT_DATA.categories).map(category => ({
      ...category,
      monthlyBudget: Math.max(0, Number(category.monthlyBudget) || 0)
    }));
    data.accounts = (data.accounts || []).filter(account => !demoAccountIds.has(account.id));

    const accountDetails = {
      acc_bank: { name: 'Bank account', type: 'bank', icon: 'landmark', color: '#2563eb' },
      acc_card: { name: 'Credit card', type: 'card', icon: 'credit-card', color: '#8b5cf6' },
      acc_upi: { name: 'Digital wallet', type: 'upi', icon: 'smartphone', color: '#06b6d4' },
      acc_cash: { name: 'Cash', type: 'cash', icon: 'wallet', color: '#10b981' }
    };
    this.data = data;
    data.transactions.forEach(transaction => {
      const details = accountDetails[transaction.account];
      if (!details) return;
      let account = data.accounts.find(item => item.id === transaction.account);
      if (!account) {
        account = { id: transaction.account, ...details, balance: 0 };
        data.accounts.push(account);
      }
      const amount = Number(transaction.amount) || 0;
      account.balance += transaction.type === 'income' ? amount : -amount;
    });
    this.saveData(data);
    return data;
  }

  saveData(dataToSave = this.data) {
    try {
      localStorage.setItem(this.getStorageKey(), JSON.stringify(dataToSave));
    } catch (e) {
      console.error('Could not save to localStorage', e);
    }
  }

  resetToDefault() {
    this.data = JSON.parse(JSON.stringify(DEFAULT_DATA));
    this.saveData();
    return this.data;
  }

  getCurrency() {
    return this.data.user.currency || '₹';
  }

  formatMoney(amount) {
    const sym = this.getCurrency();
    const formatted = Math.abs(amount).toLocaleString('en-IN', {
      maximumFractionDigits: 0
    });
    return amount < 0 ? `-${sym}${formatted}` : `${sym}${formatted}`;
  }

  getTransactions() {
    return [...this.data.transactions].sort((a, b) => new Date(b.date) - new Date(a.date));
  }

  addTransaction(tx) {
    const newTx = {
      id: 'tx_' + Date.now(),
      date: tx.date,
      title: tx.title,
      merchant: tx.merchant || tx.title,
      category: tx.category,
      amount: parseFloat(tx.amount),
      type: tx.type,
      account: tx.account,
      note: tx.note || '',
      isRecurring: Boolean(tx.isRecurring),
      discretionary: tx.type === 'income' ? false : Boolean(tx.discretionary)
    };

    const accountDetails = {
      acc_bank: { name: 'Bank account', type: 'bank', icon: 'landmark', color: '#2563eb' },
      acc_card: { name: 'Credit card', type: 'card', icon: 'credit-card', color: '#8b5cf6' },
      acc_upi: { name: 'Digital wallet', type: 'upi', icon: 'smartphone', color: '#06b6d4' },
      acc_cash: { name: 'Cash', type: 'cash', icon: 'wallet', color: '#10b981' }
    }[newTx.account];
    let acc = this.data.accounts.find(a => a.id === newTx.account);
    if (!acc && accountDetails) {
      acc = { id: newTx.account, ...accountDetails, balance: 0 };
      this.data.accounts.push(acc);
    }
    if (acc) {
      if (newTx.type === 'income') {
        acc.balance += newTx.amount;
      } else {
        acc.balance -= newTx.amount;
      }
    }

    this.data.transactions.unshift(newTx);
    this.saveData();

    // Sync with Spring Boot backend (if online)
    const token = sessionStorage.getItem('finance_auth_token');
    fetch(`${window.FINANCE_API_BASE_URL || 'http://localhost:8081/api'}/expenses`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: JSON.stringify({
        title: newTx.title,
        amount: newTx.amount,
        category: newTx.category,
        type: newTx.type,
        paymentMode: newTx.account,
        date: newTx.date,
        notes: newTx.note,
        discretionary: newTx.discretionary
      })
    }).then(async response => {
      if (!response.ok) throw new Error(`Transaction sync failed (${response.status}).`);
      const saved = await response.json();
      newTx.backendId = saved.id;
      this.saveData();
      if (window.refreshBackendGamification) await window.refreshBackendGamification();
    }).catch(err => console.warn('Spring Boot backend sync failed:', err.message));

    return newTx;
  }

  deleteTransaction(id) {
    const idx = this.data.transactions.findIndex(t => t.id === id);
    if (idx !== -1) {
      const tx = this.data.transactions[idx];
      id = tx.backendId || id;
      // Revert account balance
      const acc = this.data.accounts.find(a => a.id === tx.account);
      if (acc) {
        if (tx.type === 'income') acc.balance -= tx.amount;
        else acc.balance += tx.amount;
      }
      this.data.transactions.splice(idx, 1);
      this.saveData();

      // Sync delete with Spring Boot backend if numeric ID
      if (typeof id === 'number' || !isNaN(id)) {
        const token = sessionStorage.getItem('finance_auth_token');
        fetch(`${window.FINANCE_API_BASE_URL || 'http://localhost:8081/api'}/expenses/${id}`, { method: 'DELETE', headers: token ? { Authorization: `Bearer ${token}` } : {} })
          .then(response => {
            if (!response.ok && response.status !== 404) throw new Error(`Transaction delete sync failed (${response.status}).`);
            if (window.refreshBackendGamification) window.refreshBackendGamification();
          })
          .catch(err => console.warn('Spring Boot backend delete sync failed:', err.message));
      }
      return true;
    }
    return false;
  }

  getCategory(catId) {
    if (catId === 'debt') {
      return {
        id: 'debt',
        name: 'Debt Payments',
        icon: 'landmark',
        color: '#f97316',
        monthlyBudget: 0
      };
    }
    return this.data.categories.find(c => c.id === catId) || {
      id: 'others',
      name: 'Others',
      icon: 'more-horizontal',
      color: '#64748b',
      monthlyBudget: 0
    };
  }

  isDiscretionaryTransaction(transaction) {
    if (transaction.discretionary !== undefined && transaction.discretionary !== null) {
      return Boolean(transaction.discretionary);
    }
    return ['shopping', 'entertainment'].includes(String(transaction.category || '').toLowerCase());
  }

  getAccount(accId) {
    return this.data.accounts.find(a => a.id === accId) || {
      id: 'acc_upi',
      name: 'UPI / Wallet',
      icon: 'smartphone',
      color: '#06b6d4',
      balance: 0
    };
  }

  // Calculate Metrics
  calculateSummary() {
    let totalBalance = 0;
    this.data.accounts.forEach(acc => {
      totalBalance += acc.balance;
    });

    let totalExpense = 0;
    let totalIncome = 0;
    const categoryTotals = {};

    this.data.categories.forEach(c => {
      categoryTotals[c.id] = 0;
    });

    this.data.transactions.forEach(tx => {
      if (tx.type === 'expense') {
        totalExpense += tx.amount;
        categoryTotals[tx.category] = (categoryTotals[tx.category] || 0) + tx.amount;
      } else if (tx.type === 'income') {
        totalIncome += tx.amount;
      }
    });

    const savings = Math.max(0, totalIncome - totalExpense);
    const savingsRate = totalIncome > 0 ? ((savings / totalIncome) * 100).toFixed(1) : 0;

    return {
      totalBalance,
      totalExpense,
      totalIncome,
      savings,
      savingsRate,
      categoryTotals
    };
  }

  calculateGamification(today = new Date()) {
    const dateKey = date => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    const currentDay = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const todayKey = dateKey(currentDay);
    const periodStart = new Date(currentDay);
    periodStart.setDate(periodStart.getDate() - 29);
    const periodStartKey = dateKey(periodStart);
    const budgetByCategory = new Map(
      this.data.categories
        .filter(category => Number(category.monthlyBudget) > 0)
        .map(category => [category.id.toLowerCase(), Number(category.monthlyBudget)])
    );
    const totalBudget = [...budgetByCategory.values()].reduce((total, amount) => total + amount, 0);
    const discretionaryDays = new Set();
    let earliestTransaction = null;
    let income = 0;
    let spending = 0;
    let debtPayments = 0;
    let budgetedSpending = 0;

    this.data.transactions.forEach(transaction => {
      const date = String(transaction.date || '').slice(0, 10);
      if (!date || date > todayKey) return;
      if (!earliestTransaction || date < earliestTransaction) earliestTransaction = date;
      const amount = Math.max(0, Number(transaction.amount) || 0);

      if (transaction.type === 'income') {
        if (date >= periodStartKey) income += amount;
        return;
      }

      const category = String(transaction.category || '').toLowerCase();
      if (this.isDiscretionaryTransaction(transaction)) discretionaryDays.add(date);
      if (date >= periodStartKey) {
        spending += amount;
        if (/debt|loan|credit/.test(category)) debtPayments += amount;
      }
      if (date.slice(0, 7) === todayKey.slice(0, 7) && budgetByCategory.has(category)) {
        budgetedSpending += amount;
      }
    });

    const savingsRate = income > 0 ? ((income - spending) / income) * 100 : null;
    const budgetAdherence = totalBudget > 0
      ? (budgetedSpending <= totalBudget ? 100 : totalBudget / budgetedSpending * 100)
      : null;
    const debtToIncome = income > 0 ? debtPayments / income * 100 : null;
    const clamp = value => Math.max(0, Math.min(100, value));
    const savingsScore = savingsRate === null ? 50 : clamp(savingsRate / 20 * 100);
    const budgetScore = budgetAdherence === null ? 50 : budgetAdherence;
    const debtScore = debtToIncome === null ? 50 : clamp((50 - debtToIncome) / 30 * 100);

    let noSpendStreak = 0;
    if (earliestTransaction) {
      const latestDiscretionaryDay = [...discretionaryDays]
        .filter(date => date <= todayKey)
        .sort()
        .pop();
      const dayMilliseconds = 24 * 60 * 60 * 1000;
      const daysBetween = (start, end) => Math.floor(
        (Date.parse(`${end}T00:00:00Z`) - Date.parse(`${start}T00:00:00Z`)) / dayMilliseconds
      );
      noSpendStreak = latestDiscretionaryDay
        ? daysBetween(latestDiscretionaryDay, todayKey)
        : daysBetween(earliestTransaction, todayKey) + 1;
    }

    return {
      score: Math.round(savingsScore * 0.4 + budgetScore * 0.35 + debtScore * 0.25),
      savingsRate: savingsRate === null ? null : Math.round(savingsRate * 10) / 10,
      budgetAdherence: budgetAdherence === null ? null : Math.round(budgetAdherence * 10) / 10,
      debtToIncome: debtToIncome === null ? null : Math.round(debtToIncome * 10) / 10,
      savingsScore: Math.round(savingsScore),
      budgetScore: Math.round(budgetScore),
      debtScore: Math.round(debtScore),
      noSpendStreak,
      monthlySavings: Math.max(0, income - spending)
    };
  }

  // Monthly breakdown for trend chart
  getMonthlyTrend() {
    const monthlyData = {};
    const now = new Date();
    for (let offset = 5; offset >= 0; offset -= 1) {
      const month = new Date(now.getFullYear(), now.getMonth() - offset, 1);
      const key = month.toLocaleDateString('en-US', { month: 'short' });
      monthlyData[key] = { income: 0, expense: 0 };
    }
    this.data.transactions.forEach(transaction => {
      const transactionDate = new Date(`${transaction.date}T00:00:00`);
      const key = transactionDate.toLocaleDateString('en-US', { month: 'short' });
      if (!monthlyData[key]) return;
      if (transaction.type === 'income') monthlyData[key].income += Number(transaction.amount) || 0;
      else monthlyData[key].expense += Number(transaction.amount) || 0;
    });
    return monthlyData;
  }

  // Add a savings goal contribution
  contributeToGoal(goalId, amount) {
    const goal = this.data.goals.find(g => g.id === goalId);
    if (goal) {
      goal.current = Math.min(goal.target, goal.current + parseFloat(amount));
      this.saveData();
      window.backendGamification = null;
      if (goal.backendId) {
        const token = sessionStorage.getItem('finance_auth_token');
        fetch(`${window.FINANCE_API_BASE_URL || 'http://localhost:8081/api'}/goals/${goal.backendId}/add-funds?amount=${encodeURIComponent(amount)}`, {
          method: 'PUT',
          headers: token ? { Authorization: `Bearer ${token}` } : {}
        }).then(response => {
          if (!response.ok) throw new Error(`Goal contribution sync failed (${response.status}).`);
          if (window.refreshBackendGamification) window.refreshBackendGamification();
        }).catch(err => console.warn('Spring Boot goal contribution sync failed:', err.message));
      }
      return goal;
    }
    return null;
  }

  // Add new Goal
  addGoal(goalData) {
    const newGoal = {
      id: 'g_' + Date.now(),
      title: goalData.title,
      target: parseFloat(goalData.target),
      current: parseFloat(goalData.current || 0),
      deadline: goalData.deadline,
      color: goalData.color || '#10b981',
      icon: goalData.icon || 'target'
    };
    if (!this.data.goals) this.data.goals = [];
    this.data.goals.push(newGoal);
    this.saveData();

    // Sync Spring Boot
    const token = sessionStorage.getItem('finance_auth_token');
    fetch(`${window.FINANCE_API_BASE_URL || 'http://localhost:8081/api'}/goals`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: JSON.stringify({
        title: newGoal.title,
        targetAmount: newGoal.target,
        currentAmount: newGoal.current,
        deadline: newGoal.deadline,
        category: 'general'
      })
    }).then(async response => {
      if (!response.ok) throw new Error(`Goal sync failed (${response.status}).`);
      const saved = await response.json();
      newGoal.backendId = saved.id;
      this.saveData();
      if (window.refreshBackendGamification) await window.refreshBackendGamification();
    }).catch(err => console.warn('Spring Boot goal sync failed:', err.message));

    return newGoal;
  }

  // Add new Subscription
  addSubscription(subData) {
    const newSub = {
      id: 's_' + Date.now(),
      name: subData.name,
      amount: parseFloat(subData.amount),
      frequency: subData.frequency,
      nextDue: subData.nextDue,
      category: subData.category,
      active: true,
      usageRate: subData.usageRate
    };
    if (!this.data.subscriptions) this.data.subscriptions = [];
    this.data.subscriptions.push(newSub);
    this.saveData();

    // Sync Spring Boot
    const token = sessionStorage.getItem('finance_auth_token');
    fetch(`${window.FINANCE_API_BASE_URL || 'http://localhost:8081/api'}/subscriptions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: JSON.stringify({
        name: newSub.name,
        amount: newSub.amount,
        frequency: newSub.frequency,
        category: newSub.category,
        usageRate: newSub.usageRate,
        nextDueDate: newSub.nextDue
      })
    }).then(async response => {
      if (!response.ok) throw new Error(`Subscription sync failed (${response.status}).`);
      const saved = await response.json();
      newSub.backendId = saved.id;
      this.saveData();
    }).catch(err => console.warn('Spring Boot subscription sync failed:', err.message));

    return newSub;
  }

  // Calculate Emergency Runway (Months of expenses covered by liquid cash)
  calculateRunway() {
    const summary = this.calculateSummary();
    const liquidCash = summary.totalBalance;
    if (summary.totalExpense <= 0) {
      return { months: null, status: 'Add expense data', color: '#94a3b8' };
    }
    const months = Number((liquidCash / summary.totalExpense).toFixed(1));
    return {
      months,
      status: months >= 6 ? 'Optimal (6+ Mo)' : months >= 3 ? 'Healthy (3-6 Mo)' : 'Low (< 3 Mo)',
      color: months >= 6 ? '#10b981' : months >= 3 ? '#3b82f6' : '#ef4444'
    };
  }

  // Tax 80C Calculator
  getTaxSavingsList() {
    if (!this.data.taxSavings) {
      this.data.taxSavings = [];
      this.saveData();
    }
    return this.data.taxSavings;
  }

  addTaxSaving(item) {
    const list = this.getTaxSavingsList();
    const newItem = {
      id: 't_' + Date.now(),
      name: item.name,
      amount: parseFloat(item.amount),
      category: item.category || 'Section 80C'
    };
    list.push(newItem);
    this.saveData();
    return newItem;
  }

  getTaxSavingsTotal() {
    const list = this.getTaxSavingsList();
    return list.reduce((sum, item) => sum + item.amount, 0);
  }
}

window.financeDB = new FinanceDataManager();
