# Wealth & Expense Intelligence

Personal finance dashboard with a Spring Boot API. Frontend and backend source are kept in separate top-level folders.

## Deployment

The frontend is published to GitHub Pages, and the backend is deployed separately as a Docker service on Koyeb.

- **Frontend:** https://nikettadakapally-web.github.io/personalfinancetracker/
- **Backend API:** Koyeb assigns the backend URL when its service is created. Set the GitHub repository variable `BACKEND_API_BASE_URL` to that URL followed by `/api` (for example, `https://your-koyeb-service.koyeb.app/api`).
- **Backend health check:** `https://your-koyeb-service.koyeb.app/health`
- **Frontend deployment workflow:** [Deploy frontend](https://github.com/nikettadakapally-web/personalfinancetracker/actions/workflows/deploy-frontend.yml)
- **Backend deployment dashboard:** [Create/manage a Koyeb service](https://app.koyeb.com/)

The frontend workflow requires the GitHub Pages publishing source to be set to **GitHub Actions** under **Settings → Pages**, and the `BACKEND_API_BASE_URL` repository variable to be set after the Koyeb backend is deployed. The workflow waits until that variable exists, then publishes the frontend automatically. The published frontend and backend use different hostnames.

The free backend tier and its filesystem have provider limits. The H2 database is not durable on an ephemeral filesystem; saved accounts and finance data can be lost when the service restarts or redeploys.

## Project layout

- `frontend/` — static dashboard HTML, CSS, JavaScript, and assets
- `backend/` — Spring Boot API, Maven build, and Dockerfile
- `.github/workflows/deploy-frontend.yml` — GitHub Pages deployment workflow

## Run locally

Install Java 17 and start the backend from the repository root:

```powershell
.\backend\start-backend.ps1
```

The backend listens on `http://localhost:8081`; its health check is `/health` and its API base is `/api`. Serve the `frontend/` directory with a local static web server to open the dashboard separately.

Build the backend container image from the repository root:

```powershell
docker build -f backend/Dockerfile -t wealth-expense-backend .
```
