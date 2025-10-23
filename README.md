%% --- Oauth2-Login-Demo Architecture Diagram ---

graph TD
    subgraph User Browser[User's Web Browser]
        direction TB
        React[React Frontend<br>(localhost:3000)]
    end
    
    subgraph Backend[Spring Boot Server<br>(localhost:8080)]
        direction TB
        
        subgraph Security[Spring Security (Filters)]
            direction LR
            AuthFilter(OAuth2 Login Filter)
            CsrfFilter(CSRF Filter)
            CorsFilter(CORS Filter)
        end

        subgraph API[API Layer]
            direction TB
            UserController(UserController<br>/api/me, /api/profile)
            CsrfController(CsrfController<br>/api/csrf)
        end
        
        subgraph Service[Service Layer]
            direction TB
            UserService(CustomOAuth2UserService<br>loadUser())
        end

        subgraph Data[Data Access Layer]
            direction TB
            UserRepo(UserRepository)
            ProviderRepo(AuthProviderRepository)
        end
    end

    subgraph External Services
        direction TB
        Google[Google OAuth2]
        GitHub[GitHub OAuth2]
    end

    subgraph Database[PostgreSQL Database]
        direction TB
        UserTable(User Table<br>id, email, displayName, bio)
        ProviderTable(AuthProvider Table<br>id, user_id, provider, provider_user_id)
    end

    %% --- FLOWS ---

    %% 1. Authentication Flow
    React -- 1. Click 'Login with Google' --> AuthFilter
    AuthFilter -- 2. Redirect to --> Google
    Google -- 3. User authenticates & consents --> AuthFilter
    AuthFilter -- 4. Get Auth Code, Call Token Endpoint --> Google
    Google -- 5. Send User Info --> AuthFilter
    AuthFilter -- 6. Calls loadUser() --> UserService
    UserService -- 7. Find/Create User --> UserRepo
    UserService -- 8. Find/Create AuthProvider --> ProviderRepo
    UserRepo --> Database
    ProviderRepo --> Database
    UserService -- 9. Returns OAuth2User to --> AuthFilter
    AuthFilter -- 10. Creates session & redirects --> React
    
    %% 2. Authenticated API Request Flow
    React -- A. GET /api/me (with JSESSIONID) --> CorsFilter
    CorsFilter --> CsrfFilter
    CsrfFilter -- B. Validates session --> UserController
    UserController -- C. Get User Details --> React

    %% 3. CSRF-Protected API Request Flow
    React -- 1. GET /api/csrf --> CsrfController
    CsrfController -- 2. Returns XSRF-TOKEN --> React
    React -- 3. POST /api/profile (with JSESSIONID + X-XSRF-TOKEN header) --> CorsFilter
    CorsFilter --> CsrfFilter
    CsrfFilter -- 4. Validates Token & Session --> UserController
    UserController -- 5. Update User --> UserService
    UserService -- 6. Save User --> UserRepo
    UserRepo --> Database
    UserController -- 7. Return 200 OK --> React
