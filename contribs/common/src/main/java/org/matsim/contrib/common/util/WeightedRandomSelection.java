/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package org.matsim.contrib.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

import com.google.common.base.Preconditions;

public class WeightedRandomSelection<T> {
	private final List<Entry<T>> entryList = new ArrayList<>();
	private double totalWeight = 0;
	private final RandomGenerator random;

	public WeightedRandomSelection(RandomGenerator random) {
		this.random = random;
	}

	public void add(T obj, double weight) {
		if (weight < 0 || !Double.isFinite(weight)) {
			throw new IllegalArgumentException("Weight must be non-negative and finite");
		}

		totalWeight += weight;
		if (Double.isInfinite(totalWeight)) {
			throw new ArithmeticException("Total weight is infinite");
		}

		entryList.add(new Entry<>(obj, totalWeight));
	}

	public T select() {
		Preconditions.checkState(!entryList.isEmpty(), "No entries in the list to select from");
		Preconditions.checkState(totalWeight > 0, "Total weight is not positive");

		double rnd = random.nextDouble(0, totalWeight);
		int idx = Collections.binarySearch(entryList, new Entry<>(null, rnd));

		if (idx < 0) {
			idx = -idx - 1;
		}

		return entryList.get(idx).e;
	}

	public int size() {
		return entryList.size();
	}

	public double getTotalWeight() {
		return totalWeight;
	}

	private record Entry<E>(E e, double cumulativeWeight) implements Comparable<Entry<E>> {
		public int compareTo(Entry<E> o) {
			double diff = this.cumulativeWeight - o.cumulativeWeight;
			return diff == 0 ? 0 : (diff > 0 ? 1 : -1);
		}
	}
}
