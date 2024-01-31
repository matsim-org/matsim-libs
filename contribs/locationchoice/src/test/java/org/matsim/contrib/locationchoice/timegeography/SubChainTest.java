package org.matsim.contrib.locationchoice.timegeography;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

public class SubChainTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testConstructorandGetSlActs() {
		SubChain subchain = new SubChain();
		assertNotNull(subchain.getSlActs());
	}
}
