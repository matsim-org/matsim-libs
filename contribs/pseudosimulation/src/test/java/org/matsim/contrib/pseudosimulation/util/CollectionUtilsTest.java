package org.matsim.contrib.pseudosimulation.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CollectionUtilsTest {

	@Test
	void splitsArraysEvenlyAndPlacesRemainderInLastSegment() {
		List<Integer[]> result = CollectionUtils.split(new Integer[]{0, 1, 2, 3, 4}, 2);

		assertEquals(2, result.size());
		assertArrayEquals(new Integer[]{0, 1}, result.get(0));
		assertArrayEquals(new Integer[]{2, 3, 4}, result.get(1));
	}

	@Test
	void arraySegmentsRetainRuntimeComponentType() {
		List<String[]> result = CollectionUtils.split(new String[]{"a", "b"}, 2);

		assertEquals(String[].class, result.get(0).getClass());
		assertArrayEquals(new String[]{"a"}, result.get(0));
	}

	@Test
	void arraySplitReadsFirstElementBeforeValidatingRequestedCount() {
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> CollectionUtils.split(new String[0], 1));
	}

	@Test
	void rejectsMoreArraySegmentsThanElements() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> CollectionUtils.split(new Integer[]{1}, 2));

		assertEquals("n must not be smaller set size!", error.getMessage());
	}

	@Test
	void splitsCollectionsInEncounterOrder() {
		List<Integer>[] result = CollectionUtils.split(List.of(1, 2, 3, 4, 5), 3);

		assertEquals(List.of(1), result[0]);
		assertEquals(List.of(2), result[1]);
		assertEquals(List.of(3, 4, 5), result[2]);
	}

	@Test
	void weightedSplitNormalizesWeightsAndAssignsRoundingRemainderToFirstSegment() {
		List<Integer>[] result = CollectionUtils.split(List.of(1, 2, 3, 4, 5), new double[]{1, 1});

		assertEquals(List.of(1, 2, 3), result[0]);
		assertEquals(List.of(4, 5), result[1]);
	}

	@Test
	void integerWeightsDelegateToWeightedSplit() {
		List<Integer>[] result = CollectionUtils.split(List.of(1, 2, 3, 4), new int[]{1, 3});

		assertEquals(List.of(1), result[0]);
		assertEquals(List.of(2, 3, 4), result[1]);
	}

	@Test
	void rejectsMoreWeightsThanElementsWithLegacyMessage() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> CollectionUtils.split(List.of(1), new double[]{1, 1}));

		assertEquals("weigths.length must not be smaller than set size!", error.getMessage());
	}

	@Test
	void sumsValuesAndTreatsEmptyCollectionAsZero() {
		assertEquals(2.5, CollectionUtils.sumElements(List.of(1.0, -2.0, 3.5)));
		assertEquals(0.0, CollectionUtils.sumElements(List.of()));
	}
}
