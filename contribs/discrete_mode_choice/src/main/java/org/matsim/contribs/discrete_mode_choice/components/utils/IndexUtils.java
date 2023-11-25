package org.matsim.contribs.discrete_mode_choice.components.utils;

import java.util.Collection;
import java.util.List;

public class IndexUtils {
  private IndexUtils() {}

  public static int getTripIndex(List<String> previousModes) {
    return previousModes.size();
  }

  public static int getTourIndex(List<List<String>> previousModes) {
    return previousModes.size();
  }

  public static int getFirstTripIndex(List<List<String>> previousModes) {
    return previousModes.stream().mapToInt(Collection::size).sum();
  }
}
