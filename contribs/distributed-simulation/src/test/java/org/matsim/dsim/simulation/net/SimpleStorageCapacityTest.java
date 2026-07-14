package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.dsim.TestUtils;

import static org.junit.jupiter.api.Assertions.*;

class SimpleStorageCapacityTest {

	@Test
	void initDefaultCapacity() {

		var link = TestUtils.createSingleLink();
		link.setNumberOfLanes(42);
		var capacity = SimpleStorageCapacity.create(link, 7.5);

		assertEquals(link.getLength() * link.getNumberOfLanes() / 7.5, capacity.getMax());
	}

	@Test
	void initLargeFlow() {
		var link = TestUtils.createSingleLink();
		link.setCapacity(36000);
		link.setFreespeed(1000);
		var capacity = SimpleStorageCapacity.create(link, 50);

		assertEquals(10, capacity.getMax());
	}

	@Test
	void initSlowSpeed() {
		var link = TestUtils.createSingleLink();
		link.setCapacity(36000);
		link.setFreespeed(1);
		var capacity = SimpleStorageCapacity.create(link, 50);

		assertEquals(1000, capacity.getMax());
	}

	@Test
	void state() {

		var link = TestUtils.createSingleLink();
		var capacity = SimpleStorageCapacity.create(link, 10);

		assertTrue(capacity.isAvailable());
		capacity.consume(11);
		assertFalse(capacity.isAvailable());
		capacity.release(1, 0);
		assertFalse(capacity.isAvailable());
		capacity.release(2, 0);
		assertTrue(capacity.isAvailable());
	}
}
