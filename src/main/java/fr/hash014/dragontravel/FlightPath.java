package fr.hash014.dragontravel;

import java.util.*;

/** Server-independent, bounded, distance-parametrized flight geometry. */
public final class FlightPath {
  public static final class Point {
    public final double x, y, z;

    public Point(double x, double y, double z) {
      if (!finite(x) || !finite(y) || !finite(z))
        throw new IllegalArgumentException("Non-finite coordinates");
      this.x = x;
      this.y = y;
      this.z = z;
    }

    public double distance(Point b) {
      return Math.sqrt((x - b.x) * (x - b.x) + (y - b.y) * (y - b.y) + (z - b.z) * (z - b.z));
    }
  }

  private final List<Point> points;
  private final double[] lengths;
  private final double length;

  public FlightPath(List<Point> input) {
    if (input.size() < 2 || input.size() > 260)
      throw new IllegalArgumentException("Invalid path size");
    points = Collections.unmodifiableList(new ArrayList<Point>(input));
    lengths = new double[input.size() - 1];
    double total = 0;
    for (int i = 0; i < lengths.length; i++) {
      lengths[i] = input.get(i).distance(input.get(i + 1));
      total += lengths[i];
    }
    if (!finite(total)) throw new IllegalArgumentException("Path overflow");
    length = total;
  }

  public double length() {
    return length;
  }

  public Point at(double distance) {
    if (!finite(distance)) throw new IllegalArgumentException("Invalid distance");
    double remaining = Math.max(0, Math.min(length, distance));
    for (int i = 0; i < lengths.length; i++) {
      if (lengths[i] > 0 && remaining < lengths[i]) {
        double t = remaining / lengths[i];
        Point a = points.get(i), b = points.get(i + 1);
        double yT = (a.x == b.x && a.z == b.z) ? t * t * (3 - 2 * t) : t;
        return new Point(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * yT, a.z + (b.z - a.z) * t);
      }
      remaining -= lengths[i];
    }
    return points.get(points.size() - 1);
  }

  public static FlightPath cruise(Point from, List<Point> stops, double altitude) {
    if (stops.isEmpty() || stops.size() > 64 || !finite(altitude))
      throw new IllegalArgumentException("Invalid stops/altitude");
    List<Point> points = new ArrayList<Point>();
    points.add(from);
    double height = Math.max(from.y, altitude);
    for (Point p : stops) height = Math.max(height, p.y);
    points.add(new Point(from.x, height, from.z));
    for (int i = 0; i < stops.size(); i++) {
      Point p = stops.get(i);
      points.add(new Point(p.x, i == stops.size() - 1 ? height : p.y, p.z));
    }
    points.add(stops.get(stops.size() - 1));
    return new FlightPath(points);
  }

  public static boolean finite(double v) {
    return !Double.isNaN(v) && !Double.isInfinite(v);
  }

  public static String id(String value) {
    if (value == null || !value.matches("[A-Za-z0-9_-]{1,32}"))
      throw new IllegalArgumentException("Nom : 1-32 lettres, chiffres, _ ou -");
    return value.toLowerCase(Locale.ROOT);
  }
}
