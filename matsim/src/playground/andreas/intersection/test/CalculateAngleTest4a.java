package playground.andreas.intersection.test;

import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.testcases.MatsimTestCase;

import playground.andreas.intersection.tl.CalculateAngle;

/**
 * @author aneumann
 *
 */
public class CalculateAngleTest4a extends MatsimTestCase {
    
	public void testCalculateAngle() {
		
		Config conf = loadConfig("src/playground/andreas/intersection/test/config.xml");
		String netFileName = "src/playground/andreas/intersection/test/data/net_angletest.xml.gz";
		conf.network().setInputFile(netFileName);
		ScenarioData data = new ScenarioData(conf);
		
		assertEquals("Has to be 'null', since there is no other way back but Link 11.",
				null, CalculateAngle.getLeftLane(data.getNetwork().getLink("1")));

		assertEquals(" ",
				data.getNetwork().getLink("2"), CalculateAngle.getLeftLane(data.getNetwork().getLink("11")));

		assertEquals(" ",
				data.getNetwork().getLink("3"), CalculateAngle.getLeftLane(data.getNetwork().getLink("22")));
		
		assertEquals(" ",
				data.getNetwork().getLink("4"), CalculateAngle.getLeftLane(data.getNetwork().getLink("33")));
		
		assertEquals(" ",
				data.getNetwork().getLink("1"), CalculateAngle.getLeftLane(data.getNetwork().getLink("44")));
		
		assertEquals(" ",
				data.getNetwork().getLink("5"), CalculateAngle.getLeftLane(data.getNetwork().getLink("3")));
				
	}	

	public void reset(int iteration) {
	}

}
