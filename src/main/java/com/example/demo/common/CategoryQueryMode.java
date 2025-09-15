package com.example.demo.common;

public enum CategoryQueryMode {
    INTERSECTION(1),
    UNION(2);

    private final int code;

    CategoryQueryMode(int code) { this.code = code; }

    public int getCode() { return code; }

    public static CategoryQueryMode fromCode(int code) {
        for (CategoryQueryMode m : values()) {
            if (m.code == code) return m;
        }
        return INTERSECTION;
    }
}
