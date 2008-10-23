package org.matsim.locationchoice;

import org.matsim.controler.Controler;
import org.matsim.network.NetworkLayer;
import org.matsim.testcases.MatsimTestCase;

import playground.anhorni.locationchoice.LCControler;

public class LocationChoiceTest  extends MatsimTestCase {
	
	NetworkLayer network = new NetworkLayer();
	Controler controler = new LCControler(null);
	LocationChoice locationchoice = new LocationChoice(network, controler);
	
	public void testInit() {
		locationchoice.init();
		assertNotNull("controler not initialized", locationchoice.getControler());
		assertNotNull("network not initialized", locationchoice.getNetwork());
	}
	
	public void testFinish() {		
		assertEquals("PlanAlgoInstances not empty", true , locationchoice.getPlanAlgoInstances().isEmpty());
	}
	
}