package playground.wisinee.ipftest;

import junit.framework.TestCase;
import playground.wisinee.IPF.*;

public class RunIpfTest extends TestCase {

	private final static String testPropertyFile = "./test/scenarios/ipf/TestParameter.xml";	
	
	public void testRunIpfCal() {
		RunIPF ipftest = new RunIPF();
		ipftest.runIpfCal(testPropertyFile);
		assertEquals(2,ipftest.nz);
		assertEquals(3,ipftest.nx);
		assertEquals("Income",ipftest.heading);
	}

}
