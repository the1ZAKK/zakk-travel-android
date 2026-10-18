package com.example.login;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.Locale;

final class Ui {
  static final int BG = 0xFFF6F7F3, INK = 0xFF192B28, GREEN = 0xFF146C60, MUTED = 0xFF536860;

  static int dp(Context c, int n) {
    return Math.round(n * c.getResources().getDisplayMetrics().density);
  }

  static GradientDrawable bg(Context c, int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(c, radius));
    return d;
  }

  static LinearLayout column(Context c) {
    LinearLayout v = new LinearLayout(c);
    v.setOrientation(1);
    return v;
  }

  static LinearLayout row(Context c) {
    LinearLayout v = new LinearLayout(c);
    v.setGravity(Gravity.CENTER_VERTICAL);
    return v;
  }

  static TextView text(Context c, String value, int size, int color, boolean bold) {
    TextView t = new TextView(c);
    t.setText(value);
    t.setTextSize(size);
    t.setTextColor(color);
    if (bold) t.setTypeface(Typeface.DEFAULT, 1);
    t.setPadding(0, dp(c, 4), 0, dp(c, 4));
    return t;
  }

  static Button button(Context c, String title, boolean primary) {
    Button b = new Button(c);
    b.setText(title);
    b.setTextSize(15);
    b.setAllCaps(false);
    b.setTextColor(primary ? Color.WHITE : GREEN);
    b.setBackground(bg(c, primary ? GREEN : 0xFFE2EDE6, 16));
    b.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
    b.setMinHeight(dp(c, 50));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.topMargin = dp(c, 10);
    b.setLayoutParams(lp);
    return b;
  }

  static EditText field(Context c, String hint, int type, int id) {
    EditText e = new EditText(c);
    e.setId(id);
    e.setHint(hint);
    e.setContentDescription(hint);
    e.setTextSize(16);
    e.setTextColor(INK);
    e.setHintTextColor(MUTED);
    e.setSingleLine(true);
    e.setInputType(type);
    e.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
    e.setBackground(bg(c, Color.WHITE, 14));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.topMargin = dp(c, 12);
    e.setLayoutParams(lp);
    return e;
  }

  static ImageView image(Context c, int res, int h) {
    ImageView v = new ImageView(c);
    BitmapFactory.Options b = new BitmapFactory.Options();
    b.inJustDecodeBounds = true;
    b.inScaled = false;
    BitmapFactory.decodeResource(c.getResources(), res, b);
    int sample = 1;
    while (b.outWidth / (sample * 2) >= c.getResources().getDisplayMetrics().widthPixels
        && b.outHeight / (sample * 2) >= dp(c, h)) sample *= 2;
    BitmapFactory.Options o = new BitmapFactory.Options();
    o.inScaled = false;
    o.inSampleSize = sample;
    v.setImageBitmap(BitmapFactory.decodeResource(c.getResources(), res, o));
    v.setScaleType(ImageView.ScaleType.CENTER_CROP);
    v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(c, h)));
    v.setBackground(bg(c, Color.WHITE, 18));
    v.setClipToOutline(true);
    v.setImportantForAccessibility(2);
    return v;
  }

  static void space(LinearLayout parent, int h) {
    parent.addView(
        new View(parent.getContext()),
        new LinearLayout.LayoutParams(1, dp(parent.getContext(), h)));
  }

  static void weighted(LinearLayout row, View v) {
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
    lp.setMargins(dp(row.getContext(), 3), 0, dp(row.getContext(), 3), 0);
    row.addView(v, lp);
  }

  static String money(int cents) {
    return String.format(Locale.US, "$%,.2f", cents / 100.0);
  }
}
