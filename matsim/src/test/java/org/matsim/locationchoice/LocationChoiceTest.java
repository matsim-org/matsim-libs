package org.matsim.locationchoice;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.testcases.MatsimTestCase;


public class LocationChoiceTest  extends MatsimTestCase {
	
	private LocationChoice locationchoice = null;
	private Initializer initializer;
	
	public LocationChoiceTest() {
	}
	
	@Override
	protected void setUp() throws Exception {
        super.setUp();
        this.initializer = new Initializer();
        this.initializer.init(this);
        this.initialize();     
    }
	
	@Override
	protected void tearDown() throws Exception {
		this.locationchoice = null;
		this.initializer = null;
		super.tearDown();
	}
	
	private void initialize() {		
		this.locationchoice = new LocationChoice(this.initializer.getControler().getNetwork(), 
				this.initializer.getControler(), ((ScenarioImpl)this.initializer.getControler().getScenario()).getKnowledges());
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
		method = this.locationchoice.getClass().getDeclaredMethod("initLocal", new Class[]{Network.class, Controler.class});
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
		locationchoice.prepareReplanning();
		locationchoice.finishReplanning();
		assertEquals(true , locationchoice.getPlanAlgoInstances().isEmpty());
	}
	
}