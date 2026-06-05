# Luồng Security JWT hiện tại

File chi tiết dài trước đó đã được rút gọn và chỉnh theo flow mới.

Flow hiện tại không dùng `AuthenticatedUser` trong domain nữa. Controller lấy thẳng `userId` từ principal bằng:

```java
@AuthenticationPrincipal(expression = "userId") Long userId
```

## Flow chính

```text
Login
 -> sinh JWT có subject=email và claim userId
 -> FE gửi Authorization: Bearer <accessToken>
 -> JwtAuthFilter verify token bằng jwt.secret
 -> lấy email từ subject
 -> CustomUserDetailsService load User từ DB bằng email
 -> new SecurityUser(user, authorities)
 -> UsernamePasswordAuthenticationToken principal = SecurityUser
 -> SecurityContextHolder.setAuthentication(authToken)
 -> Controller lấy userId bằng expression "userId"
 -> Application Service xử lý use case bằng userId
```

## Chỗ principal được set

```java
UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
        userDetails,
        null,
        userDetails.getAuthorities()
);

SecurityContextHolder.getContext().setAuthentication(authToken);
```

Ở đây:

```text
authToken.principal = userDetails
userDetails thực tế là SecurityUser
```

## Chỗ userId được lấy

Trong `SecurityUser`:

```java
public Long getUserId() {
    return user.getId();
}
```

Trong controller:

```java
@GetMapping("/me")
public ResponseEntity<UserResponse> getProfile(
        @AuthenticationPrincipal(expression = "userId") Long userId
) {
    return ResponseEntity.ok(userAppService.getProfileById(userId));
}
```

Spring làm tương đương:

```text
SecurityContextHolder.getContext().getAuthentication().getPrincipal()
 -> SecurityUser
 -> gọi getUserId()
 -> trả Long userId cho controller
```

## File ngắn

Xem bản input/output ngắn ở:

```text
security_jwt_flow_short.md
```

