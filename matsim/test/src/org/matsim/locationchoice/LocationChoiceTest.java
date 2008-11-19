package org.matsim.locationchoice;

import java.lang.reflect.Method;
import org.matsim.network.NetworkLayer;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.locationchoice.LocationChoice;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSetSimultan;
import org.matsim.testcases.MatsimTestCase;


public class LocationChoiceTest  extends MatsimTestCase {
	
	LocationChoice locationchoice = null;
	Controler controler = null;
	
	public LocationChoiceTest() {
		this.initialize();
	}
	
	private void initialize() {
		Gbl.reset();
		String path = "test/input/org/matsim/locationchoice/config.xml";		
		String configpath[] = {path};
		controler = new Controler(configpath);
		controler.setOverwriteFiles(true);
		controler.run();		
		this.locationchoice = new LocationChoice(controler.getNetwork(), controler);
	}
	
	public void testConstructorandInitLocal() {
		assertNotNull("controler not initialized", locationchoice.getControler());
		assertNotNull("network not initialized", locationchoice.getNetwork());
	}

	public void testInitLocal() {
		
		// TODO: why is it not working in constructor?
		this.initialize();
		
		locationchoice.setControler(null);
		locationchoice.setNetwork(null);
		
		Gbl.getConfig().locationchoice();
		
		try {
            Method method = this.locationchoice.getClass().getDeclaredMethod("initLocal", new Class[]{NetworkLayer.class, Controler.class});
            method.setAccessible(true);
            method.invoke(this.locationchoice, new Object[]{controler.getNetwork(), controler});
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
		assertNotNull("controler not initialized", this.locationchoice.getControler());
		assertNotNull("network not initialized", this.locationchoice.getNetwork());
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