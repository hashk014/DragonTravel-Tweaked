package fr.hash014.dragontravel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class FlightPathTest {
  FlightPath.Point p(double x, double y, double z) {
    return new FlightPath.Point(x, y, z);
  }

  @Test
  void exactArrivalAndBounds() {
    FlightPath path = new FlightPath(Arrays.asList(p(0, 0, 0), p(3, 4, 0)));
    assertEquals(5, path.length());
    assertEquals(0, path.at(-1).distance(p(0, 0, 0)));
    assertEquals(0, path.at(100).distance(p(3, 4, 0)));
  }

  @Test
  void noOvershootAcrossRepeatedStops() {
    FlightPath path =
        new FlightPath(Arrays.asList(p(0, 0, 0), p(0, 0, 0), p(10, 0, 0), p(10, 0, 0)));
    for (double d = 0; d < 20; d += .13) {
      assertTrue(path.at(d).x >= 0);
      assertTrue(path.at(d).x <= 10);
    }
  }

  @Test
  void smoothVerticalCruise() {
    FlightPath path = FlightPath.cruise(p(0, 80, 0), Arrays.asList(p(10, 80, 0)), 100);
    assertEquals(50, path.length());
    assertEquals(90, path.at(10).y, 1e-8);
    assertEquals(0, path.at(50).distance(p(10, 80, 0)));
  }

  @Test
  void nanInfinityRejected() {
    assertThrows(IllegalArgumentException.class, () -> p(Double.NaN, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> p(0, Double.POSITIVE_INFINITY, 0));
    FlightPath path = new FlightPath(Arrays.asList(p(0, 0, 0), p(1, 0, 0)));
    assertThrows(IllegalArgumentException.class, () -> path.at(Double.NaN));
  }

  @Test
  void emptyAndUnboundedRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new FlightPath(Collections.singletonList(p(0, 0, 0))));
    assertThrows(
        IllegalArgumentException.class,
        () -> FlightPath.cruise(p(0, 0, 0), Collections.<FlightPath.Point>emptyList(), 80));
    assertThrows(
        IllegalArgumentException.class,
        () -> FlightPath.cruise(p(0, 0, 0), Collections.nCopies(65, p(1, 1, 1)), 80));
  }

  @Test
  void zeroLengthCompletes() {
    FlightPath path = new FlightPath(Arrays.asList(p(2, 3, 4), p(2, 3, 4)));
    assertEquals(0, path.length());
    assertEquals(0, path.at(0).distance(p(2, 3, 4)));
  }

  @Test
  void safeIdentifier() {
    assertEquals("my_station", FlightPath.id("My_Station"));
    for (String bad : Arrays.asList("a.b", "../file", "", "x y", "é", "a/b"))
      assertThrows(IllegalArgumentException.class, () -> FlightPath.id(bad));
  }

  @Test
  void diagonalDistanceAndMonotonicTravel() {
    FlightPath path = new FlightPath(Arrays.asList(p(-10, 90, -10), p(10, 90, 10)));
    double last = -100;
    for (double d = 0; d < path.length(); d += .1) {
      assertTrue(path.at(d).x >= last);
      last = path.at(d).x;
    }
    assertEquals(Math.sqrt(800), path.length(), 1e-8);
  }
}
