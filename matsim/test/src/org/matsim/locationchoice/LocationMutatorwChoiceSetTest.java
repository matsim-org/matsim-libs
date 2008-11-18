package org.matsim.locationchoice;

import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;


public class LocationMutatorwChoiceSetTest  extends MatsimTestCase {
	
	RandomLocationMutator locationmutatorcs = null;
	Controler controler = null;
	
	public LocationMutatorwChoiceSetTest() {
		initialize();
	}
	
	private void initialize() {
		Gbl.reset();
		String path = "test/input/org/matsim/locationchoice/config.xml";		
		String configpath[] = {path};
		controler = new Controler(configpath);
		controler.setOverwriteFiles(true);
		controler.run();		
		this.locationmutatorcs = new RandomLocationMutator(controler.getNetwork(), controler);
	}

	/* TODO: Construct scenario with knowledge to compare plans before and after loc. choice
	 * 
	 */
	public void testhandlePlan() {
		this.locationmutatorcs.handlePlan(controler.getPopulation().getPerson("1").getSelectedPlan());	
	}	
}