package org.matsim.locationchoice;

import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.locationchoice.constrained.ManageSubchains;
import org.matsim.testcases.MatsimTestCase;

public class ManageSubchainsTest extends MatsimTestCase {
	
	ManageSubchains manager = null;
	Controler controler = null;
	
	public ManageSubchainsTest() {
		manager = new ManageSubchains();
		this.initialize();
	}
	
	private void initialize() {
		Gbl.reset();
		String path = "test/input/org/matsim/locationchoice/config.xml";		
		String configpath[] = {path};
		controler = new Controler(configpath);
		controler.setOverwriteFiles(true);
		controler.run();		
	}
	
	public void testPrimarySecondaryActivityFound() {
		Plan plan = controler.getPopulation().getPerson("1").getSelectedPlan();
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