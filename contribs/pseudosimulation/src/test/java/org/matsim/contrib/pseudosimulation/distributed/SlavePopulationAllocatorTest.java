package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SlavePopulationAllocatorTest {

	private double originalPlanAllocationLimiter;

	@BeforeEach
	void rememberGlobalLimiter() {
		originalPlanAllocationLimiter = MasterControler.planAllocationLimiter;
		MasterControler.planAllocationLimiter = 10.0;
	}

	@AfterEach
	void restoreGlobalLimiter() {
		MasterControler.planAllocationLimiter = originalPlanAllocationLimiter;
	}

	@Test
	void dividesPopulationEvenlyForEquallyFastSlavesAndMutatesMemoryLimits() {
		long[] maxMemory = {1_000_000_000, 1_000_000_000};

		int[] allocation = MasterControler.getSlaveTargetPopulationSizes(
				new double[]{100, 100}, new int[]{10, 10}, maxMemory, new long[]{1_000, 1_000},
				1_000, 100, 0, 100);

		assertArrayEquals(new int[]{50, 50}, allocation);
		assertArrayEquals(new long[]{800_000_000, 800_000_000}, maxMemory);
	}

	@Test
	void allocatesMorePeopleToTheFasterSlave() {
		int[] allocation = allocate(
				new double[]{100, 200}, new int[]{10, 10}, new long[]{1_000_000_000, 1_000_000_000},
				0, 100);

		assertArrayEquals(new int[]{66, 34}, allocation);
	}

	@Test
	void givesNewSlaveTheFastestObservedTimePerPlan() {
		int[] allocation = allocate(
				new double[]{100, 0}, new int[]{10, 0}, new long[]{1_000_000_000, 1_000_000_000},
				0, 100);

		assertArrayEquals(new int[]{50, 50}, allocation);
	}

	@Test
	void dampensMovementAwayFromCurrentPopulation() {
		int[] allocation = allocate(
				new double[]{100, 200}, new int[]{10, 10}, new long[]{1_000_000_000, 1_000_000_000},
				0.5, 100);

		assertArrayEquals(new int[]{62, 38}, allocation);
	}

	@Test
	void returnsCurrentPopulationWhenMemoryLimitsProduceNonPositiveAllocation() {
		int[] currentPopulation = {1, 1};

		int[] allocation = MasterControler.getSlaveTargetPopulationSizes(
				new double[]{1, 1}, currentPopulation,
				new long[]{200_001_000, 200_001_000}, new long[]{1_000, 1_000},
				1_000, 100, 0, 2);

		assertArrayEquals(currentPopulation, allocation);
		assertEquals(10.0, MasterControler.planAllocationLimiter);
	}

	@Test
	void acceptsEmptySlaveArraysWhenPopulationIsEmpty() {
		int[] allocation = MasterControler.getSlaveTargetPopulationSizes(
				new double[0], new int[0], new long[0], new long[0], 1_000, 100, 0, 0);

		assertArrayEquals(new int[0], allocation);
	}

	private int[] allocate(double[] iterationTimes, int[] persons, long[] maxMemory,
			double dampeningFactor, int populationSize) {
		return MasterControler.getSlaveTargetPopulationSizes(
				iterationTimes, persons, maxMemory, new long[]{1_000, 1_000},
				1_000, 100, dampeningFactor, populationSize);
	}
}
