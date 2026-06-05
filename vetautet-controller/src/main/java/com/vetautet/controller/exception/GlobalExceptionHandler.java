package com.vetautet.controller.exception;

import com.vetautet.domain.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorMessage> handleBusinessExceptions(BusinessException ex, HttpServletRequest request) {
        ErrorMessage message = ErrorMessage.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(ex.getCode())
                .message(ex.getCode())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ErrorMessage> handleRateLimit(RequestNotPermitted ex, HttpServletRequest request) {
        ErrorMessage message = ErrorMessage.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("RATE_LIMIT_EXCEEDED")
                .message("Too many requests. Please try again later.")
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(message, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorMessage> handleOpenCircuit(CallNotPermittedException ex, HttpServletRequest request) {
        ErrorMessage message = ErrorMessage.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("CIRCUIT_BREAKER_OPEN")
                .message("External service is temporarily unavailable. Please try again later.")
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    // 1. Bắt lỗi Validation (Dữ liệu đầu vào sai định dạng)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorMessage message = ErrorMessage.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message("Dữ liệu gửi lên không đúng định dạng")
                .path(request.getRequestURI())
                .validationErrors(errors)
                .build();
        
        return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
    }

    // 2. Bắt lỗi Logic Nghiệp vụ (RuntimeException)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorMessage> handleRuntimeExceptions(RuntimeException ex, HttpServletRequest request) {
        ErrorMessage message = ErrorMessage.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Business Logic Error")
                .message(ex.getMessage() != null ? ex.getMessage() : "Lỗi logic hoặc dữ liệu không hợp lệ (Null message)")
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
    }

    // 3. Bắt lỗi Security (Chưa đăng nhập - 401)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorMessage> handleAuthExceptions(AuthenticationException ex, HttpServletRequest request) {
        ErrorMessage message = ErrorMessage.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Bạn cần đăng nhập để thực hiện hành động này")
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(message, HttpStatus.UNAUTHORIZED);
    }

    // 4. Bắt lỗi Phân quyền (Không có quyền - 403)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorMessage> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ErrorMessage message = ErrorMessage.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("Bạn không có quyền truy cập chức năng này")
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(message, HttpStatus.FORBIDDEN);
    }

    // 5. Bắt các lỗi hệ thống không xác định (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleGeneralExceptions(Exception ex, HttpServletRequest request) {
        ErrorMessage message = ErrorMessage.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Lỗi hệ thống bất ngờ (Ghost in the Shell). Vui lòng thử lại sau!")
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
