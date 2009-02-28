package org.matsim.locationchoice;

import org.matsim.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;


public class RandomLocationMutatorTest  extends MatsimTestCase {

	private RandomLocationMutator randomlocationmutator = null;
	private Initializer initializer;

	protected void setUp() throws Exception {
		super.setUp();
		this.initializer = new Initializer();
		this.initializer.init(this);
		this.initialize();     
	}

	protected void tearDown() throws Exception {
		this.randomlocationmutator = null;
		this.initializer = null;
		super.tearDown();
	}

	private void initialize() {		
		this.randomlocationmutator = new RandomLocationMutator(this.initializer.getControler().getNetwork(), 
				this.initializer.getControler());
	}

	/* 
	 * TODO: Construct scenario with knowledge to compare plans before and after loc. choice
	 */
	public void testHandlePlan() {
		this.initialize();
		this.randomlocationmutator.handlePlan(
				this.initializer.getControler().getPopulation().getPerson(new IdImpl("1")).getSelectedPlan());	
	}	
}