package playground.christoph.withinday;

import org.matsim.testcases.MatsimTestCase;

public class EquilTest extends MatsimTestCase {

	public void testScenario(){
		MyWithinDayControler.start(this.getInputDirectory() + "config.xml");
	}
	
}
