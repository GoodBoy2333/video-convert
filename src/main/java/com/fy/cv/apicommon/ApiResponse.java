package com.fy.cv.apicommon;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/**
 * <p>
 * 通用API接口返回
 * </p>
 */
public class ApiResponse<T> implements Serializable {
    /**
     * 通用返回状态
     */
    private Integer status;
    /**
     * 通用返回信息
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String message;
    /**
     * 通用返回数据
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private T data;

    public static ApiResponse success(String message) {
        return new ApiResponse(1, message, "");
    }

    public static ApiResponse fail(String message) {
        return new ApiResponse(0, message, "");
    }

    public static <T> ApiResponse success(String message, T data) {
        return new ApiResponse(1, message, data);
    }

    public static <T> ApiResponse fail(String message, T data) {
        return new ApiResponse(0, message, data);
    }

    public static <T> ApiResponse success(T data) {
        return new ApiResponse(1,"", data);
    }

    public static <T> ApiResponse fail(T data) {
        return new ApiResponse(0, "", data);
    }

    public ApiResponse() {}

    public ApiResponse(Integer status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
