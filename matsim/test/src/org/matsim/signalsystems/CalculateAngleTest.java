package org.matsim.signalsystems;

import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.signalsystems.CalculateAngle;
import org.matsim.testcases.MatsimTestCase;


/**
 * @author aneumann
 *
 */
public class CalculateAngleTest extends MatsimTestCase {
    
	public void testCalculateAngle() {
		Config conf = loadConfig(this.getClassInputDirectory() + "config.xml");
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
	
}