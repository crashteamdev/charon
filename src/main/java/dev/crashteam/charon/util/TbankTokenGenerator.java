package dev.crashteam.charon.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

public class TbankTokenGenerator {

    public static String generateInitToken(Long amount, String customerKey, String description,
                                           String orderId, String recurrent, String terminalKey,
                                           String secretKey) {
        Map<String, String> params = new TreeMap<>();
        params.put("Amount", String.valueOf(amount));
        if (customerKey != null) {
            params.put("CustomerKey", customerKey);
        }
        if (description != null) {
            params.put("Description", description);
        }
        params.put("OrderId", orderId);
        if (recurrent != null) {
            params.put("Recurrent", recurrent);
        }
        params.put("TerminalKey", terminalKey);

        return generateToken(params, secretKey);
    }

    public static String generateChargeToken(String paymentId, String rebillId,
                                             String terminalKey, String secretKey) {
        Map<String, String> params = new TreeMap<>();
        params.put("PaymentId", paymentId);
        params.put("RebillId", rebillId);
        params.put("TerminalKey", terminalKey);

        return generateToken(params, secretKey);
    }

    public static String generateGetStateToken(String paymentId, String terminalKey, String secretKey) {
        Map<String, String> params = new TreeMap<>();
        params.put("PaymentId", paymentId);
        params.put("TerminalKey", terminalKey);

        return generateToken(params, secretKey);
    }

    private static String generateToken(Map<String, String> params, String secretKey) {
        StringBuilder concatenated = new StringBuilder();
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            concatenated.append(entry.getKey())
                      .append('=')
                      .append(entry.getValue());
        }
        concatenated.append(secretKey);
        
        return sha256(concatenated.toString());
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

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
