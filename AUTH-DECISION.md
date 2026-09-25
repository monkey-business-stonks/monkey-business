# Authentication Decision - Sprint 6

**Date:** 2026-09-25  
**Branch:** rhea  
**Decision Maker:** Team  

---

## Decision: Token-less Password Validation (Development Stub)

### Current Implementation

For **Sprint 6 demonstration**, the team has decided to implement **basic password validation without token-based authentication**.

#### What This Means:

- Users are created with plain-text passwords in memory
- Login endpoint validates credentials by comparing plain-text passwords
- No JWT, OAuth2, or session tokens are generated
- All endpoints remain open with `@CrossOrigin(origins = "*")`
- Authentication is verified at the business logic layer only

#### Why This Approach?

1. **Rapid Development:** Focus on API contract and business logic without auth complexity
2. **Clear Separation:** Demonstrates that order validation lives in domain layer, not auth layer
3. **Testing Simplicity:** Easy to test order flow without token setup
4. **Documented Gaps:** Makes security concerns explicit for Sprint 7+

---

## Implementation Details

### User Creation
**Endpoint:** `POST /api/users`

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "password123"
}
```

**Validation:**
- ✅ Username: non-empty
- ✅ Email: valid format
- ✅ Password: minimum 8 characters
- ✅ Uniqueness: checked against in-memory database

**Stored:** Plain text in `UserService.userDatabase` HashMap

### Authentication
**Endpoint:** `POST /api/users/authenticate`

```json
{
  "username": "alice",
  "password": "password123"
}
```

**Response:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "alice",
  "email": "alice@example.com",
  "userAccessLevel": "User",
  "token": null  // Not populated in current implementation
}
```

**Validation:**
- ✅ Username exists in index
- ✅ Password matches stored value (exact string comparison)
- ✅ User is active (`isActive == true`)

### Access Control
**Current:** NONE enforced at HTTP layer

- No auth filter checking request headers
- No role-based access control
- No session management
- All endpoints accessible to anyone

**Business Logic Control:** OrderValidator checks `user.isActive()` but this is internal only

---

## Security Implications

### ⚠️ Critical Issues (Fix Before Production)

| Issue | Severity | Impact | Solution |
|-------|----------|--------|----------|
| Plain-text passwords | 🔴 CRITICAL | Password breach = full compromise | Use bcrypt hashing |
| No token-based auth | 🔴 CRITICAL | No way to maintain sessions | Implement JWT + refresh tokens |
| Unrestricted CORS | 🔴 CRITICAL | Any website can call API | Restrict to specific UI domain |
| No endpoint auth | 🔴 CRITICAL | Public API with no auth checks | Add authentication filter |
| No request validation | 🟠 HIGH | Injection attacks possible | Add @Valid annotations |
| No HTTPS | 🟠 HIGH | Credentials sent in plain text | Use TLS in production |

### 🟡 Medium Priority (Sprint 7)

- No audit logging of authentication attempts
- No rate limiting on auth endpoint
- No password complexity requirements
- No "forgot password" flow
- No multi-factor authentication
- No token expiration

---

## Production Migration Plan

### Phase 1: Implement JWT (1-2 days)
```java
1. Add Spring Security + JWT dependency
2. Create JwtTokenProvider
3. Generate tokens on login
4. Validate tokens on each request
5. Add token expiration (15 min access, 7 day refresh)
```

### Phase 2: Secure Credentials (1 day)
```java
1. Add bcrypt PasswordEncoder
2. Hash passwords on user creation
3. Validate against hash on login
4. Update UserService to use encoder
```

### Phase 3: Add Auth Filter (1 day)
```java
1. Implement JwtAuthenticationFilter
2. Register filter in SecurityConfig
3. Add @PreAuthorize on protected endpoints
4. Define role-based access rules
```

### Phase 4: Restrict CORS (0.5 days)
```java
1. Replace @CrossOrigin(origins = "*")
2. Set to specific UI domain
3. Configure allowed methods, headers
4. Test from UI
```

### Phase 5: HTTPS & Security Headers (1 day)
```java
1. Configure SSL/TLS in docker-compose
2. Add security headers (HSTS, CSP, X-Frame-Options)
3. Enable CSRF protection
4. Test certificate chain
```

**Estimated Total:** 4-5 days for production-ready auth

---

## Testing & Verification

### Current Test Cases

**UserTest.java:**
- ✅ Login with correct password
- ✅ Login with incorrect password
- ✅ Login when user inactive
- ✅ Password change
- ✅ Access level checks

### Integration Tests Needed (Post-Sprint-6)

```java
// Auth endpoint tests
@Test void testCreateUserEndpoint()
@Test void testAuthenticateEndpoint()
@Test void testUnauthorizedOrderPlacement()

// Security tests
@Test void testPlainTextPasswordNotStored()
@Test void testInvalidCredentialsRejected()
@Test void testCORSConfiguredCorrectly()
```

---

## Stakeholder Communication

### For Sprint 6 Demo:
- ✅ API is functional and testable
- ✅ Order validation correctly implemented
- ✅ Error handling consistent across endpoints
- ⚠️ Authentication is NOT production-ready
- ⚠️ This is intentional for architecture focus

### For Sprint 7 Backlog:
- Implement JWT-based authentication
- Add password hashing with bcrypt
- Restrict CORS to production domain
- Add comprehensive security tests

---

## Related Files

- **Implementation:** `server/src/main/java/main/service/UserService.java`
- **Endpoint:** `server/src/main/java/main/controller/UserController.java` (lines 56-65)
- **Domain Model:** `server/src/main/java/main/User.java`
- **Tests:** `server/src/test/java/main/UserTest.java`
- **API Contract:** `server/src/main/resources/openapi.yaml` (#/paths/~1users~1authenticate)

---

## Sign-Off

**Decision:** Approved for Sprint 6 demonstration  
**Trade-off:** Security vs. architectural clarity accepted  
**Review Date:** Sprint 7 planning  
**Escalation:** Implement Phase 1-2 before production deployment
