package org.matsim.contrib.parking.parkingproxy;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;

import static org.junit.jupiter.api.Assertions.*;

class HectareMapperTest {
	@Test
	void testKey() {
		HectareMapper hectareMapper = new HectareMapper(100);
		assertEquals(0, hectareMapper.getKey(0, 0));
		assertEquals(0, hectareMapper.getKey(0, 50));
		assertEquals(1, hectareMapper.getKey(150, 50));
	}

	@Test
	void center() {
		HectareMapper hectareMapper = new HectareMapper(100);
		assertEquals(new Coord(-50, -50), hectareMapper.getCenter(-1));
		assertEquals(new Coord(50, 50), hectareMapper.getCenter(0));
		assertEquals(new Coord(150, 50), hectareMapper.getCenter(1));
	}
}
