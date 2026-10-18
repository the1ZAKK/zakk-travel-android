package com.example.login;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import java.util.*;

public final class TravelStore extends SQLiteOpenHelper {
  private final Context app;

  public TravelStore(Context context) {
    super(context.getApplicationContext(), "travel-v2.db", null, 1);
    app = context.getApplicationContext();
  }

  @Override
  public void onConfigure(SQLiteDatabase db) {
    db.setForeignKeyConstraintsEnabled(true);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE users(id INTEGER PRIMARY KEY,name TEXT NOT NULL,email TEXT NOT NULL"
            + " UNIQUE,password TEXT NOT NULL)");
    db.execSQL(
        "CREATE TABLE favorites(user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE"
            + " CASCADE,item_id TEXT NOT NULL,PRIMARY KEY(user_id,item_id))");
    db.execSQL(
        "CREATE TABLE bookings(id INTEGER PRIMARY KEY,user_id INTEGER NOT NULL REFERENCES users(id)"
            + " ON DELETE CASCADE,item_id TEXT NOT NULL,arrival TEXT NOT NULL,nights INTEGER NOT"
            + " NULL CHECK(nights BETWEEN 1 AND 30),guests INTEGER NOT NULL CHECK(guests BETWEEN 1"
            + " AND 6),total_cents INTEGER NOT NULL"
            + " CHECK(total_cents>=0),UNIQUE(user_id,item_id,arrival))");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    throw new IllegalStateException("An explicit database migration is required.");
  }

  public long register(String name, String email, String password) {
    String n = Rules.name(name), e = Rules.email(email);
    Rules.password(password);
    ContentValues v = new ContentValues();
    v.put("name", n);
    v.put("email", e);
    v.put("password", PasswordHasher.hash(password));
    try {
      return getWritableDatabase().insertOrThrow("users", null, v);
    } catch (SQLiteConstraintException ex) {
      throw new IllegalArgumentException("This email already has an account.");
    }
  }

  public long login(String email, String password) {
    String e = Rules.email(email);
    Rules.password(password);
    try (Cursor c =
        getReadableDatabase()
            .query(
                "users",
                new String[] {"id", "password"},
                "email=?",
                new String[] {e},
                null,
                null,
                null)) {
      if (c.moveToFirst() && PasswordHasher.verify(password, c.getString(1))) {
        long id = c.getLong(0);
        prefs().edit().putLong("user", id).apply();
        return id;
      }
    }
    throw new IllegalArgumentException("Email or password is incorrect.");
  }

  private SharedPreferences prefs() {
    return app.getSharedPreferences("travel-session", 0);
  }

  public long user() {
    long id = prefs().getLong("user", -1);
    try (Cursor c =
        getReadableDatabase()
            .query(
                "users", new String[] {"id"}, "id=?", new String[] {"" + id}, null, null, null)) {
      return c.moveToFirst() ? id : -1;
    }
  }

  private long requireUser() {
    long id = user();
    if (id < 0) throw new IllegalStateException("Please sign in.");
    return id;
  }

  public void logout() {
    prefs().edit().clear().apply();
  }

  public String name() {
    try (Cursor c =
        getReadableDatabase()
            .query(
                "users",
                new String[] {"name"},
                "id=?",
                new String[] {"" + requireUser()},
                null,
                null,
                null)) {
      return c.moveToFirst() ? c.getString(0) : "Traveller";
    }
  }

  public Set<String> favorites() {
    Set<String> out = new HashSet<>();
    try (Cursor c =
        getReadableDatabase()
            .query(
                "favorites",
                new String[] {"item_id"},
                "user_id=?",
                new String[] {"" + requireUser()},
                null,
                null,
                null)) {
      while (c.moveToNext()) out.add(c.getString(0));
    }
    return out;
  }

  public boolean toggle(String id) {
    Catalog.get(id);
    long uid = requireUser();
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      boolean added =
          db.delete("favorites", "user_id=? AND item_id=?", new String[] {"" + uid, id}) == 0;
      if (added) {
        ContentValues v = new ContentValues();
        v.put("user_id", uid);
        v.put("item_id", id);
        db.insertOrThrow("favorites", null, v);
      }
      db.setTransactionSuccessful();
      return added;
    } finally {
      db.endTransaction();
    }
  }

  public long book(String id, String arrival, int nights, int guests) {
    Catalog.Place p = Catalog.get(id);
    Rules.booking(arrival, nights, guests);
    if (!p.hotel && nights != 1) throw new IllegalArgumentException("City visits use one day.");
    ContentValues v = new ContentValues();
    v.put("user_id", requireUser());
    v.put("item_id", id);
    v.put("arrival", arrival);
    v.put("nights", nights);
    v.put("guests", guests);
    v.put("total_cents", Rules.total(p.cents, nights));
    try {
      return getWritableDatabase().insertOrThrow("bookings", null, v);
    } catch (SQLiteConstraintException ex) {
      throw new IllegalArgumentException("This place is already planned for that date.");
    }
  }

  public static final class Booking {
    public final long id;
    public final Catalog.Place place;
    public final String date;
    public final int nights, guests, total;

    Booking(Cursor c) {
      id = c.getLong(0);
      place = Catalog.get(c.getString(1));
      date = c.getString(2);
      nights = c.getInt(3);
      guests = c.getInt(4);
      total = c.getInt(5);
    }
  }

  public List<Booking> bookings() {
    List<Booking> out = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .query(
                "bookings",
                new String[] {"id", "item_id", "arrival", "nights", "guests", "total_cents"},
                "user_id=?",
                new String[] {"" + requireUser()},
                null,
                null,
                "arrival ASC,id ASC")) {
      while (c.moveToNext()) out.add(new Booking(c));
    }
    return out;
  }

  public void cancel(long id) {
    getWritableDatabase()
        .delete("bookings", "id=? AND user_id=?", new String[] {"" + id, "" + requireUser()});
  }

  public void deleteAccount() {
    getWritableDatabase().delete("users", "id=?", new String[] {"" + requireUser()});
    logout();
  }
}
