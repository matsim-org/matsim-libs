package org.matsim.locationchoice.constrained;


import java.util.List;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.locationchoice.Initializer;
import org.matsim.testcases.MatsimTestCase;


public class LocationMutatorwChoiceSetTest  extends MatsimTestCase {
	
	private LocationMutatorwChoiceSet locationmutator = null;
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
		this.locationmutator = null;
		this.initializer = null;
		super.tearDown();
	}

	private void initialize() {		
		this.locationmutator = new LocationMutatorwChoiceSet(this.initializer.getControler().getNetwork(),
				this.initializer.getControler(), ((ScenarioImpl)this.initializer.getControler().getScenario()).getKnowledges());
	}
	
	public void testConstructor() {
		this.initialize();
		assertEquals(this.locationmutator.getMaxRecursions(), 10);
		assertEquals(this.locationmutator.getRecursionTravelSpeedChange(), 0.1, EPSILON);
	}
	
	
	public void testHandlePlan() {
		this.initialize();
		Plan plan = this.initializer.getControler().getPopulation().getPersons().get(new IdImpl("1")).getSelectedPlan();		
		this.locationmutator.handlePlan(plan);
		assertEquals(((PlanImpl) plan).getFirstActivity().getCoord().getX(), -25000.0, EPSILON);
		assertEquals(((PlanImpl) plan).getNextLeg(((PlanImpl) plan).getFirstActivity()).getRoute(), null);
	}	
	
	public void testCalcActChains() {
		this.initialize();
		Plan plan = this.initializer.getControler().getPopulation().getPersons().get(new IdImpl("1")).getSelectedPlan();		
		List<SubChain> list = this.locationmutator.calcActChains(plan);
		assertEquals(list.size(), 1);
	}
}