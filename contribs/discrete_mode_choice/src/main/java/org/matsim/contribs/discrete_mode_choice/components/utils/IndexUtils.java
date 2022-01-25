package org.matsim.contribs.discrete_mode_choice.components.utils;

import java.util.Collection;
import java.util.List;

public class IndexUtils {
	private IndexUtils() {
	}

	static public int getTripIndex(List<String> previousModes) {
		return previousModes.size();
	}

	static public int getTourIndex(List<List<String>> previousModes) {
		return previousModes.size();
	}

	static public int getFirstTripIndex(List<List<String>> previousModes) {
		return previousModes.stream().mapToInt(Collection::size).sum();
	}
}
