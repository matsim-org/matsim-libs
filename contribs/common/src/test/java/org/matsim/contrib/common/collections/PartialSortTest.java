/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.contrib.common.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.common.collections.PartialSort.kSmallestElements;

import java.util.Comparator;

import org.junit.jupiter.api.Test;
import java.util.stream.Stream;

/**
 * @author Michal Maciejewski (michalm)
 */
public class PartialSortTest {
	private final Comparator<Integer> comparator = Integer::compareTo;

	@Test
	void k0_noneSelected() {
		assertThat(kSmallestElements(0, Stream.of(), comparator)).isEmpty();
		assertThat(kSmallestElements(0, Stream.of(7, 1, 4, 9, 8), comparator)).isEmpty();
	}

	@Test
	void reversedComparator_largestElementsSelected() {
		assertThat(kSmallestElements(1, Stream.of(7, 1, 4, 9, 8), comparator.reversed())).containsExactly(9);
		assertThat(kSmallestElements(3, Stream.of(7, 1, 4, 9, 8), comparator.reversed())).containsExactly(9, 8, 7);
	}

	@Test
	void allElementsPairwiseNonEqual_inputOrderNotImportant() {
		assertThat(kSmallestElements(3, Stream.of(1, 7, 9, 8), comparator)).containsExactly(1, 7, 8);
		assertThat(kSmallestElements(3, Stream.of(9, 8, 7, 1), comparator)).containsExactly(1, 7, 8);
	}

	@Test
	void exactlyKElementsProvided() {
		assertThat(kSmallestElements(3, Stream.of(7, 1, 4), comparator)).containsExactly(1, 4, 7);
	}

	@Test
	void moreThenKElementsProvided() {
		assertThat(kSmallestElements(3, Stream.of(7, 1, 4, 9, 8), comparator)).containsExactly(1, 4, 7);
		assertThat(kSmallestElements(3, Stream.of(13, 7, 1, 55, 4, 9, 8, 11), comparator)).containsExactly(1, 4, 7);
	}

	@Test
	void lessThenKElementsProvided() {
		assertThat(kSmallestElements(3, Stream.of(7, 1), comparator)).containsExactly(1, 7);
		assertThat(kSmallestElements(3, Stream.of(13), comparator)).containsExactly(13);
		assertThat(kSmallestElements(3, Stream.of(), comparator)).isEmpty();
	}
}
