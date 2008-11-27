package org.matsim.locationchoice.facilityload;

import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;

public class ScoringPenaltyTest  extends MatsimTestCase {	
	private ScoringPenalty scoringpenalty = null;
	private Controler controler = null;
		
	public ScoringPenaltyTest() {
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
	
	public void testGetPenalty() {
		this.initialize();
		FacilityPenalty facilityPenalty = new FacilityPenalty(0.0, 1);
		this.scoringpenalty = new ScoringPenalty(0.0, 1.0, facilityPenalty, 1.0);
		assertEquals(this.scoringpenalty.getPenalty(), 0.0);
	}	
}