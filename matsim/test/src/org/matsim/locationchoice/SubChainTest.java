package org.matsim.locationchoice;

import org.matsim.locationchoice.constrained.SubChain;
import org.matsim.testcases.MatsimTestCase;


public class SubChainTest extends MatsimTestCase {
	
	SubChain subchain = null;
	
	public SubChainTest() {	
		subchain = new SubChain();
	}
	
	public void testConstructorandGetSlActs() {	
		assertNotNull("", this.subchain.getSlActs());
	}
}