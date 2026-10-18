package com.example.login;

import java.time.LocalDate;
import java.util.*;

public final class CoreChecks {
  static int count;

  static void check(boolean v) {
    count++;
    if (!v) throw new AssertionError("Check " + count);
  }

  static void rejects(Runnable r) {
    try {
      r.run();
      throw new AssertionError("Accepted invalid input");
    } catch (IllegalArgumentException | ArithmeticException expected) {
      count++;
    }
  }

  public static void main(String[] args) {
    check(Rules.email(" Person@Example.COM ").equals("person@example.com"));
    for (String e : new String[] {"", "a@b", "a b@example.com", "a@@b.com", "no-at.example.com"})
      rejects(() -> Rules.email(e));
    rejects(() -> Rules.name(" "));
    rejects(() -> Rules.password("short"));
    rejects(() -> Rules.password(new String(new char[129])));
    String pass = "a sufficiently long passphrase", hash = PasswordHasher.hash(pass);
    check(!hash.equals(PasswordHasher.hash(pass)));
    check(!hash.contains(pass));
    check(PasswordHasher.verify(pass, hash));
    check(!PasswordHasher.verify("wrong", hash));
    for (String bad :
        new String[] {
          "", "broken", "1:AA:AA", "999999999:AA:AA", "600000:####:####", "600000:AA==:AA=="
        }) check(!PasswordHasher.verify(pass, bad));
    check(Rules.total(15000, 3) == 45000);
    rejects(() -> Rules.total(-1, 2));
    rejects(() -> Rules.total(1, 0));
    rejects(() -> Rules.total(1, 31));
    rejects(() -> Rules.total(Integer.MAX_VALUE, 30));
    String tomorrow = LocalDate.now().plusDays(1).toString();
    Rules.booking(tomorrow, 30, 6);
    rejects(() -> Rules.booking("2024-02-30", 1, 1));
    rejects(() -> Rules.booking(LocalDate.now().minusDays(1).toString(), 1, 1));
    rejects(() -> Rules.booking(LocalDate.now().plusYears(3).toString(), 1, 1));
    rejects(() -> Rules.booking(tomorrow, 1, 0));
    rejects(() -> Rules.booking(tomorrow, 1, 7));
    check(Catalog.all().size() == 10);
    Set<String> ids = new HashSet<>();
    for (Catalog.Place p : Catalog.all()) {
      check(ids.add(p.id));
      check(p.rating > 0 && p.rating <= 5);
      check(!p.hotel || p.cents > 0);
      check(Catalog.get(p.id) == p);
      check(Catalog.gallery(p.id).length == 3);
    }
    check(Catalog.search("", 1).size() == 5);
    check(Catalog.search("", 2).size() == 5);
    check(Catalog.search(" FRANCE ", 0).size() == 2);
    check(Catalog.search("nothing matches", 0).isEmpty());
    Locale old = Locale.getDefault();
    try {
      Locale.setDefault(new Locale("tr", "TR"));
      check(Catalog.search("PARIS", 0).size() == 2);
    } finally {
      Locale.setDefault(old);
    }
    rejects(() -> Catalog.get("invalid"));
    System.out.println("PASS: " + count + " core checks");
  }
}
