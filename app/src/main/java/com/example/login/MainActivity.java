package com.example.login;

import android.content.*;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import java.util.concurrent.*;

public final class MainActivity extends Screen {
  private static final ExecutorService WORK = Executors.newSingleThreadExecutor();
  private boolean register;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    if (store.user() > 0) {
      home();
      return;
    }
    register = state != null && state.getBoolean("register");
    form();
  }

  @Override
  public void onSaveInstanceState(Bundle state) {
    state.putBoolean("register", register);
    super.onSaveInstanceState(state);
  }

  private void home() {
    startActivity(
        new Intent(this, HomeActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    finish();
  }

  private void form() {
    LinearLayout root = page();
    if (!register) root.addView(Ui.image(this, R.drawable.london, 180));
    Ui.space(root, 18);
    root.addView(Ui.text(this, "ZAKK / TRAVEL", 13, Ui.GREEN, true));
    root.addView(
        Ui.text(
            this,
            register ? "Make room for\nsomething new." : "Your next chapter\nstarts here.",
            32,
            Ui.INK,
            true));
    root.addView(
        Ui.text(
            this,
            register
                ? "Create a local account for your saved places and trips."
                : "Discover a place. Save a favourite. Make a plan.",
            16,
            Ui.MUTED,
            false));
    EditText name =
        Ui.field(
            this, "Your name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, 100);
    if (register) root.addView(name);
    EditText email =
        Ui.field(
            this,
            "Email address",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            101);
    root.addView(email);
    EditText password = Ui.field(this, "Password • 10–128 characters", 129, 102);
    password.setSaveEnabled(false);
    root.addView(password);
    EditText confirm = Ui.field(this, "Confirm password", 129, 103);
    confirm.setSaveEnabled(false);
    if (register) root.addView(confirm);
    TextView error = Ui.text(this, "", 14, 0xFF9C2C21, false);
    error.setAccessibilityLiveRegion(1);
    root.addView(error);
    Button submit = Ui.button(this, register ? "Create account" : "Sign in", true);
    submit.setId(104);
    root.addView(submit);
    Button toggle = Ui.button(this, register ? "Back to sign in" : "Create an account", false);
    root.addView(toggle);
    toggle.setOnClickListener(
        v -> {
          register = !register;
          form();
        });
    root.addView(
        Ui.text(
            this,
            "Offline planner. No email is sent. Data stays in this installation; uninstalling"
                + " removes it.",
            13,
            Ui.MUTED,
            false));
    submit.setOnClickListener(
        v -> {
          String n = name.getText().toString(),
              e = email.getText().toString(),
              p = password.getText().toString();
          boolean creating = register;
          if (creating && !p.equals(confirm.getText().toString())) {
            error.setText("Passwords do not match.");
            return;
          }
          submit.setEnabled(false);
          toggle.setEnabled(false);
          submit.setText("Please wait…");
          error.setText("");
          Context app = getApplicationContext();
          WORK.execute(
              () -> {
                String problem = null;
                try (TravelStore db = new TravelStore(app)) {
                  if (creating) db.register(n, e, p);
                  else db.login(e, p);
                } catch (RuntimeException ex) {
                  problem =
                      ex instanceof IllegalArgumentException
                          ? ex.getMessage()
                          : "Unable to complete this action. Please try again.";
                }
                final String result = problem;
                runOnUiThread(
                    () -> {
                      if (isDestroyed() || isFinishing()) return;
                      submit.setEnabled(true);
                      toggle.setEnabled(true);
                      submit.setText(creating ? "Create account" : "Sign in");
                      password.setText("");
                      confirm.setText("");
                      if (result != null) error.setText(result);
                      else if (creating) {
                        register = false;
                        form();
                        message("Account created. Sign in to continue.");
                      } else home();
                    });
              });
        });
  }
}
