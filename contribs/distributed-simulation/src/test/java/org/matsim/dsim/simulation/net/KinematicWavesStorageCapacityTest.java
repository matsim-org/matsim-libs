package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.dsim.TestUtils;

import static org.junit.jupiter.api.Assertions.*;

class KinematicWavesStorageCapacityTest {

	@Test
	void initIncreaseStorageForHoles() {

		var link = TestUtils.createSingleLink();
		link.setLength(10);
		link.setFreespeed(10);
		link.setNumberOfLanes(2);
		link.setCapacity(5400);
		var capacity = new KinematicWavesStorageCapacity(link, 10);
		var expectedStorageCap = link.getFlowCapacityPerSec() * link.getLength() * (1 / link.getFreespeed() + 1. / 15. * 3.6);
		assertEquals(expectedStorageCap, capacity.getMax(), 0.001);
	}

	@Test
	void testState() {

		var link = TestUtils.createSingleLink();
		var holeTravelTime = link.getLength() / KinematicWavesStorageCapacity.HOLE_SPEED;
		var capacity = new KinematicWavesStorageCapacity(link, 10);
		assertEquals(10, capacity.getMax(), 0.001);
		assertTrue(capacity.isAvailable());

		// capacity is consumed immediately
		capacity.consume(5);
		assertTrue(capacity.isAvailable());
		capacity.consume(5);
		assertFalse(capacity.isAvailable());

		// capacity is released deferred because of holes
		capacity.release(5, 0);
		assertFalse(capacity.isAvailable());
		capacity.update(holeTravelTime - 1);
		assertFalse(capacity.isAvailable());
		capacity.update(holeTravelTime);
		assertTrue(capacity.isAvailable());
	}
}
