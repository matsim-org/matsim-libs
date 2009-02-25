package org.matsim.locationchoice.constrained;


import java.util.List;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.locationchoice.Initializer;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.testcases.MatsimTestCase;


public class LocationMutatorwChoiceSetTest  extends MatsimTestCase {
	
	private LocationMutatorwChoiceSet locationmutator = null;
	private Initializer initializer;
	
	public LocationMutatorwChoiceSetTest() {
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
		this.locationmutator = new LocationMutatorwChoiceSet(this.initializer.getControler().getNetwork(),
				this.initializer.getControler());
	}
	
	public void testConstructor() {
		this.initialize();
		assertEquals(this.locationmutator.getMaxRecursions(), 10);
		assertEquals(this.locationmutator.getRecursionTravelSpeedChange(), 0.1);
	}
	
	
	public void testHandlePlan() {
		this.initialize();
		Plan plan = this.initializer.getControler().getPopulation().getPerson("1").getSelectedPlan();		
		this.locationmutator.handlePlan(plan);
		assertEquals(plan.getFirstActivity().getCoord().getX(), -25000.0);
		assertEquals(plan.getNextLeg(plan.getFirstActivity()).getRoute(), null);
	}	
	
	public void testCalcActChains() {
		this.initialize();
		Plan plan = this.initializer.getControler().getPopulation().getPerson("1").getSelectedPlan();		
		List<SubChain> list = this.locationmutator.calcActChains(plan);
		assertEquals(list.size(), 1);
	}
}