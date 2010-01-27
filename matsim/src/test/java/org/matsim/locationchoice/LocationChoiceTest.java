package org.matsim.locationchoice;

import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.testcases.MatsimTestCase;


public class LocationChoiceTest  extends MatsimTestCase {

	private LocationChoice locationchoice = null;
	private Initializer initializer;

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
				this.initializer.getControler(), (this.initializer.getControler().getScenario()).getKnowledges());
	}

	public void testConstructorandInitLocal() {
		assertNotNull("controler not initialized", locationchoice.getControler());
		assertNotNull("network not initialized", locationchoice.getNetwork());
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