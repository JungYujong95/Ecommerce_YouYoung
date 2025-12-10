package org.example.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 메서드입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "잘못된 타입의 값입니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C005", "접근이 거부되었습니다"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 리프레시 토큰입니다"),
    LOGOUT_USER(HttpStatus.UNAUTHORIZED, "A005", "로그아웃된 사용자입니다"),

    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원을 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M002", "이미 사용 중인 이메일입니다"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "M003", "비밀번호가 일치하지 않습니다"),
    INACTIVE_MEMBER(HttpStatus.FORBIDDEN, "M004", "비활성화된 회원입니다"),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다"),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "P002", "재고가 부족합니다"),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "주문을 찾을 수 없습니다"),
    ORDER_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "O002", "이미 취소된 주문입니다"),
    ORDER_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "O003", "취소할 수 없는 주문 상태입니다"),
    ORDER_LOCK_FAILED(HttpStatus.CONFLICT, "O004", "다른 요청이 처리 중입니다. 잠시 후 다시 시도해주세요");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
