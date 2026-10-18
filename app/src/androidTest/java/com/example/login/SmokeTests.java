package com.example.login;

import android.app.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.time.LocalDate;
import java.util.*;

/** Dependency-free device tests using real Android SQLite, views and accessibility actions. */
public final class SmokeTests extends Instrumentation {
  private Context context;
  private TravelStore db;
  private int failures;
  private int executed;
  private boolean skipAuthentication;
  private boolean captureUi;
  private static final String PASS = "test password 123";

  public static final class IsolatedApplication extends Application {
    private String isolated(String name) {
      return "instrumented-" + name;
    }

    @Override
    public File getDatabasePath(String n) {
      return getBaseContext().getDatabasePath(isolated(n));
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String n, int m, SQLiteDatabase.CursorFactory f) {
      return getBaseContext().openOrCreateDatabase(isolated(n), m, f);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(
        String n, int m, SQLiteDatabase.CursorFactory f, DatabaseErrorHandler h) {
      return getBaseContext().openOrCreateDatabase(isolated(n), m, f, h);
    }

    @Override
    public boolean deleteDatabase(String n) {
      return getBaseContext().deleteDatabase(isolated(n));
    }

    @Override
    public SharedPreferences getSharedPreferences(String n, int m) {
      return getBaseContext().getSharedPreferences(isolated(n), m);
    }
  }

  @Override
  public Application newApplication(ClassLoader cl, String name, Context context)
      throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    return super.newApplication(cl, IsolatedApplication.class.getName(), context);
  }

  @Override
  public void onCreate(Bundle arguments) {
    super.onCreate(arguments);
    skipAuthentication =
        arguments != null && "true".equals(arguments.getString("skipAuthentication"));
    captureUi = arguments != null && "true".equals(arguments.getString("captureUi"));
    start();
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static void rejects(Runnable r) {
    try {
      r.run();
      throw new AssertionError("Invalid action accepted");
    } catch (IllegalArgumentException | IllegalStateException expected) {
    }
  }

  private void status(String test, int current, int code, String stream) {
    Bundle b = new Bundle();
    b.putString("class", getClass().getName());
    b.putString("test", test);
    b.putInt("current", executed);
    b.putInt("numtests", skipAuthentication ? 6 : 7);
    b.putString("stream", stream);
    sendStatus(code, b);
  }

  private void run(String name, int index, Runnable test) {
    if (skipAuthentication && name.equals("authentication")) return;
    executed++;
    status(name, index, 1, "\n" + name + ": ");
    try {
      check(context instanceof IsolatedApplication, "Isolated test storage is required");
      context.deleteDatabase("travel-v2.db");
      context.getSharedPreferences("travel-session", 0).edit().clear().commit();
      db = new TravelStore(context);
      test.run();
      status(name, index, 0, "PASS\n");
    } catch (Throwable ex) {
      failures++;
      Bundle b = new Bundle();
      b.putString("class", getClass().getName());
      b.putString("test", name);
      b.putString("stack", android.util.Log.getStackTraceString(ex));
      b.putString("stream", "FAIL: " + ex + "\n");
      sendStatus(-2, b);
    } finally {
      if (db != null) {
        db.close();
        db = null;
      }
    }
  }

  @Override
  public void onStart() {
    context = getTargetContext().getApplicationContext();
    run(
        "authentication",
        1,
        () -> {
          long id = db.register("Zakaria", "PERSON@example.com", PASS);
          check(db.login(" person@example.com ", PASS) == id, "Normalized login");
          check(db.name().equals("Zakaria"), "Display name");
          try (Cursor c = db.getReadableDatabase().rawQuery("SELECT password FROM users", null)) {
            check(c.moveToFirst() && !c.getString(0).equals(PASS), "Passwords must be hashed");
          }
          db.logout();
          check(db.user() < 0, "Sign out");
          rejects(() -> db.login("person@example.com", "wrong password"));
          rejects(() -> db.register("Duplicate", "person@example.com", PASS));
        });
    run(
        "accountIsolation",
        2,
        () -> {
          first();
          db.toggle("paris");
          long id = db.book("hotel-paris", tomorrow(), 3, 2);
          db.logout();
          seed("Second", "second@example.com");
          session("second@example.com");
          check(db.favorites().isEmpty() && db.bookings().isEmpty(), "Private account records");
          db.cancel(id);
          db.logout();
          session("first@example.com");
          check(
              db.favorites().contains("paris") && db.bookings().size() == 1,
              "Other account cannot remove plans");
          check(db.bookings().get(0).total == 45000, "Exact money total");
        });
    run(
        "persistenceAndDeletion",
        3,
        () -> {
          first();
          db.toggle("tokyo");
          db.book("tokyo", tomorrow(), 1, 1);
          db.close();
          db = new TravelStore(context);
          check(
              db.user() > 0 && db.favorites().contains("tokyo") && db.bookings().size() == 1,
              "Persistence across database reopen");
          db.deleteAccount();
          check(db.user() < 0, "Deleted session");
          for (String table : new String[] {"users", "favorites", "bookings"})
            try (Cursor c =
                db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + table, null)) {
              check(c.moveToFirst() && c.getInt(0) == 0, "Foreign-key cascade");
            }
        });
    run(
        "planValidation",
        4,
        () -> {
          first();
          db.book("paris", tomorrow(), 1, 1);
          rejects(() -> db.book("paris", tomorrow(), 1, 1));
          rejects(() -> db.book("paris", "2020-01-01", 1, 1));
          rejects(() -> db.book("paris", tomorrow(), 2, 1));
          rejects(() -> db.book("missing", tomorrow(), 1, 1));
          check(db.bookings().size() == 1, "Invalid plans leave no records");
          db.cancel(db.bookings().get(0).id);
          check(db.bookings().isEmpty(), "Cancel own plan");
        });
    run(
        "quotedInputAndSessionGuard",
        5,
        () -> {
          seed("O'Connor", "o'connor@example.com");
          session("o'connor@example.com");
          check(db.name().equals("O'Connor"), "Quoted values are bound");
          db.toggle("rome");
          db.logout();
          rejects(() -> db.login("x' OR 1=1 --@example.com", PASS));
          rejects(() -> db.favorites());
          session("o'connor@example.com");
          check(db.favorites().contains("rome"), "Quoted identifier round trip");
        });
    run(
        "deleteOnlyOwnAccount",
        6,
        () -> {
          first();
          db.toggle("paris");
          db.logout();
          seed("Second", "second@example.com");
          session("second@example.com");
          db.toggle("tokyo");
          db.deleteAccount();
          session("first@example.com");
          check(
              db.favorites().size() == 1 && db.favorites().contains("paris"),
              "Other account preserved");
        });
    run("searchSavePlanRemoveUi", 7, this::uiJourney);
    Bundle result = new Bundle();
    result.putString("stream", "\nTests run: " + executed + ", failures: " + failures + "\n");
    result.putInt("failures", failures);
    finish(failures == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
  }

  private void first() {
    seed("First", "first@example.com");
    session("first@example.com");
  }

  // Non-authentication scenarios use known fixtures to avoid repeating expensive KDF work.
  // These helpers exist only in the separate instrumentation APK.
  private void seed(String name, String email) {
    ContentValues values = new ContentValues();
    values.put("name", name);
    values.put("email", email);
    values.put(
        "password", "600000:MDEyMzQ1Njc4OWFiY2RlZg==:5xs5rOmdstTQttiAQuZ7lYmb9oA/cbw8I/ANhYWVN6g=");
    db.getWritableDatabase().insertOrThrow("users", null, values);
  }

  private void session(String email) {
    try (Cursor c =
        db.getReadableDatabase()
            .query(
                "users", new String[] {"id"}, "email=?", new String[] {email}, null, null, null)) {
      check(c.moveToFirst(), "Fixture account exists");
      context
          .getSharedPreferences("travel-session", 0)
          .edit()
          .putLong("user", c.getLong(0))
          .commit();
    }
  }

  private String tomorrow() {
    return LocalDate.now().plusDays(1).toString();
  }

  private View find(View root, String text) {
    if (root instanceof TextView && text.contentEquals(((TextView) root).getText())) return root;
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int i = 0; i < group.getChildCount(); i++) {
        View hit = find(group.getChildAt(i), text);
        if (hit != null) return hit;
      }
    }
    return null;
  }

  private void click(Activity a, String text) {
    runOnMainSync(
        () -> {
          View v = find(a.getWindow().getDecorView(), text);
          check(v != null, "Missing control: " + text);
          v.performClick();
        });
    waitForIdleSync();
  }

  private void accessibleClick(String text) {
    String observed = "";
    for (int i = 0; i < 100; i++) {
      AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
      if (root != null) {
        ArrayDeque<AccessibilityNodeInfo> nodes = new ArrayDeque<>();
        nodes.add(root);
        StringBuilder labels = new StringBuilder();
        while (!nodes.isEmpty()) {
          AccessibilityNodeInfo n = nodes.removeFirst();
          if (n.getText() != null) labels.append(n.getText()).append(" | ");
          if (n.getText() != null
              && text.equalsIgnoreCase(n.getText().toString())
              && n.isClickable()
              && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            waitForIdleSync();
            return;
          }
          for (int child = 0; child < n.getChildCount(); child++) {
            AccessibilityNodeInfo next = n.getChild(child);
            if (next != null) nodes.add(next);
          }
        }
        observed = labels.toString();
      }
      android.os.SystemClock.sleep(100);
    }
    throw new AssertionError("Missing dialog control: " + text + "; observed: " + observed);
  }

  private void uiJourney() {
    first();
    HomeActivity home =
        (HomeActivity)
            startActivitySync(
                new Intent(context, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    DetailActivity detail = null;
    try {
      waitForIdleSync();
      capture("home");
      runOnMainSync(() -> ((EditText) home.findViewById(200)).setText("FRANCE"));
      waitForIdleSync();
      check(
          ((RecyclerView) home.findViewById(201)).getAdapter().getItemCount() == 2,
          "Country search");
      click(home, "Cities");
      check(
          ((RecyclerView) home.findViewById(201)).getAdapter().getItemCount() == 1, "Type filter");
      click(home, "♡ Save");
      check(db.favorites().contains("paris"), "Save control persists");
      click(home, "Saved");
      check(((RecyclerView) home.findViewById(201)).getAdapter().getItemCount() == 1, "Saved tab");
      ActivityMonitor monitor = addMonitor(DetailActivity.class.getName(), null, false);
      click(home, "Explore →");
      detail = (DetailActivity) waitForMonitorWithTimeout(monitor, 15000);
      check(detail != null, "Detail navigation");
      waitForIdleSync();
      DetailActivity active = detail;
      capture("detail");
      runOnMainSync(() -> active.findViewById(300).performClick());
      waitForIdleSync();
      capture("planning-dialog");
      accessibleClick("Save plan");
      check(db.bookings().size() == 1, "Plan control persists");
      runOnMainSync(active::finish);
      waitForIdleSync();
      click(home, "Trips");
      capture("trips");
      check(find(home.getWindow().getDecorView(), "Local itinerary entry") != null, "Trips page");
      click(home, "Remove plan");
      accessibleClick("Remove");
      check(db.bookings().isEmpty(), "Remove plan control");
    } finally {
      DetailActivity active = detail;
      if (active != null) runOnMainSync(active::finish);
      runOnMainSync(home::finish);
      waitForIdleSync();
    }
  }

  private void capture(String name) {
    if (!captureUi) return;
    android.os.SystemClock.sleep(name.equals("trips") ? 3000 : 350);
    android.graphics.Bitmap bitmap = getUiAutomation().takeScreenshot();
    check(bitmap != null, "Screenshot available");
    try (java.io.FileOutputStream out =
        new java.io.FileOutputStream(
            new File(context.getCacheDir(), "instrumented-review-" + name + ".png"))) {
      check(
          bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out),
          "Screenshot written");
    } catch (java.io.IOException ex) {
      throw new AssertionError(ex);
    } finally {
      bitmap.recycle();
    }
  }
}
