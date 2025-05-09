package org.example.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtils {

    public static boolean isStrongPassword(String password) {
        System.out.println("Validating password strength for: " + password.replaceAll(".", "*"));
        if (password.length() < 8) {
            System.out.println("Password too short (less than 8 characters)");
            return false;
        }

        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                hasDigit = true;
                break;
            }
        }

        boolean hasLetter = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
                break;
            }
        }

        boolean isStrong = hasDigit && hasLetter;
        System.out.println("Password strength validation result: " + isStrong);
        return isStrong;
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password: " + e.getMessage());
        }
    }
}