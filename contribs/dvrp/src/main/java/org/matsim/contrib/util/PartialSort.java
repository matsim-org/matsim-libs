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

import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

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
	public static <T> List<T> kSmallestElements(int k, Stream<T> elements, ToDoubleFunction<T> evaluator) {
		PartialSort<T> nearestRequestSort = new PartialSort<T>(k);
		nearestRequestSort.addAll(elements, evaluator::applyAsDouble);
		return nearestRequestSort.kSmallestElements();
	}

	private static class ElementValuePair<T> implements Comparable<ElementValuePair<T>> {
		private final T element;
		private final double value;

		public ElementValuePair(T element, double value) {
			this.element = element;
			this.value = value;
		}

		@Override
		public int compareTo(ElementValuePair<T> o) {
			return -Double.compare(value, o.value);// reversed comparison (the smallest is the last in the queue)
		}
	}

	private final int k;
	private final PriorityQueue<ElementValuePair<T>> kSmallestElements;// descending order: from k-th to 1-st

	public PartialSort(int k) {
		this.k = k;
		kSmallestElements = new PriorityQueue<>(k);
	}

	public void add(T element, double value) {
		if (kSmallestElements.size() < k) {
			kSmallestElements.add(new ElementValuePair<>(element, value));
		} else if (Double.compare(value, kSmallestElements.peek().value) < 0) {
			kSmallestElements.poll();
			kSmallestElements.add(new ElementValuePair<>(element, value));
		}
	}

	public void addAll(Stream<T> elements, ToDoubleFunction<T> evaluator) {
		elements.forEach(e -> this.add(e, evaluator.applyAsDouble(e)));
	}

	/**
	 * Gets k smallest elements (side effect: they are removed from the queue -- the queue gets empty).
	 * 
	 * @return list containing k smallest elements sorted ascending: from the smallest to the k-th smallest
	 */
	public List<T> kSmallestElements() {
		@SuppressWarnings("unchecked")
		T[] array = (T[])new Object[kSmallestElements.size()];
		for (int i = array.length - 1; i >= 0; i--) {
			array[i] = kSmallestElements.poll().element;
		}
		return Arrays.asList(array);
	}
}
