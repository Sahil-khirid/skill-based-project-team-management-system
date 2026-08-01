# Frontend — Skill-Based Project Team Management System

React/Vite single-page application providing the shared UI foundation,
authentication, and routing for the Skill-Based Project Team Management
System. It is the client used by all members' feature modules.

## Prerequisites

- Node.js v24+
- npm v11+

## Setup

```bash
npm install
```

Copy the environment example and adjust if needed:

```bash
cp .env.example .env
```

`.env` must define:

```
VITE_API_BASE_URL=http://localhost:8080
```

This is the API Gateway URL. **All API calls go through the API Gateway** —
the frontend never calls a backend microservice port (e.g. auth-service,
user-skill-service) directly.

## Scripts

```bash
npm run dev      # start the Vite dev server
npm run lint     # run oxlint
npm run build    # production build to dist/
```

## Module structure

- `src/api/` — shared Axios client (`apiClient.js`), auth API calls
  (`authApi.js`), and error-message extraction (`errorMessage.js`)
- `src/auth/` — `AuthContext`/`AuthProvider`, `useAuth` hook, token storage,
  `ProtectedRoute`, and `RoleRoute`
- `src/components/` — shared UI (navbar, loading spinner)
- `src/layouts/` — page layout shell
- `src/pages/` — route-level pages

### Ownership

- **Member 1 (this foundation):** app scaffold, Bootstrap setup, routing,
  Axios client, authentication state, login/registration/logout, protected
  and role-aware routes
- **Member 2:** Profile, Skills, Availability
- **Member 3:** Projects, required skills, recommendations, Teams
- **Member 4:** Tasks, assignment, status, progress

## Known limitations

- Automatic refresh-token rotation is **not implemented yet**. The refresh
  token is stored and used only for the logout request.
