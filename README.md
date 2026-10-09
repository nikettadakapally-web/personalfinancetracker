# Wealth & Expense Intelligence

Personal finance dashboard with a Spring Boot API.

## Live deployment

- **Frontend:** [Open the finance dashboard](https://wealth-expense-intelligence.onrender.com/)
- **Backend API base URL:** `https://wealth-expense-intelligence.onrender.com/api`
- **Backend health check:** [Check API health](https://wealth-expense-intelligence.onrender.com/health)

The frontend and backend are deployed together as one Render web service. The backend API routes are available under `/api` (for example, `/api/auth` and `/api/expenses`).

This service uses Render's free plan. It may take about a minute to wake after inactivity, and its ephemeral database can lose saved accounts and finance data after a restart, spin-down, or redeploy.

## Project contents

- `dashboard-site/` — frontend dashboard (HTML, CSS, and JavaScript)
- `backend/personalfinancetracker/` — Spring Boot backend API
- `render.yaml` — Render Blueprint deployment configuration
