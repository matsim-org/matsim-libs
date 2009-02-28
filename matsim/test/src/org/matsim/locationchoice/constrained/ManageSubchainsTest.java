package org.matsim.locationchoice.constrained;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Plan;
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
		Plan plan = this.initializer.getControler().getPopulation().getPerson(new IdImpl("1")).getSelectedPlan();
		Act act = plan.getFirstActivity();
		Leg leg = plan.getNextLeg(act);
		this.manager.primaryActivityFound(act, leg);
		assertEquals(act, manager.getSubChains().get(0).getFirstPrimAct());
		
		act = plan.getNextActivity(leg);
		this.manager.secondaryActivityFound(act, plan.getNextLeg(act));
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