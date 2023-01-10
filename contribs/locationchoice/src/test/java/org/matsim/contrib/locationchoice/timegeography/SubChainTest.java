package org.matsim.contrib.locationchoice.timegeography;

import static org.junit.Assert.assertNotNull;

import org.matsim.testcases.MatsimTestCase;

public class SubChainTest extends MatsimTestCase {

	@org.junit.Test public void testConstructorandGetSlActs() {
		SubChain subchain = new SubChain();
		assertNotNull(subchain.getSlActs());
	}
}
