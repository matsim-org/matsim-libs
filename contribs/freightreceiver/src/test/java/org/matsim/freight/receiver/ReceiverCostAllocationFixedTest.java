package org.matsim.freight.receiver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.testcases.MatsimTestUtils;

public class ReceiverCostAllocationFixedTest {

	@Test
	void getScore() {
		Assertions.assertEquals(-20.0, new ReceiverCostAllocationFixed(20.0).getScore(null, null), MatsimTestUtils.EPSILON, "Wrong cost.");
	}
}
