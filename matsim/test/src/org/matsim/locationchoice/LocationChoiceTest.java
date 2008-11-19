package org.matsim.locationchoice;

import java.lang.reflect.Method;

import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.locationchoice.LocationChoice;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSetSimultan;
import org.matsim.network.NetworkLayer;
import org.matsim.testcases.MatsimTestCase;


public class LocationChoiceTest  extends MatsimTestCase {
	
	LocationChoice locationchoice = null;
	Controler controler = null;
	
	public LocationChoiceTest() {
		Gbl.reset();
		String path = "test/input/org/matsim/locationchoice/config.xml";		
		String configpath[] = {path};
		controler = new Controler(configpath);
		controler.setOverwriteFiles(true);
		controler.run();		
		this.locationchoice = new LocationChoice(controler.getNetwork(), controler);
	}
	/*	
	 * does not work: static invocation context? Gbl == null
	 * 
	public void testInitLocal() {
				
		try {
            Method method = locationchoice.getClass().getDeclaredMethod("initLocal", new Class[]{NetworkLayer.class, Controler.class});
            method.setAccessible(true);
            method.invoke(locationchoice, new Object[]{controler.getNetwork(), controler});
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
		assertNotNull("controler not initialized", locationchoice.getControler());
		assertNotNull("network not initialized", locationchoice.getNetwork());
	}
	*/	
	public void testConstructorandInitLocal() {
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