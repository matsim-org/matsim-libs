package org.matsim.utils.eventsfilecomparison;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EventsFileFingerprintComparatorTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testEqual() {

		utils.getClassInputDirectory();


	}

	@Test
	void testDiffTimestamp() {
	}

	@Test
	void testDiffCounts() {
	}

	@Test
	void testDiffContent() {
	}

}
