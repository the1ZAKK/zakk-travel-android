package com.example.login;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import java.time.LocalDate;
import java.util.*;

public final class HomeActivity extends Screen {
  private int tab, kind;
  private String query = "";
  private RecyclerView grid;
  private Cards adapter;
  private EditText search;
  private TextView title, note, empty;
  private LinearLayout filters, document;
  private ScrollView scroll;
  private boolean compact;
  private final Button[] nav = new Button[4], chips = new Button[3];

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    if (!signedIn()) return;
    if (state != null) {
      tab = state.getInt("tab");
      kind = state.getInt("kind");
      query = state.getString("query", "");
    }
    compact = getResources().getConfiguration().screenHeightDp < 500;
    LinearLayout root = Ui.column(this);
    int p = Ui.dp(this, compact ? 10 : 18);
    root.setPadding(p, p, p, p);
    root.setBackgroundColor(Ui.BG);
    setContentView(root);
    root.addView(Ui.text(this, "ZAKK / TRAVEL", 12, Ui.GREEN, true));
    title = Ui.text(this, "", compact ? 20 : 29, Ui.INK, true);
    root.addView(title);
    note = Ui.text(this, "", 13, Ui.MUTED, false);
    root.addView(note);
    if (compact) note.setVisibility(View.GONE);
    search = Ui.field(this, "Search cities, hotels or countries", 1, 200);
    search.setText(query);
    root.addView(search);
    filters = Ui.row(this);
    root.addView(filters);
    String[] labels = {"All", "Cities", "Hotels"};
    for (int i = 0; i < 3; i++) {
      final int n = i;
      chips[i] = Ui.button(this, labels[i], false);
      Ui.weighted(filters, chips[i]);
      chips[i].setOnClickListener(
          v -> {
            kind = n;
            refresh();
          });
    }
    empty = Ui.text(this, "", 15, Ui.MUTED, false);
    root.addView(empty);
    grid = new RecyclerView(this);
    grid.setId(201);
    int columns = getResources().getConfiguration().screenWidthDp >= 600 ? 3 : 2;
    if (getResources().getConfiguration().fontScale > 1.3
        || getResources().getConfiguration().screenWidthDp < 360) columns = 1;
    grid.setLayoutManager(new GridLayoutManager(this, columns));
    adapter = new Cards();
    grid.setAdapter(adapter);
    root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
    scroll = new ScrollView(this);
    document = Ui.column(this);
    scroll.addView(document);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout bottom = Ui.row(this);
    root.addView(bottom);
    String[] tabs = {"Explore", "Saved", "Trips", "Account"};
    for (int i = 0; i < 4; i++) {
      final int n = i;
      nav[i] = Ui.button(this, tabs[i], false);
      nav[i].setTextSize(12);
      Ui.weighted(bottom, nav[i]);
      nav[i].setOnClickListener(
          v -> {
            tab = n;
            refresh();
          });
    }
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int a, int c, int f) {}

          public void afterTextChanged(Editable e) {}

          public void onTextChanged(CharSequence s, int a, int b, int c) {
            query = s.toString();
            refresh();
          }
        });
    refresh();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (adapter != null && signedIn()) refresh();
  }

  @Override
  public void onSaveInstanceState(Bundle out) {
    out.putInt("tab", tab);
    out.putInt("kind", kind);
    out.putString("query", query);
    super.onSaveInstanceState(out);
  }

  private void selected(Button b, boolean yes) {
    b.setBackground(Ui.bg(this, yes ? Ui.GREEN : 0xFFE2EDE6, 14));
    b.setTextColor(yes ? 0xFFFFFFFF : Ui.GREEN);
    b.setSelected(yes);
  }

  private void refresh() {
    if (!signedIn()) return;
    boolean browsing = tab < 2;
    grid.setVisibility(browsing ? View.VISIBLE : View.GONE);
    scroll.setVisibility(browsing ? View.GONE : View.VISIBLE);
    search.setVisibility(browsing ? View.VISIBLE : View.GONE);
    filters.setVisibility(browsing ? View.VISIBLE : View.GONE);
    empty.setVisibility(View.GONE);
    for (int i = 0; i < 4; i++) selected(nav[i], i == tab);
    for (int i = 0; i < 3; i++) selected(chips[i], i == kind);
    String[] titles = {
      "Find your next\nfavourite place.",
      "Keep the places\nthat speak to you.",
      "Your next adventures.",
      "Hello, " + store.name() + "."
    };
    title.setText(compact ? titles[tab].replace('\n', ' ') : titles[tab]);
    if (browsing) {
      List<Catalog.Place> places = new ArrayList<>(Catalog.search(query, kind));
      if (tab == 1) {
        Set<String> saved = store.favorites();
        places.removeIf(p -> !saved.contains(p.id));
      }
      adapter.replace(places);
      note.setText(places.size() + " places · Offline collection · Sample ratings and prices");
      if (places.isEmpty()) {
        empty.setText("No places match. Try a different search or save a place from Explore.");
        empty.setVisibility(View.VISIBLE);
      }
    } else if (tab == 2) trips();
    else account();
  }

  private void trips() {
    note.setText("Plans saved on this device. Demo reservations only.");
    document.removeAllViews();
    List<TravelStore.Booking> list = store.bookings();
    if (list.isEmpty())
      document.addView(
          Ui.text(
              this,
              "Your itinerary is waiting. Explore a place and plan your first visit.",
              18,
              Ui.MUTED,
              false));
    for (TravelStore.Booking b : list) {
      LinearLayout card = Ui.column(this);
      int p = Ui.dp(this, 16);
      card.setPadding(p, p, p, p);
      card.setBackground(Ui.bg(this, 0xFFFFFFFF, 18));
      card.addView(Ui.text(this, b.place.name, 21, Ui.INK, true));
      card.addView(
          Ui.text(
              this,
              b.date
                  + (b.place.hotel ? " → " + LocalDate.parse(b.date).plusDays(b.nights) : "")
                  + " · "
                  + b.guests
                  + " guest(s)",
              14,
              Ui.MUTED,
              false));
      card.addView(
          Ui.text(
              this,
              b.place.hotel
                  ? b.nights + " night(s) · " + Ui.money(b.total) + " estimated"
                  : "City visit · No charge",
              15,
              Ui.GREEN,
              true));
      card.addView(
          Ui.text(
              this,
              b.place.hotel ? "DEMO · Not a confirmed hotel reservation" : "Local itinerary entry",
              12,
              Ui.MUTED,
              false));
      Button remove = Ui.button(this, "Remove plan", false);
      card.addView(remove);
      remove.setOnClickListener(
          v ->
              new AlertDialog.Builder(this)
                  .setTitle("Remove this plan?")
                  .setMessage(b.place.name + " · " + b.date)
                  .setNegativeButton("Keep", null)
                  .setPositiveButton(
                      "Remove",
                      (d, w) -> {
                        store.cancel(b.id);
                        refresh();
                      })
                  .show());
      document.addView(card);
      Ui.space(document, 14);
    }
  }

  private void account() {
    note.setText("Your travel corner, on your device.");
    document.removeAllViews();
    document.addView(Ui.text(this, "Private by design", 22, Ui.INK, true));
    document.addView(
        Ui.text(
            this,
            "Accounts, saved places and plans stay in this installation. No tracking, cloud sync or"
                + " payments are connected. Prices and ratings are sample data.",
            16,
            Ui.MUTED,
            false));
    document.addView(
        Ui.text(
            this,
            "There is no password recovery service. Keep your password safe. Uninstalling the app"
                + " removes local data.",
            15,
            Ui.MUTED,
            false));
    Button out = Ui.button(this, "Sign out", true);
    document.addView(out);
    out.setOnClickListener(
        v -> {
          store.logout();
          login();
        });
    Button delete = Ui.button(this, "Delete local account", false);
    document.addView(delete);
    delete.setOnClickListener(
        v ->
            new AlertDialog.Builder(this)
                .setTitle("Delete your local account?")
                .setMessage(
                    "Your account, saved places and plans will be removed from this device.")
                .setNegativeButton("Keep account", null)
                .setPositiveButton(
                    "Delete account",
                    (d, w) -> {
                      store.deleteAccount();
                      login();
                    })
                .show());
  }

  private final class Cards extends RecyclerView.Adapter<Cards.Holder> {
    private final List<Catalog.Place> items = new ArrayList<>();
    private Set<String> saved = new HashSet<>();

    Cards() {
      setHasStableIds(true);
    }

    void replace(List<Catalog.Place> list) {
      items.clear();
      items.addAll(list);
      saved = store.favorites();
      notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
      return items.size();
    }

    @Override
    public long getItemId(int position) {
      return items.get(position).id.hashCode();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int type) {
      LinearLayout card = Ui.column(HomeActivity.this);
      RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
      lp.setMargins(
          Ui.dp(HomeActivity.this, 6),
          Ui.dp(HomeActivity.this, 6),
          Ui.dp(HomeActivity.this, 6),
          Ui.dp(HomeActivity.this, 8));
      card.setLayoutParams(lp);
      card.setBackground(Ui.bg(HomeActivity.this, 0xFFFFFFFF, 20));
      card.setClipToOutline(true);
      return new Holder(card);
    }

    @Override
    public void onBindViewHolder(Holder h, int position) {
      Catalog.Place p = items.get(position);
      h.card.removeAllViews();
      h.card.addView(Ui.image(HomeActivity.this, p.image, 150));
      LinearLayout content = Ui.column(HomeActivity.this);
      int pad = Ui.dp(HomeActivity.this, 12);
      content.setPadding(pad, pad, pad, pad);
      h.card.addView(content);
      content.addView(Ui.text(HomeActivity.this, p.location, 12, Ui.MUTED, false));
      content.addView(Ui.text(HomeActivity.this, p.name, 20, Ui.INK, true));
      content.addView(
          Ui.text(
              HomeActivity.this,
              String.format(Locale.US, "★ %.1f · Sample rating", p.rating),
              12,
              Ui.GREEN,
              false));
      content.addView(
          Ui.text(
              HomeActivity.this,
              p.hotel ? Ui.money(p.cents) + " / night" : "City guide",
              14,
              Ui.INK,
              true));
      Button open = Ui.button(HomeActivity.this, "Explore →", true);
      open.setContentDescription("Explore " + p.name);
      content.addView(open);
      open.setOnClickListener(
          v ->
              startActivity(
                  new Intent(HomeActivity.this, DetailActivity.class).putExtra("item_id", p.id)));
      Button save =
          Ui.button(HomeActivity.this, saved.contains(p.id) ? "♥ Saved" : "♡ Save", false);
      save.setContentDescription((saved.contains(p.id) ? "Remove saved " : "Save ") + p.name);
      content.addView(save);
      save.setOnClickListener(
          v -> {
            store.toggle(p.id);
            refresh();
          });
    }

    final class Holder extends RecyclerView.ViewHolder {
      final LinearLayout card;

      Holder(LinearLayout v) {
        super(v);
        card = v;
      }
    }
  }
}
