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
		var capacity = KinematicWavesStorageCapacity.create(link, 10);
		var expectedStorageCap = link.getFlowCapacityPerSec() * link.getLength() * (1 / link.getFreespeed() + 1. / 15. * 3.6);
		assertEquals(expectedStorageCap, capacity.getMax(), 0.001);
	}

	@Test
	void holeSpeed() {

		var link = TestUtils.createSingleLink();
		var holeTravelTime = link.getLength() / KinematicWavesStorageCapacity.HOLE_SPEED;
		var capacity = KinematicWavesStorageCapacity.create(link, 10);
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

	@Test
	void releaseMultipleHoles() {
		var link = TestUtils.createSingleLink();
		var holeTravelTime = link.getLength() / KinematicWavesStorageCapacity.HOLE_SPEED;
		var capacity = KinematicWavesStorageCapacity.create(link, 10);
		assertEquals(10, capacity.getMax(), 0.001);
		assertTrue(capacity.isAvailable());

		capacity.consume(20); // consume more capacity than available 20/10
		capacity.release(5, 0);
		capacity.release(4, 1);
		capacity.release(4, 1);

		assertFalse(capacity.isAvailable());
		capacity.update(holeTravelTime);// first hole arrives. Capacity is still oversubscribed
		assertFalse(capacity.isAvailable());
		capacity.update(holeTravelTime + 1); // two holes arrive. Capacity is available
		assertTrue(capacity.isAvailable());
	}
}
