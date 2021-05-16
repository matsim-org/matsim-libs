/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.util;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * Sorts from smallest to largest. If the opposite should be the case then add elements with their values negated:
 * {@code PartialSort.add(element, -value)}. Works fine for small k (k << n); otherwise, one should consider a partial
 * version of heapsort or quicksort.
 * <p>
 * More info: <a href="http://en.wikipedia.org/wiki/Partial_sorting">Partial sorting</a>
 *
 * @param <T>
 */
public class PartialSort<T> {
	public static <T> List<T> kSmallestElements(int k, Stream<T> elements, Comparator<T> comparator) {
		PartialSort<T> nearestRequestSort = new PartialSort<>(k, comparator);
		elements.forEach(nearestRequestSort::add);
		return nearestRequestSort.kSmallestElements();
	}

	private final int k;
	private final Comparator<T> comparator;
	@Nullable
	private final PriorityQueue<T> kSmallestElements;// descending order: from k-th to 1-st

	public PartialSort(int k, Comparator<T> comparator) {
		Preconditions.checkArgument(k >= 0, "k must not be negative.");
		this.k = k;
		this.comparator = comparator;
		kSmallestElements = k == 0 ? null : new PriorityQueue<>(k, comparator.reversed());
	}

	public void add(T element) {
		if (kSmallestElements != null) {
			if (kSmallestElements.size() < k) {
				kSmallestElements.add(element);
			} else if (comparator.compare(element, kSmallestElements.peek()) < 0) {
				kSmallestElements.poll();
				kSmallestElements.add(element);
			}
		}
	}

	/**
	 * Gets k smallest elements.
	 * <p>
	 * SIDE EFFECT: elements are removed from the queue.
	 *
	 * @return list containing k smallest elements sorted ascending: from the smallest to the k-th smallest
	 */
	public List<T> kSmallestElements() {
		if (kSmallestElements == null) {
			return List.of();
		}

		@SuppressWarnings("unchecked")
		T[] array = (T[])new Object[kSmallestElements.size()];
		for (int i = array.length - 1; i >= 0; i--) {
			array[i] = kSmallestElements.poll();
		}
		return List.of(array);
	}
}
