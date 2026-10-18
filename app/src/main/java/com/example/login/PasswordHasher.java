package com.example.login;

import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
  private PasswordHasher() {}

  public static String hash(String password) {
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return "600000:"
        + Base64.getEncoder().encodeToString(salt)
        + ":"
        + Base64.getEncoder().encodeToString(derive(password, salt, 600000));
  }

  public static boolean verify(String password, String stored) {
    try {
      String[] p = stored.split(":");
      if (p.length != 3) return false;
      int rounds = Integer.parseInt(p[0]);
      if (rounds < 100000 || rounds > 1000000) return false;
      byte[] salt = Base64.getDecoder().decode(p[1]), expected = Base64.getDecoder().decode(p[2]);
      return salt.length == 16
          && expected.length == 32
          && MessageDigest.isEqual(expected, derive(password, salt, rounds));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static byte[] derive(String password, byte[] salt, int rounds) {
    char[] chars = password.toCharArray();
    PBEKeySpec spec = new PBEKeySpec(chars, salt, rounds, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Password protection unavailable.", e);
    } finally {
      spec.clearPassword();
      Arrays.fill(chars, '\0');
    }
  }
}
