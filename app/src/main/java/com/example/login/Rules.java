package com.example.login;

import java.time.LocalDate;
import java.util.Locale;

public final class Rules {
  private Rules() {}

  public static String email(String value) {
    String e = value.trim().toLowerCase(Locale.ROOT);
    if (e.length() > 254 || !e.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
      throw new IllegalArgumentException("Enter a valid email address.");
    return e;
  }

  public static String name(String value) {
    String n = value.trim();
    if (n.length() < 2 || n.length() > 60)
      throw new IllegalArgumentException("Name must have 2–60 characters.");
    return n;
  }

  public static void password(String value) {
    if (value.length() < 10 || value.length() > 128)
      throw new IllegalArgumentException("Use a password of 10–128 characters.");
  }

  public static int total(int cents, int nights) {
    if (cents < 0 || nights < 1 || nights > 30)
      throw new IllegalArgumentException("Choose 1–30 nights.");
    return Math.multiplyExact(cents, nights);
  }

  public static void booking(String date, int nights, int guests) {
    LocalDate d;
    try {
      d = LocalDate.parse(date);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Choose a valid arrival date.");
    }
    if (d.isBefore(LocalDate.now()) || d.isAfter(LocalDate.now().plusYears(2)))
      throw new IllegalArgumentException("Arrival must be between today and two years ahead.");
    total(0, nights);
    if (guests < 1 || guests > 6) throw new IllegalArgumentException("Choose 1–6 guests.");
  }
}
