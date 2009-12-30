package org.matsim.locationchoice;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;


public class RandomLocationMutatorTest  extends MatsimTestCase {

	private RandomLocationMutator randomlocationmutator = null;
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
		this.randomlocationmutator = null;
		this.initializer = null;
		super.tearDown();
	}

	private void initialize() {		
		this.randomlocationmutator = new RandomLocationMutator(this.initializer.getControler().getNetwork(), 
				this.initializer.getControler(), ((ScenarioImpl)this.initializer.getControler().getScenario()).getKnowledges());
	}

	/* 
	 * TODO: Construct scenario with knowledge to compare plans before and after loc. choice
	 */
	public void testHandlePlan() {
		this.initialize();
		this.randomlocationmutator.handlePlan(
				this.initializer.getControler().getPopulation().getPersons().get(new IdImpl("1")).getSelectedPlan());	
	}	
}