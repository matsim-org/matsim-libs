package org.matsim.contrib.locationchoice.timegeography;

import org.matsim.testcases.MatsimTestCase;

public class SubChainTest extends MatsimTestCase {
	
	public void testConstructorandGetSlActs() {	
		SubChain subchain = new SubChain();
		assertNotNull(subchain.getSlActs());
	}
}
