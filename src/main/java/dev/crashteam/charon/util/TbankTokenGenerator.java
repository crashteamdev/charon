package dev.crashteam.charon.util;

import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

public class TbankTokenGenerator {

    public static String generateInitToken(String terminalKey, Long amount, String orderId,
                                           String description, String customerKey, String secretKey, String recurrent) {
        Map<String, String> params = new TreeMap<>();
        params.put("TerminalKey", terminalKey);
        params.put("Amount", amount.toString());
        params.put("OrderId", orderId);

        if (description != null && !description.isEmpty()) {
            params.put("Description", description);
        }
        if (customerKey != null && !customerKey.isEmpty()) {
            params.put("CustomerKey", customerKey);
        }
        if (StringUtils.hasText(recurrent)) {
            params.put("Recurrent", recurrent);
        }

        params.put("Password", secretKey);

        return calculateHash(params);
    }

    public static String generateChargeToken(String terminalKey, String paymentId, String secretKey, String rebillId) {
        Map<String, String> params = new TreeMap<>();
        params.put("TerminalKey", terminalKey);
        params.put("PaymentId", paymentId);
        params.put("Password", secretKey);
        params.put("RebillId", rebillId);

        return calculateHash(params);
    }

    public static String generateGetStateToken(String paymentId, String terminalKey, String secretKey) {
        Map<String, String> params = new TreeMap<>();
        params.put("TerminalKey", terminalKey);
        params.put("PaymentId", paymentId);
        params.put("Password", secretKey);

        return calculateHash(params);
    }

    private static String calculateHash(Map<String, String> params) {
        try {
            StringBuilder concatenated = new StringBuilder();
            for (String value : params.values()) {
                concatenated.append(value);
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(concatenated.toString().getBytes(StandardCharsets.UTF_8));


            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
} 
