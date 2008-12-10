package org.matsim.locationchoice.constrained;


import java.util.List;

import org.matsim.population.Plan;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.testcases.MatsimTestCase;


public class LocationMutatorwChoiceSetTest  extends MatsimTestCase {
	
	private LocationMutatorwChoiceSet locationmutator = null;
	private Controler controler = null;
	
	public LocationMutatorwChoiceSetTest() {
	}
	
	private void initialize() {
		Gbl.reset();
		String path = "test/input/org/matsim/locationchoice/config.xml";		
		String configpath[] = {path};
		controler = new Controler(configpath);
		controler.setOverwriteFiles(true);
		controler.run();		
		this.locationmutator = new LocationMutatorwChoiceSet(controler.getNetwork(), controler);
	}
	
	public void testConstructor() {
		this.initialize();
		assertEquals(this.locationmutator.getMaxRecursions(), 10);
		assertEquals(this.locationmutator.getRecursionTravelSpeedChange(), 0.1);
	}
	
	
	public void testHandlePlan() {
		this.initialize();
		Plan plan = controler.getPopulation().getPerson("1").getSelectedPlan();		
		this.locationmutator.handlePlan(plan);
		assertEquals(plan.getFirstActivity().getCoord().getX(), -25000.0);
		assertEquals(plan.getNextLeg(plan.getFirstActivity()).getRoute(), null);
	}	
	
	public void testCalcActChains() {
		this.initialize();
		Plan plan = controler.getPopulation().getPerson("1").getSelectedPlan();		
		List<SubChain> list = this.locationmutator.calcActChains(plan);
		assertEquals(list.size(), 1);
	}
}