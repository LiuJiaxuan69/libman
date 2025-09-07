package com.example.demo.common;

import java.util.UUID;

public class TimeBase62UUIDGenerator {

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static String generate() {
        long timestamp = System.currentTimeMillis(); // 保留数字形式
        String uuidPart = encode(UUID.randomUUID().getMostSignificantBits())
                        + encode(UUID.randomUUID().getLeastSignificantBits());
        return timestamp + uuidPart; // 时间戳数字 + Base62 UUID
    }

    private static String encode(long value) {
        StringBuilder sb = new StringBuilder();
        long val = value < 0 ? -value : value; // 取绝对值
        if (val == 0) return "0"; // 避免空串
        while (val > 0) {
            sb.append(BASE62.charAt((int)(val % 62)));
            val /= 62;
        }
        return sb.reverse().toString();
    }

    public static void main(String[] args) {
        System.out.println(TimeBase62UUIDGenerator.generate());
        System.out.println(TimeBase62UUIDGenerator.generate());
    }
}
