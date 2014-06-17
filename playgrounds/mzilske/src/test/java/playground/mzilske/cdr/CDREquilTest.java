package playground.mzilske.cdr;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class CDREquilTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();
	
	
	/*
	 * Run a simple uncongested loop. One half of the population habitually phones at the beginning of activities, the other at
	 * the ends of activities.
	 * All of the population get two plans each, one which stays at activities as long as possible to reach the next sighting
	 * (assuming an uncongested network), and one (simpler) which departs each location as soon as the agent has been seen 
	 * there for the last consecutive time.
	 * 
	 * We expect Cadyts to find the 100% correct fit.
	 *  
	 */
	@Test
	public void testOneWorkplace() {
		OneWorkplace oneWorkplace = new OneWorkplace();
		oneWorkplace.run(utils.getOutputDirectory());
		Assert.assertEquals("All-day squares", 0.0, CompareMain.compareAllDay(oneWorkplace.scenario, oneWorkplace.getCdrVolumes(), oneWorkplace.getCompare().getGroundTruthVolumes()), 0.0);
		Assert.assertEquals("Timebin squares", 0.0, CompareMain.compareTimebins(oneWorkplace.scenario, oneWorkplace.getCdrVolumes(), oneWorkplace.getCompare().getGroundTruthVolumes()), 0.0);
		Assert.assertEquals("EMD", 0.0, CompareMain.compareEMD(oneWorkplace.scenario, oneWorkplace.getCdrVolumes(), oneWorkplace.getCompare().getGroundTruthVolumes()), 0.0);
	}
	
	@Test
	public void testTwoWorkplaces() {
		TwoWorkplaces twoWorkplaces = new TwoWorkplaces();
		twoWorkplaces.run(utils.getOutputDirectory());
		Assert.assertEquals("All-day squares", 0.0, CompareMain.compareAllDay(twoWorkplaces.scenario, twoWorkplaces.getCdrVolumes(), twoWorkplaces.getCompare().getGroundTruthVolumes()), 0.0);
		Assert.assertEquals("Timebin squares", 0.0, CompareMain.compareTimebins(twoWorkplaces.scenario, twoWorkplaces.getCdrVolumes(), twoWorkplaces.getCompare().getGroundTruthVolumes()), 0.0);
		Assert.assertEquals("EMD", 0.0, CompareMain.compareEMD(twoWorkplaces.scenario, twoWorkplaces.getCdrVolumes(), twoWorkplaces.getCompare().getGroundTruthVolumes()), 0.0);
	}

}
