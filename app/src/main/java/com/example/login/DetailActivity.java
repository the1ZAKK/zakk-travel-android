package com.example.login;

import android.app.*;
import android.os.Bundle;
import android.widget.*;
import java.time.*;

public final class DetailActivity extends Screen {
  private Catalog.Place place;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    if (!signedIn()) return;
    try {
      place = Catalog.get(getIntent().getStringExtra("item_id"));
    } catch (IllegalArgumentException ex) {
      message(ex.getMessage());
      finish();
      return;
    }
    LinearLayout root = page();
    Button back = Ui.button(this, "← Back to collection", false);
    root.addView(back);
    back.setOnClickListener(v -> finish());
    Ui.space(root, 18);
    root.addView(Ui.image(this, place.image, 260));
    Ui.space(root, 16);
    root.addView(
        Ui.text(this, place.location.toUpperCase(java.util.Locale.ROOT), 13, Ui.GREEN, true));
    root.addView(Ui.text(this, place.name, 34, Ui.INK, true));
    root.addView(
        Ui.text(
            this,
            String.format(java.util.Locale.US, "★ %.1f · Illustrative rating", place.rating),
            15,
            Ui.GREEN,
            true));
    root.addView(Ui.text(this, place.description, 17, Ui.MUTED, false));
    if (place.hotel) {
      root.addView(Ui.text(this, Ui.money(place.cents) + " / night", 26, Ui.INK, true));
      root.addView(
          Ui.text(
              this,
              "Sample USD room price for up to six guests. Taxes and fees are not calculated.",
              13,
              Ui.MUTED,
              false));
    }
    Button plan = Ui.button(this, place.hotel ? "Plan a demo stay" : "Plan a visit", true);
    plan.setId(300);
    root.addView(plan);
    plan.setOnClickListener(v -> booking());
    Button save = Ui.button(this, "", false);
    save.setId(301);
    Runnable label =
        () ->
            save.setText(
                store.favorites().contains(place.id)
                    ? "♥ Saved · tap to remove"
                    : "♡ Save this place");
    label.run();
    root.addView(save);
    save.setOnClickListener(
        v -> {
          store.toggle(place.id);
          label.run();
        });
    Ui.space(root, 16);
    root.addView(Ui.text(this, "A closer look", 22, Ui.INK, true));
    HorizontalScrollView gallery = new HorizontalScrollView(this);
    LinearLayout photos = Ui.row(this);
    int n = 0;
    for (int res : Catalog.gallery(place.id)) {
      ImageView im = Ui.image(this, res, 145);
      im.setImportantForAccessibility(1);
      im.setContentDescription(place.name + " illustrative photo " + (++n));
      LinearLayout.LayoutParams lp =
          new LinearLayout.LayoutParams(Ui.dp(this, 210), Ui.dp(this, 145));
      lp.setMarginEnd(Ui.dp(this, 10));
      photos.addView(im, lp);
    }
    gallery.addView(photos);
    root.addView(gallery);
    root.addView(
        Ui.text(
            this,
            "Plans stay on this device. Nothing is sent to a hotel or travel provider.",
            13,
            Ui.MUTED,
            false));
  }

  @Override
  public void onResume() {
    super.onResume();
    if (place != null) signedIn();
  }

  private void booking() {
    LinearLayout body = Ui.column(this);
    int p = Ui.dp(this, 22);
    body.setPadding(p, p, p, p);
    LocalDate[] date = {LocalDate.now().plusDays(1)};
    Button choose = Ui.button(this, "Arrival: " + date[0], false);
    body.addView(choose);
    choose.setOnClickListener(
        v -> {
          DatePickerDialog picker =
              new DatePickerDialog(
                  this,
                  (view, y, m, d) -> {
                    date[0] = LocalDate.of(y, m + 1, d);
                    choose.setText("Arrival: " + date[0]);
                  },
                  date[0].getYear(),
                  date[0].getMonthValue() - 1,
                  date[0].getDayOfMonth());
          picker
              .getDatePicker()
              .setMinDate(
                  LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
          picker
              .getDatePicker()
              .setMaxDate(
                  LocalDate.now()
                      .plusYears(2)
                      .atStartOfDay(ZoneId.systemDefault())
                      .toInstant()
                      .toEpochMilli());
          picker.show();
        });
    LinearLayout counters = Ui.row(this);
    body.addView(counters);
    LinearLayout a = Ui.column(this), b = Ui.column(this);
    Ui.weighted(counters, a);
    Ui.weighted(counters, b);
    a.addView(Ui.text(this, place.hotel ? "Nights" : "Visit days", 14, Ui.INK, true));
    b.addView(Ui.text(this, "Guests", 14, Ui.INK, true));
    NumberPicker nights = new NumberPicker(this), guests = new NumberPicker(this);
    nights.setMinValue(1);
    nights.setMaxValue(place.hotel ? 30 : 1);
    guests.setMinValue(1);
    guests.setMaxValue(6);
    nights.setWrapSelectorWheel(false);
    guests.setWrapSelectorWheel(false);
    nights.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
    guests.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
    nights.setContentDescription("Number of nights");
    guests.setContentDescription("Number of guests");
    a.addView(nights);
    b.addView(guests);
    TextView total = Ui.text(this, "", 20, Ui.GREEN, true);
    body.addView(total);
    Runnable update =
        () ->
            total.setText(
                place.hotel
                    ? "Estimated total: " + Ui.money(Rules.total(place.cents, nights.getValue()))
                    : "City visit · No charge");
    update.run();
    nights.setOnValueChangedListener((v, o, n) -> update.run());
    body.addView(
        Ui.text(
            this,
            "This saves a local plan. It does not reserve a room or charge you.",
            14,
            Ui.MUTED,
            false));
    TextView error = Ui.text(this, "", 14, 0xFF9C2C21, false);
    error.setAccessibilityLiveRegion(1);
    body.addView(error);
    ScrollView scroll = new ScrollView(this);
    scroll.addView(body);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(place.hotel ? "Plan your stay" : "Plan your visit")
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save plan", null)
            .create();
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      try {
                        store.book(
                            place.id, date[0].toString(), nights.getValue(), guests.getValue());
                        message("Plan saved. Find it in Trips.");
                        dialog.dismiss();
                      } catch (IllegalArgumentException ex) {
                        error.setText(ex.getMessage());
                      }
                    }));
    dialog.show();
  }
}
