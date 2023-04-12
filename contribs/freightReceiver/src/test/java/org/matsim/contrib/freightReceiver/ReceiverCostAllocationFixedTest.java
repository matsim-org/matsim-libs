package org.matsim.contrib.freightReceiver;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class ReceiverCostAllocationFixedTest {

	@Test
	public void getScore() {
		Assert.assertEquals("Wrong cost.", -20.0, new ReceiverCostAllocationFixed(20.0).getScore(null, null), MatsimTestUtils.EPSILON);
	}
}
