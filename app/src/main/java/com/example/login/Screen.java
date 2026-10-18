package com.example.login;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

abstract class Screen extends Activity {
  protected TravelStore store;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    store = new TravelStore(this);
  }

  protected LinearLayout page() {
    ScrollView s = new ScrollView(this);
    s.setFillViewport(true);
    s.setBackgroundColor(Ui.BG);
    LinearLayout root = Ui.column(this);
    int p = Ui.dp(this, 24);
    root.setPadding(p, p, p, p);
    s.addView(root);
    setContentView(s);
    return root;
  }

  protected boolean signedIn() {
    if (store.user() > 0) return true;
    login();
    return false;
  }

  protected void login() {
    startActivity(
        new Intent(this, MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    finish();
  }

  protected void message(String text) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show();
  }

  @Override
  public void onDestroy() {
    store.close();
    super.onDestroy();
  }
}
