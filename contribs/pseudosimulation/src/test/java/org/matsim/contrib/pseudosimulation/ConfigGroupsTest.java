package org.matsim.contrib.pseudosimulation;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.pseudosimulation.distributed.DistributedSimConfigGroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigGroupsTest {

	@Test
	void psimDefaultsAndSettersRoundTripWithoutValidation() {
		PSimConfigGroup config = new PSimConfigGroup();

		assertEquals("psim", config.getName());
		assertEquals(5, config.getIterationsPerCycle());
		assertTrue(config.isFullTransitPerformanceTransmission());

		config.setIterationsPerCycle(-3);
		config.setFullTransitPerformanceTransmission(false);
		assertEquals(-3, config.getIterationsPerCycle());
		assertFalse(config.isFullTransitPerformanceTransmission());
	}

	@Test
	void distributedDefaultsAreCharacterized() {
		DistributedSimConfigGroup config = new DistributedSimConfigGroup();

		assertEquals("distributed", config.getName());
		assertEquals(12345, config.getMasterPortNumber());
		assertEquals(10, config.getSlaveIterationsPerMasterIteration());
		assertEquals(1, config.getDefaultNumThreadsOnSlave());
		assertEquals(2, config.getInitialNumberOfSlaves());
		assertEquals(0.1, config.getMasterMutationRate());
		assertEquals(0.3, config.getSlaveMutationRate());
		assertEquals(0.6667, config.getMasterBorrowingRate());
		assertTrue(config.isIntelligentRouters());
		assertTrue(config.isFullTransitPerformanceTransmission());
		assertTrue(config.isSlavesRunInParallelToMaster());
	}

	@Test
	void distributedSettersRoundTripEvenOutOfRangeValues() {
		DistributedSimConfigGroup config = new DistributedSimConfigGroup();
		config.setMasterPortNumber(-1);
		config.setSlaveIterationsPerMasterIteration(0);
		config.setDefaultNumThreadsOnSlave(-2);
		config.setInitialNumberOfSlaves(-3);
		config.setMasterMutationRate(1.2);
		config.setSlaveMutationRate(-0.2);
		config.setMasterBorrowingRate(2.0);
		config.setIntelligentRouters(false);
		config.setFullTransitPerformanceTransmission(false);
		config.setSlavesRunInParallelToMaster(false);

		assertEquals(-1, config.getMasterPortNumber());
		assertEquals(0, config.getSlaveIterationsPerMasterIteration());
		assertEquals(-2, config.getDefaultNumThreadsOnSlave());
		assertEquals(-3, config.getInitialNumberOfSlaves());
		assertEquals(1.2, config.getMasterMutationRate());
		assertEquals(-0.2, config.getSlaveMutationRate());
		assertEquals(2.0, config.getMasterBorrowingRate());
		assertFalse(config.isIntelligentRouters());
		assertFalse(config.isFullTransitPerformanceTransmission());
		assertFalse(config.isSlavesRunInParallelToMaster());
	}
}
