package org.matsim.signalsystems;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.signalsystems.CalculateAngle;
import org.matsim.testcases.MatsimTestCase;


/**
 * @author aneumann
 *
 */
public class CalculateAngleTest extends MatsimTestCase {
    
	public void testCalculateAngle() {
		Config conf = loadConfig(this.getClassInputDirectory() + "config.xml");
		ScenarioImpl data = new ScenarioImpl(conf);
		
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