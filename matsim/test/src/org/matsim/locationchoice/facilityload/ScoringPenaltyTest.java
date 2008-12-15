package org.matsim.locationchoice.facilityload;

import org.matsim.gbl.Gbl;
import org.matsim.locationchoice.Initializer;
import org.matsim.testcases.MatsimTestCase;

public class ScoringPenaltyTest  extends MatsimTestCase {	
	private ScoringPenalty scoringpenalty = null;
	private Initializer initializer;
		
	public ScoringPenaltyTest() {
	}
	
	protected void setUp() throws Exception {
        super.setUp();
        this.initializer = new Initializer();
        this.initializer.init(this);    
    }
	
	protected void tearDown() throws Exception {
         super.tearDown();
         Gbl.reset();
    }
	
	public void testGetPenalty() {
		FacilityPenalty facilityPenalty = new FacilityPenalty(0.0, 1);
		this.scoringpenalty = new ScoringPenalty(0.0, 1.0, facilityPenalty, 1.0);
		assertEquals(this.scoringpenalty.getPenalty(), 0.0);
	}	
}