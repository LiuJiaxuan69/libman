package com.example.demo.common;

import java.math.BigInteger;
import java.util.UUID;

public class TimeBase62UUIDGenerator {

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static String generate() {
        long timestamp = System.currentTimeMillis(); // 保留数字形式
        UUID uuid = UUID.randomUUID();
        String uuidPart = encodeUnsignedLong(uuid.getMostSignificantBits())
                + encodeUnsignedLong(uuid.getLeastSignificantBits());
        return timestamp + uuidPart; // 时间戳数字 + Base62 UUID
    }

    // 将 long 看作无符号数并转换为 base62 字符串，避免 Long.MIN_VALUE 取负溢出问题
    private static String encodeUnsignedLong(long value) {
        BigInteger unsigned = BigInteger.valueOf(value);
        if (value < 0) {
            // 把负数当作 64 位无符号数处理
            unsigned = unsigned.add(BigInteger.ONE.shiftLeft(64));
        }
        if (unsigned.equals(BigInteger.ZERO)) return "0";
        StringBuilder sb = new StringBuilder();
        BigInteger base = BigInteger.valueOf(62);
        while (unsigned.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] dr = unsigned.divideAndRemainder(base);
            unsigned = dr[0];
            int rem = dr[1].intValue();
            sb.append(BASE62.charAt(rem));
        }
        return sb.reverse().toString();
    }

    public static void main(String[] args) {
        System.out.println(TimeBase62UUIDGenerator.generate());
        System.out.println(TimeBase62UUIDGenerator.generate());
    }
}
