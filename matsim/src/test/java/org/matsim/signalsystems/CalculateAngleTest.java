package org.matsim.signalsystems;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.testcases.MatsimTestCase;


/**
 * @author aneumann
 *
 */
public class CalculateAngleTest extends MatsimTestCase {
    
	public void testCalculateAngle() {
		Config conf = loadConfig(this.getClassInputDirectory() + "config.xml");
		ScenarioImpl data = new ScenarioImpl(conf);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(data);
		loader.loadNetwork();

		assertEquals("Has to be 'null', since there is no other way back but Link 11.",
				null, CalculateAngle.getLeftLane(data.getNetwork().getLinks().get(new IdImpl("1"))));

		assertEquals(
				data.getNetwork().getLinks().get(data.createId("2")), CalculateAngle.getLeftLane(data.getNetwork().getLinks().get(data.createId("11"))));

		assertEquals(
				data.getNetwork().getLinks().get(data.createId("3")), CalculateAngle.getLeftLane(data.getNetwork().getLinks().get(data.createId("22"))));
		
		assertEquals(
				data.getNetwork().getLinks().get(data.createId("4")), CalculateAngle.getLeftLane(data.getNetwork().getLinks().get(data.createId("33"))));
		
		assertEquals(
				data.getNetwork().getLinks().get(data.createId("1")), CalculateAngle.getLeftLane(data.getNetwork().getLinks().get(data.createId("44"))));
		
		assertEquals(
				data.getNetwork().getLinks().get(data.createId("5")), CalculateAngle.getLeftLane(data.getNetwork().getLinks().get(data.createId("3"))));
				
	}
	
}