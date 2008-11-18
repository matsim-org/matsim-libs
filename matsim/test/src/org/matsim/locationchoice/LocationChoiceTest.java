package org.matsim.locationchoice;

import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.locationchoice.LocationChoice;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSetSimultan;
import org.matsim.testcases.MatsimTestCase;

public class LocationChoiceTest  extends MatsimTestCase {
	
	LocationChoice locationchoice = null;
	
	public LocationChoiceTest() {
		initialize();
	}
	
	private void initialize() {
		Gbl.reset();
		Controler controler = null;
		String path = "test/input/org/matsim/locationchoice/config.xml";		
		String configpath[] = {path};
		controler = new Controler(configpath);
		controler.setOverwriteFiles(true);
		controler.run();		
		this.locationchoice = new LocationChoice(controler.getNetwork(), controler);
	}

	
	public void testInit() {
		locationchoice.init();
		assertNotNull("controler not initialized", locationchoice.getControler());
		assertNotNull("network not initialized", locationchoice.getNetwork());
	}
		
	public void testGetPlanAlgoInstance() {	
		locationchoice.setConstrained(false);
		assertEquals(locationchoice.getPlanAlgoInstance().getClass(), RandomLocationMutator.class);
		locationchoice.setConstrained(true);
		assertEquals(locationchoice.getPlanAlgoInstance().getClass(), LocationMutatorwChoiceSetSimultan.class);
	}	
	
	public void testFinish() {	
		locationchoice.getPlanAlgoInstance();
		assertEquals(false, locationchoice.getPlanAlgoInstances().isEmpty());
		locationchoice.init();
		locationchoice.finish();
		assertEquals(true , locationchoice.getPlanAlgoInstances().isEmpty());
	}
}