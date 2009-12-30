package org.matsim.locationchoice.constrained;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.locationchoice.Initializer;
import org.matsim.testcases.MatsimTestCase;

public class ManageSubchainsTest extends MatsimTestCase {
	
	private ManageSubchains manager = null;
	private Initializer initializer;
	
	public ManageSubchainsTest() {
		manager = new ManageSubchains();
	}
	
	@Override
	protected void setUp() throws Exception {
        super.setUp();
        this.initializer = new Initializer();
        this.initializer.init(this);     
    }
	
	@Override
	protected void tearDown() throws Exception {
		this.initializer = null;
		this.manager = null;
		super.tearDown();
	}
	
	public void testPrimarySecondaryActivityFound() {
		Plan plan = this.initializer.getControler().getPopulation().getPersons().get(new IdImpl("1")).getSelectedPlan();
		ActivityImpl act = ((PlanImpl) plan).getFirstActivity();
		LegImpl leg = ((PlanImpl) plan).getNextLeg(act);
		this.manager.primaryActivityFound(act, leg);
		assertEquals(act, manager.getSubChains().get(0).getFirstPrimAct());
		
		act = ((PlanImpl) plan).getNextActivity(leg);
		this.manager.secondaryActivityFound(act, ((PlanImpl) plan).getNextLeg(act));
		assertEquals(act, manager.getSubChains().get(0).getSlActs().get(0));
		
		/*
		try {
			Field field = this.manager.getClass().getDeclaredField("subChains");
			field.setAccessible(true);
		 } catch (NoSuchFieldException  e) {
	            e.printStackTrace();
	            fail();
	     }
	     */	 
	}
}