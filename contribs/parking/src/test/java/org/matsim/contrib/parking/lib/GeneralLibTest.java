package org.matsim.contrib.parking.lib;

import static org.junit.Assert.*;

import org.junit.Test;
import org.matsim.contrib.parking.parkingchoice.lib.GeneralLib;

public class GeneralLibTest {

	@Test
	public void testProjectTimeWithin24Hours() {
		assertEquals(10.0, GeneralLib.projectTimeWithin24Hours(10.0), 0);
		assertEquals(0.0, GeneralLib.projectTimeWithin24Hours(60 * 60 * 24.0), 0);
		assertEquals(1.0, GeneralLib.projectTimeWithin24Hours(60 * 60 * 24.0 + 1), 0.1);
		assertEquals(60 * 60 * 24.0 - 1, GeneralLib.projectTimeWithin24Hours(-1), 0.1);
	}

	@Test
	public void testGetIntervalDuration() {
		assertEquals(10.0, GeneralLib.getIntervalDuration(0.0, 10.0), 0);
		assertEquals(11.0, GeneralLib.getIntervalDuration(60 * 60 * 24.0 - 1.0, 10.0), 0);
	}

	@Test
	public void testIsIn24HourInterval() {
		assertTrue(GeneralLib.isIn24HourInterval(0.0, 10.0, 9.0));
		assertFalse(GeneralLib.isIn24HourInterval(0.0, 10.0, 11.0));
		assertTrue(GeneralLib.isIn24HourInterval(0.0, 10.0, 0.0));
		assertTrue(GeneralLib.isIn24HourInterval(0.0, 10.0, 10.0));

		assertFalse(GeneralLib.isIn24HourInterval(10.0, 3.0, 9.0));
		assertTrue(GeneralLib.isIn24HourInterval(10.0, 3.0, 11.0));
		assertTrue(GeneralLib.isIn24HourInterval(10.0, 3.0, 2.0));
	}
}
