package dev.crashteam.charon.config;

import java.util.Arrays;

public class PromoCodeConfig {

    private static final String ALPHABETIC = "ABCDEFGHIJKLMNPQRSTUVWXYZ";
    private static final String ALPHANUMERIC = "123456789abcdefghijkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ";
    private static final String NUMBERS = "0123456789";
    public static final Character PATTERN_PLACEHOLDER = '#';

    private Integer length;
    private String charset;
    private String prefix;
    private String pattern;

    public PromoCodeConfig() {
        this(null, null, null, null);
    }

    public PromoCodeConfig(Integer length, String charset, String prefix, String pattern) {
        if (length == null) {
            length = 8;
        }
        if (charset == null) {
            charset = ALPHANUMERIC;
        }
        if (pattern == null) {
            var chars = new char[length];
            Arrays.fill(chars, PATTERN_PLACEHOLDER);
            pattern = String.valueOf(chars);
        }
        this.length = length;
        this.charset = charset;
        this.prefix = prefix;
        this.pattern = pattern;
    }

    public Integer getLength() {
        return length;
    }

    public String getCharset() {
        return charset;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getPattern() {
        return pattern;
    }
}
