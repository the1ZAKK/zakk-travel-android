package com.example.login;

import java.util.*;

public final class Catalog {
  public static final class Place {
    public final String id, name, location, description;
    public final boolean hotel;
    public final int image, cents;
    public final double rating;

    Place(
        String id,
        String name,
        String location,
        String description,
        boolean hotel,
        int image,
        int cents,
        double rating) {
      this.id = id;
      this.name = name;
      this.location = location;
      this.description = description;
      this.hotel = hotel;
      this.image = image;
      this.cents = cents;
      this.rating = rating;
    }
  }

  private static final List<Place> ITEMS =
      Collections.unmodifiableList(
          Arrays.asList(
              new Place(
                  "paris",
                  "Paris",
                  "France",
                  "Explore the Seine, neighbourhood cafes, the Louvre and the streets of"
                      + " Montmartre.",
                  false,
                  R.drawable.paris,
                  0,
                  4.8),
              new Place(
                  "newyork",
                  "New York",
                  "United States",
                  "Find neighbourhood galleries, Central Park and waterfront views of the skyline.",
                  false,
                  R.drawable.newyork,
                  0,
                  4.7),
              new Place(
                  "tokyo",
                  "Tokyo",
                  "Japan",
                  "Discover quiet temple gardens, neon streets, local food and design.",
                  false,
                  R.drawable.tokyo,
                  0,
                  4.9),
              new Place(
                  "rome",
                  "Rome",
                  "Italy",
                  "Walk between ancient landmarks, lively piazzas and local trattorias.",
                  false,
                  R.drawable.rome,
                  0,
                  4.8),
              new Place(
                  "london",
                  "London",
                  "United Kingdom",
                  "Explore riverside walks, museums, green parks and independent markets.",
                  false,
                  R.drawable.london,
                  0,
                  4.6),
              new Place(
                  "hotel-paris",
                  "Hotel Paris",
                  "Paris, France",
                  "A sample city stay. Pictures, prices and ratings are illustrative, with no live"
                      + " availability.",
                  true,
                  R.drawable.hotel_paris,
                  15000,
                  4.7),
              new Place(
                  "hotel-newyork",
                  "Hotel New York",
                  "New York, United States",
                  "A sample city stay. Pictures, prices and ratings are illustrative, with no live"
                      + " availability.",
                  true,
                  R.drawable.hotel_newyork,
                  20000,
                  4.6),
              new Place(
                  "hotel-tokyo",
                  "Hotel Tokyo",
                  "Tokyo, Japan",
                  "A sample city stay. Pictures, prices and ratings are illustrative, with no live"
                      + " availability.",
                  true,
                  R.drawable.hotel_tokyo,
                  18000,
                  4.8),
              new Place(
                  "hotel-rome",
                  "Hotel Rome",
                  "Rome, Italy",
                  "A sample city stay. Pictures, prices and ratings are illustrative, with no live"
                      + " availability.",
                  true,
                  R.drawable.hotel_rome,
                  16000,
                  4.7),
              new Place(
                  "hotel-london",
                  "Hotel London",
                  "London, United Kingdom",
                  "A sample city stay. Pictures, prices and ratings are illustrative, with no live"
                      + " availability.",
                  true,
                  R.drawable.hotel_london,
                  21000,
                  4.6)));

  public static List<Place> all() {
    return ITEMS;
  }

  public static Place get(String id) {
    for (Place p : ITEMS) if (p.id.equals(id)) return p;
    throw new IllegalArgumentException("Place unavailable.");
  }

  public static List<Place> search(String query, int kind) {
    List<Place> out = new ArrayList<>();
    String q = query.trim().toLowerCase(Locale.ROOT);
    for (Place p : ITEMS) {
      if (kind == 1 && p.hotel || kind == 2 && !p.hotel) continue;
      if ((p.name + " " + p.location).toLowerCase(Locale.ROOT).contains(q)) out.add(p);
    }
    return out;
  }

  public static int[] gallery(String id) {
    switch (id) {
      case "paris":
        return new int[] {R.drawable.paris1, R.drawable.paris2, R.drawable.paris3};
      case "newyork":
        return new int[] {R.drawable.newyork1, R.drawable.newyork2, R.drawable.newyork3};
      case "tokyo":
        return new int[] {R.drawable.tokyo1, R.drawable.tokyo2, R.drawable.tokyo3};
      case "rome":
        return new int[] {R.drawable.rome1, R.drawable.rome2, R.drawable.rome3};
      case "london":
        return new int[] {R.drawable.london1, R.drawable.london2, R.drawable.london3};
      case "hotel-paris":
        return new int[] {R.drawable.parishotel1, R.drawable.parishotel2, R.drawable.parishotel3};
      case "hotel-newyork":
        return new int[] {
          R.drawable.newyorkhotel1, R.drawable.newyorkhotel2, R.drawable.newyorkhotel3
        };
      case "hotel-tokyo":
        return new int[] {R.drawable.tokyohotel1, R.drawable.tokyohotel2, R.drawable.tokyohotel3};
      case "hotel-rome":
        return new int[] {R.drawable.romehotel1, R.drawable.romehotel2, R.drawable.romehotel3};
      case "hotel-london":
        return new int[] {
          R.drawable.londonhotel1, R.drawable.londonhotel2, R.drawable.londonhotel3
        };
      default:
        throw new IllegalArgumentException("Place unavailable.");
    }
  }
}
