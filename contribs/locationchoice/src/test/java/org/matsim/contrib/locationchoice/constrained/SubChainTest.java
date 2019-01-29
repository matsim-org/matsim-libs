package org.matsim.contrib.locationchoice.constrained;

import org.matsim.contrib.locationchoice.timegeography.SubChain;
import org.matsim.testcases.MatsimTestCase;

public class SubChainTest extends MatsimTestCase {
	
	public void testConstructorandGetSlActs() {	
		SubChain subchain = new SubChain();
		assertNotNull(subchain.getSlActs());
	}
}