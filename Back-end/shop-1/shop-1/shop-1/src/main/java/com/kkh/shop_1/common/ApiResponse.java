package com.kkh.shop_1.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 * 모든 API 응답의 공통 형식을 정의하는 객체
 *
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    /**
     *
     * 성공 응답 (데이터 포함)
     *
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", null, data);
    }

    /**
     *
     * 성공 응답 (데이터 없음)
     *
     */
    public static <T> ApiResponse<T> successNoData() {
        return new ApiResponse<>(true, "OK", null, null);
    }

    /**
     *
     * 실패 응답 (코드 및 메시지)
     *
     */
    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }

    /**
     *
     * 실패 응답 (기본 에러 코드)
     *
     */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, "ERROR", message, null);
    }
}