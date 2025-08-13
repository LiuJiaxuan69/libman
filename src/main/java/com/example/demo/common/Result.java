package com.example.demo.common;

import lombok.Data;

@Data
public class Result<T> {
    private ResultStatus status;
    private String errorMessage;
    private T data;

    public Result() {
    }

    public Result(ResultStatus status, String errorMessage, T data) {
        this.status = status;
        this.errorMessage = errorMessage;
        this.data = data;
    }

    // 业务执行成功返回方法
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setStatus(ResultStatus.SUCCESS);
        result.setErrorMessage(null);
        result.setData(data);
        return result;
    }

    // 业务执行失败返回方法
    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<>();
        result.setStatus(ResultStatus.FAIL);
        result.setErrorMessage(msg);
        result.setData(null);
        return result;
    }

    // 业务执行失败返回方法
    public static <T> Result<T> unlogin() {
        Result<T> result = new Result<>();
        result.setStatus(ResultStatus.UNLOGIN);
        result.setErrorMessage("用户未登录");
        result.setData(null);
        return result;
    }
}
