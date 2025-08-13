package com.example.demo.common;

public enum BookStatus {
    DELETED(0, "无效"),
    NORMAL(1, "可借阅"),
    FORBIDDEN(2, "不可借阅");

    private Integer code;
    private String name;

    BookStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static BookStatus getNameByCode(Integer code) {
        switch (code) {
            case 0: return DELETED;
            case 1: return NORMAL;
            case 2: return FORBIDDEN;
        }
        return null;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
