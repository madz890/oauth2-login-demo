Quick React frontend for Oath2 Login Demo

Run locally:

1. cd frontend
2. npm install
3. npm start

Notes:
- The frontend proxies API requests to the Spring Boot backend (see package.json proxy).
- The profile form posts directly to `/profile`. Spring Security enables CSRF by default; to make this work you must either fetch and include the CSRF token from the server or disable CSRF for this endpoint in your `SecurityConfig` for local development.
- You can extend this minimal app with React Router or fetch-based API calls for a better SPA experience.
