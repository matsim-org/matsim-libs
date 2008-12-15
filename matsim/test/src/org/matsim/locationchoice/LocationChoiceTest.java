package org.matsim.locationchoice;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.matsim.network.NetworkLayer;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.locationchoice.LocationChoice;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.testcases.MatsimTestCase;


public class LocationChoiceTest  extends MatsimTestCase {
	
	private LocationChoice locationchoice = null;
	private Initializer initializer;
	
	public LocationChoiceTest() {
	}
	
	protected void setUp() throws Exception {
        super.setUp();
        this.initializer = new Initializer();
        this.initializer.init(this);
        this.initialize();     
    }
	
	protected void tearDown() throws Exception {
         super.tearDown();
         Gbl.reset();
    }
	
	private void initialize() {		
		this.locationchoice = new LocationChoice(this.initializer.getControler().getNetwork(), 
				this.initializer.getControler());
	}
	
	public void testConstructorandInitLocal() {
		assertNotNull("controler not initialized", locationchoice.getControler());
		assertNotNull("network not initialized", locationchoice.getNetwork());
	}

	public void testInitLocal() throws SecurityException, NoSuchMethodException, IllegalArgumentException, 
		IllegalAccessException, InvocationTargetException {
		
		// TODO: why is it not working in constructor?
		//this.initialize();
		
		locationchoice.setControler(null);
		locationchoice.setNetwork(null);
		
		Gbl.getConfig().locationchoice();
		
        Method method = null;
		method = this.locationchoice.getClass().getDeclaredMethod("initLocal", new Class[]{NetworkLayer.class, Controler.class});
		method.setAccessible(true);
		method.invoke(this.locationchoice, new Object[]{this.initializer.getControler().getNetwork(),
				this.initializer.getControler()});
		
		assertNotNull("controler not initialized", this.locationchoice.getControler());
		assertNotNull("network not initialized", this.locationchoice.getNetwork());
	}
	
	public void testGetPlanAlgoInstance() {
		//this.initialize();
		locationchoice.setConstrained(false);
		assertEquals(locationchoice.getPlanAlgoInstance().getClass(), RandomLocationMutator.class);
		locationchoice.setConstrained(true);
		assertEquals(locationchoice.getPlanAlgoInstance().getClass(), LocationMutatorwChoiceSet.class);
	}	
	
	public void testFinish() {	
		//this.initialize();
		locationchoice.getPlanAlgoInstance();
		assertEquals(false, locationchoice.getPlanAlgoInstances().isEmpty());
		locationchoice.init();
		locationchoice.finish();
		assertEquals(true , locationchoice.getPlanAlgoInstances().isEmpty());
	}
	
}