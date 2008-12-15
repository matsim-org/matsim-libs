package org.matsim.locationchoice;

import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;


public class RandomLocationMutatorTest  extends MatsimTestCase {
	
	private RandomLocationMutator randomlocationmutator = null;
	private Initializer initializer;
	
	public RandomLocationMutatorTest() {
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
		this.randomlocationmutator = new RandomLocationMutator(this.initializer.getControler().getNetwork(), 
				this.initializer.getControler());
	}

	/* 
	 * TODO: Construct scenario with knowledge to compare plans before and after loc. choice
	 */
	public void testHandlePlan() {
		this.initialize();
		this.randomlocationmutator.handlePlan(
				this.initializer.getControler().getPopulation().getPerson("1").getSelectedPlan());	
	}	
}