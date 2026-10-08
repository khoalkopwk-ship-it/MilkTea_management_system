package vn.edu.ute.milktea.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .traceId(traceId)
                .error(ErrorResponse.ErrorDetail.builder()
                        .code(ex.getErrorCode().name())
                        .message(ex.getMessage())
                        .fieldErrors(ex.getFieldErrors())
                        .build())
                .build();
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .traceId(UUID.randomUUID().toString())
                .error(ErrorResponse.ErrorDetail.builder()
                        .code(ErrorCode.VALIDATION_FAILED.name())
                        .message("Dữ liệu đầu vào không hợp lệ")
                        .fieldErrors(fieldErrors)
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .traceId(UUID.randomUUID().toString())
                .error(ErrorResponse.ErrorDetail.builder()
                        .code(ErrorCode.ACCESS_DENIED.name())
                        .message("Bạn không có quyền thực hiện thao tác này")
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .traceId(UUID.randomUUID().toString())
                .error(ErrorResponse.ErrorDetail.builder()
                        .code(ErrorCode.AUTH_INVALID_CREDENTIALS.name())
                        .message("Thông tin xác thực không hợp lệ")
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .traceId(UUID.randomUUID().toString())
                .error(ErrorResponse.ErrorDetail.builder()
                        .code(ErrorCode.INTERNAL_ERROR.name())
                        .message("Đã xảy ra lỗi hệ thống: " + ex.getMessage())
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
