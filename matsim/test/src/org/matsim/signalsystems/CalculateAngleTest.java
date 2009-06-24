package org.matsim.signalsystems;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.ScenarioLoader;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.testcases.MatsimTestCase;


/**
 * @author aneumann
 *
 */
public class CalculateAngleTest extends MatsimTestCase {
    
	public void testCalculateAngle() {
		Config conf = loadConfig(this.getClassInputDirectory() + "config.xml");
		ScenarioImpl data = new ScenarioImpl(conf);
		ScenarioLoader loader = new ScenarioLoader(data);
		loader.loadNetwork();

		assertEquals("Has to be 'null', since there is no other way back but Link 11.",
				null, CalculateAngle.getLeftLane(data.getNetwork().getLink(new IdImpl("1"))));

		assertEquals(
				data.getNetwork().getLink(data.createId("2")), CalculateAngle.getLeftLane(data.getNetwork().getLink(data.createId("11"))));

		assertEquals(
				data.getNetwork().getLink(data.createId("3")), CalculateAngle.getLeftLane(data.getNetwork().getLink(data.createId("22"))));
		
		assertEquals(
				data.getNetwork().getLink(data.createId("4")), CalculateAngle.getLeftLane(data.getNetwork().getLink(data.createId("33"))));
		
		assertEquals(
				data.getNetwork().getLink(data.createId("1")), CalculateAngle.getLeftLane(data.getNetwork().getLink(data.createId("44"))));
		
		assertEquals(
				data.getNetwork().getLink(data.createId("5")), CalculateAngle.getLeftLane(data.getNetwork().getLink(data.createId("3"))));
				
	}
	
}