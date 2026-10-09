/**
 * Chart.js Visualizations for Personal Finance Dashboard
 */

class FinanceCharts {
  constructor(db) {
    this.db = db;
    this.categoryChart = null;
    this.trendChart = null;
  }

  init() {
    this.renderCategoryChart();
    this.renderTrendChart();
  }

  updateAll() {
    this.renderCategoryChart();
    this.renderTrendChart();
  }

  renderCategoryChart() {
    const ctx = document.getElementById('categoryDonutChart');
    if (!ctx) return;

    const summary = this.db.calculateSummary();
    const categories = this.db.data.categories;
    const sym = this.db.getCurrency();

    // Prepare labels, values, colors
    const labels = [];
    const dataValues = [];
    const backgroundColors = [];

    categories.forEach(cat => {
      const amount = summary.categoryTotals[cat.id] || 0;
      if (amount > 0) {
        labels.push(cat.name);
        dataValues.push(amount);
        backgroundColors.push(cat.color);
      }
    });

    if (dataValues.length === 0) {
      labels.push('No Expenses');
      dataValues.push(1);
      backgroundColors.push('#e2e8f0');
    }

    if (this.categoryChart) {
      this.categoryChart.destroy();
    }

    // Custom center text plugin
    const centerTextPlugin = {
      id: 'centerText',
      beforeDraw: (chart) => {
        const { width, height, ctx } = chart;
        ctx.restore();
        
        const fontSizeAmount = (height / 11).toFixed(2);
        ctx.font = `700 ${fontSizeAmount}px "DM Sans", sans-serif`;
        ctx.textBaseline = 'middle';
        ctx.fillStyle = document.body.classList.contains('dark-mode') ? '#f8fafc' : '#0f172a';

        const textAmount = `${sym} ${summary.totalExpense.toLocaleString()}`;
        const textAmountX = Math.round((width - ctx.measureText(textAmount).width) / 2);
        const textAmountY = height / 2 - 8;
        ctx.fillText(textAmount, textAmountX, textAmountY);

        const fontSizeLabel = (height / 20).toFixed(2);
        ctx.font = `500 ${fontSizeLabel}px "DM Sans", sans-serif`;
        ctx.fillStyle = document.body.classList.contains('dark-mode') ? '#858585' : '#64748b';
        const textLabel = 'Total Spent';
        const textLabelX = Math.round((width - ctx.measureText(textLabel).width) / 2);
        const textLabelY = height / 2 + 16;
        ctx.fillText(textLabel, textLabelX, textLabelY);

        ctx.save();
      }
    };

    this.categoryChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: dataValues,
          backgroundColor: backgroundColors,
          borderWidth: 3,
          borderColor: document.body.classList.contains('dark-mode') ? '#242424' : '#ffffff',
          hoverOffset: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '72%',
        plugins: {
          legend: {
            display: false
          },
          tooltip: {
            backgroundColor: 'rgba(15, 23, 42, 0.95)',
            titleFont: { size: 13, weight: 'bold', family: 'DM Sans' },
            bodyFont: { size: 12, family: 'DM Sans' },
            padding: 12,
            cornerRadius: 10,
            callbacks: {
              label: (context) => {
                const val = context.raw;
                const total = summary.totalExpense || 1;
                const pct = Math.round((val / total) * 100);
                return ` ${sym}${val.toLocaleString()} (${pct}%)`;
              }
            }
          }
        },
        animation: {
          animateScale: true,
          animateRotate: true,
          duration: 800
        }
      },
      plugins: [centerTextPlugin]
    });

    // Render custom legend beside chart
    this.renderCategoryLegend(labels, dataValues, backgroundColors, summary.totalExpense);
  }

  renderCategoryLegend(labels, dataValues, colors, total) {
    const legendContainer = document.getElementById('categoryLegendContainer');
    if (!legendContainer) return;

    legendContainer.innerHTML = '';
    labels.forEach((label, idx) => {
      const val = dataValues[idx];
      const pct = total > 0 ? Math.round((val / total) * 100) : 0;
      
      const item = document.createElement('div');
      item.className = 'legend-item';
      item.innerHTML = `
        <div class="legend-left">
          <span class="legend-dot" style="background-color: ${colors[idx]}"></span>
          <span class="legend-label">${label}</span>
        </div>
        <span class="legend-pct">${pct}%</span>
      `;
      legendContainer.appendChild(item);
    });
  }

  renderTrendChart() {
    const ctx = document.getElementById('monthlyTrendChart');
    if (!ctx) return;

    const trendData = this.db.getMonthlyTrend();
    const months = Object.keys(trendData);
    const expenseData = months.map(m => trendData[m].expense);
    const sym = this.db.getCurrency();

    if (this.trendChart) {
      this.trendChart.destroy();
    }

    // Gradient fill
    const canvas = ctx.getContext('2d');
    const gradient = canvas.createLinearGradient(0, 0, 0, 200);
    gradient.addColorStop(0, 'rgba(16, 185, 129, 0.35)');
    gradient.addColorStop(1, 'rgba(16, 185, 129, 0.0)');

    this.trendChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: months,
        datasets: [{
          label: 'Spending Trend',
          data: expenseData,
          borderColor: '#10b981',
          backgroundColor: gradient,
          borderWidth: 2.5,
          fill: true,
          tension: 0.4,
          pointBackgroundColor: '#10b981',
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          pointRadius: 4,
          pointHoverRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false
          },
          tooltip: {
            backgroundColor: 'rgba(15, 23, 42, 0.95)',
            titleFont: { size: 13, weight: 'bold', family: 'DM Sans' },
            bodyFont: { size: 12, family: 'DM Sans' },
            padding: 10,
            cornerRadius: 8,
            callbacks: {
              label: (context) => ` Spent: ${sym}${context.raw.toLocaleString()}`
            }
          }
        },
        scales: {
          x: {
            grid: {
              display: false,
              drawBorder: false
            },
            ticks: {
              color: '#94a3b8',
              font: { size: 11, family: 'DM Sans' }
            }
          },
          y: {
            display: false,
            grid: {
              display: false
            }
          }
        }
      }
    });
  }
}

window.financeCharts = new FinanceCharts(window.financeDB);
