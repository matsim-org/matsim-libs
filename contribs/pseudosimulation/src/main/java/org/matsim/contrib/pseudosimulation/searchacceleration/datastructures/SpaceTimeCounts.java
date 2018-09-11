/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration.datastructures;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import floetteroed.utilities.Tuple;

/**
 * Stores integer (counting) data in a map with keys consisting of (space, time)
 * tuples. "Space" is represented by the generic class L (e.g. a network link).
 * 
 * This minimal class exists only to speed up numerical operations in
 * {@link ScoreUpdater} that require iterating over all map entries. For a less
 * memory-intensive implementation, see {@link SpaceTimeIndicators}.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param L
 *            the space coordinate type
 *
 */
class SpaceTimeCounts<L> {

	// -------------------- MEMBERS --------------------

	// all values are non-null
	private final Map<Tuple<L, Integer>, Double> data = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	SpaceTimeCounts(final SpaceTimeIndicators<L> parent, final Map<?, Double> weights) {
		if (parent != null) {
			for (int timeBin = 0; timeBin < parent.getTimeBinCnt(); timeBin++) {
				for (L spaceObj : parent.getVisitedSpaceObjects(timeBin)) {
					this.add(this.newKey(spaceObj, timeBin), weights.get(spaceObj));
				}
			}
		}
	}

	// -------------------- INTERNALS --------------------

	private Tuple<L, Integer> newKey(final L spaceObj, final Integer timeBin) {
		return new Tuple<>(spaceObj, timeBin);
	}

	private Double get(final Tuple<L, Integer> key) {
		if (this.data.containsKey(key)) {
			return this.data.get(key);
		} else {
			return 0.0;
		}
	}

	private void set(final Tuple<L, Integer> key, final Double value) {
		if ((value == null) || (value.doubleValue() == 0)) {
			this.data.remove(key);
		} else {
			this.data.put(key, value);
		}
	}

	private void add(final Tuple<L, Integer> key, final Double addend) {
		if (addend != null) {
			this.set(key, this.get(key) + addend);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	Set<Map.Entry<Tuple<L, Integer>, Double>> entriesView() {
		return Collections.unmodifiableSet(this.data.entrySet());
	}

	void subtract(final SpaceTimeCounts<L> other) {
		for (Map.Entry<Tuple<L, Integer>, Double> otherEntry : other.data.entrySet()) {
			this.set(otherEntry.getKey(), this.get(otherEntry.getKey()) - otherEntry.getValue());
		}
	}
}
