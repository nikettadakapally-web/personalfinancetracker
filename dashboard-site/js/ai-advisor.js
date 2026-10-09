/**
 * Dashboard insights and budget breakdowns based only on saved user data.
 */
class AIFinancialAdvisor {
  constructor(db) {
    this.db = db;
  }

  generateDashboardInsights() {
    const insights = [];
    const now = new Date();
    const monthKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    const monthlySpending = new Map();

    (this.db.data.transactions || []).forEach(transaction => {
      if (transaction.type !== 'expense' || String(transaction.date || '').slice(0, 7) !== monthKey) return;
      const categoryId = String(transaction.category || '');
      monthlySpending.set(categoryId, (monthlySpending.get(categoryId) || 0) + (Number(transaction.amount) || 0));
    });

    this.db.data.categories.forEach(category => {
      const budget = Number(category.monthlyBudget) || 0;
      const spent = monthlySpending.get(String(category.id)) || 0;
      if (budget > 0 && spent >= budget * 0.8) {
        insights.push({
          id: `budget_${category.id}`,
          type: spent > budget ? 'warning' : 'info',
          icon: category.icon || 'wallet',
          title: `${category.name} budget`,
          text: `${this.db.formatMoney(spent)} spent of your ${this.db.formatMoney(budget)} budget.`,
          actionText: 'Ask about this',
          actionPrompt: `How can I manage my ${category.name.toLowerCase()} spending?`
        });
      }
    });

    const lowUsageSubscriptions = (this.db.data.subscriptions || [])
      .filter(subscription => /low|unused/i.test(subscription.usageRate || ''));
    if (lowUsageSubscriptions.length) {
      insights.push({
        id: 'low_usage_subscriptions',
        type: 'info',
        icon: 'repeat',
        title: 'Review recurring costs',
        text: `${lowUsageSubscriptions.length} subscription${lowUsageSubscriptions.length === 1 ? '' : 's'} marked low-usage. Review whether they are still worth keeping.`,
        actionText: 'Review with the advisor',
        actionPrompt: 'Help me review my low-usage subscriptions.'
      });
    }

    return insights;
  }

  get50_30_20_Breakdown() {
    const summary = this.db.calculateSummary();
    const categories = summary.categoryTotals;
    const income = summary.totalIncome;
    const needs = (categories.bills || 0) + (categories.health || 0)
      + (categories.food || 0) * 0.5 + (categories.travel || 0) * 0.7;
    const wants = (categories.shopping || 0) + (categories.entertainment || 0)
      + (categories.food || 0) * 0.5 + (categories.travel || 0) * 0.3
      + (categories.others || 0);
    const savings = (categories.investment || 0) + summary.savings;
    const percentage = amount => income > 0 ? Math.round(amount / income * 100) : null;

    return {
      income,
      needs: { actual: needs, target: income * 0.5, pct: percentage(needs) },
      wants: { actual: wants, target: income * 0.3, pct: percentage(wants) },
      savings: { actual: savings, target: income * 0.2, pct: percentage(savings) }
    };
  }
}

window.aiAdvisor = new AIFinancialAdvisor(window.financeDB);
