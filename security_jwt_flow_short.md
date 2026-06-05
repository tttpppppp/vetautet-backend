# Luồng JWT ngắn gọn

## 1. Login lấy token

Input:

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@gmail.com",
  "password": "123456"
}
```

Flow:

```text
AuthController.login
 -> UserAppServiceImpl.login
 -> authenticate(email, password)
 -> getByEmail(email)
 -> generateAccessToken(email, userId)
 -> generateRefreshToken(email, userId)
 -> saveRefreshToken(...)
```

Output:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "email": "user@gmail.com"
}
```

## 2. Token chứa gì

Input khi sinh token:

```text
email = user.getEmail()
userId = user.getId()
```

Flow:

```text
JwtService.generateAccessToken(email, userId)
 -> claims.put("userId", userId)
 -> subject(email)
 -> expiration(...)
 -> signWith(jwt.secret)
```

Output JWT:

```text
subject = email
claim userId = user id
signature = ký bằng jwt.secret
```

## 3. FE gọi API private

Input:

```http
GET /api/v1/auth/me
Authorization: Bearer <accessToken>
```

Flow:

```text
Request
 -> SecurityFilterChain
 -> JwtAuthFilter
```

Output:

```text
JwtAuthFilter nhận Bearer token
```

## 4. JwtAuthFilter đọc token

Input:

```text
Authorization = "Bearer eyJ..."
```

Flow:

```text
authHeader = request.getHeader("Authorization")
 -> check startsWith("Bearer ")
 -> jwt = authHeader.substring(7)
```

Output:

```text
jwt = "eyJ..."
```

## 5. Verify token và lấy email

Input:

```text
jwt
jwt.secret
```

Flow:

```text
JwtService.extractUsername(jwt)
 -> extractAllClaims(jwt)
 -> Jwts.parser().verifyWith(secret).parseSignedClaims(jwt)
 -> Claims.getSubject()
```

Output:

```text
userEmail = token.subject
```

Ví dụ:

```text
userEmail = "user@gmail.com"
```

## 6. Load UserDetails

Input:

```text
userEmail = "user@gmail.com"
```

Flow:

```text
CustomUserDetailsService.loadUserByUsername(userEmail)
 -> userRepository.findByEmail(userEmail)
 -> build authorities từ roles/permissions
 -> new SecurityUser(user, authorities)
```

Output:

```text
userDetails = SecurityUser
userDetails.userId = 5
userDetails.username = user.email
userDetails.authorities = ROLE_CUSTOMER, ...
```

## 7. Check token valid

Input:

```text
jwt
userDetails.getUsername()
```

Flow:

```text
JwtService.isTokenValid(jwt, userDetails.getUsername())
 -> extractUsername(jwt)
 -> so sánh subject email với userDetails.username
 -> check token chưa hết hạn
```

Output:

```text
true hoặc false
```

## 8. Set SecurityContextHolder

Input:

```text
userDetails = SecurityUser
authorities = userDetails.getAuthorities()
```

Flow:

```text
new UsernamePasswordAuthenticationToken(userDetails, null, authorities)
 -> principal = userDetails
 -> authToken.setDetails(...)
 -> SecurityContextHolder.getContext().setAuthentication(authToken)
```

Output:

```text
SecurityContextHolder.context.authentication.principal = SecurityUser
```

## 9. Controller lấy userId

Input:

```java
@AuthenticationPrincipal(expression = "userId") Long userId
```

Flow:

```text
Spring lấy principal từ SecurityContextHolder
 -> principal là SecurityUser
 -> expression "userId"
 -> Spring gọi SecurityUser.getUserId()
```

Code trong `SecurityUser`:

```java
public Long getUserId() {
    return user.getId();
}
```

Output controller nhận:

```text
userId = id của User đã được load từ DB
```

Ví dụ:

```text
userId = 5
```

## 10. /auth/me flow đầy đủ

Input:

```http
GET /api/v1/auth/me
Authorization: Bearer <accessToken>
```

Flow:

```text
JwtAuthFilter
 -> lấy Bearer token
 -> verify token bằng jwt.secret
 -> lấy email từ subject
 -> load user DB bằng email
 -> tạo SecurityUser
 -> check token valid
 -> set SecurityContextHolder
 -> AuthController.getProfile
 -> @AuthenticationPrincipal(expression = "userId") Long userId
 -> userAppService.getProfileById(userId)
```

Output:

```json
{
  "id": 5,
  "email": "user@gmail.com"
}
```

## 11. Refresh token ngắn gọn

Input:

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "..."
}
```

Flow:

```text
AuthController.refreshToken
 -> UserAppServiceImpl.refreshToken
 -> get refresh token trong DB
 -> check revoked
 -> verify refresh token
 -> lấy email từ subject
 -> revoke refresh token cũ
 -> load user bằng email
 -> sinh accessToken mới
 -> sinh refreshToken mới
 -> lưu refreshToken mới
```

Output:

```json
{
  "accessToken": "new-access-token",
  "refreshToken": "new-refresh-token",
  "email": "user@gmail.com"
}
```

## 12. Kết luận ngắn nhất

```text
Token -> lấy email -> query DB ra User -> bọc SecurityUser -> set SecurityContextHolder -> controller lấy userId bằng expression "userId"
```

