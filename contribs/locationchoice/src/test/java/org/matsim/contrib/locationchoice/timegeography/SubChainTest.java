package org.matsim.contrib.locationchoice.timegeography;

import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class SubChainTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test public void testConstructorandGetSlActs() {
		SubChain subchain = new SubChain();
		assertNotNull(subchain.getSlActs());
	}
}
