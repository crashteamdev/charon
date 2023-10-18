package dev.crashteam.charon.util;

import dev.crashteam.charon.config.PromoCodeConfig;
import java.util.Random;

public class PromoCodeGenerator {

    public static String getPromoCode(PromoCodeConfig promoCodeConfig) {
        var random = new Random(System.currentTimeMillis());
        var sb = new StringBuilder();
        var chars = promoCodeConfig.getCharset().toCharArray();
        var pattern = promoCodeConfig.getPattern().toCharArray();
        if (promoCodeConfig.getPrefix() != null) {
            sb.append(promoCodeConfig.getPrefix());
        }
        for (int i = 0; i < chars.length; i++) {
            if (pattern[i] == PromoCodeConfig.PATTERN_PLACEHOLDER) {
                sb.append(chars[random.nextInt(chars.length)]);
            } else {
                sb.append(pattern[i]);
            }
        }
        return sb.toString();
    }
}
