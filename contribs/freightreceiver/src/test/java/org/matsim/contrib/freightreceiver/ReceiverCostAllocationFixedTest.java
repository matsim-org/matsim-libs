package org.matsim.contrib.freightreceiver;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.matsim.testcases.MatsimTestUtils;

public class ReceiverCostAllocationFixedTest {

	@Test
	void getScore() {
		Assert.assertEquals("Wrong cost.", -20.0, new ReceiverCostAllocationFixed(20.0).getScore(null, null), MatsimTestUtils.EPSILON);
	}
}
