package playground.mzilske.cdr;

import org.junit.Assert;
import org.junit.Test;

public class CDREquilTest {
	
	
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
		oneWorkplace.run();
		Assert.assertEquals("All-day squares", 0.0, oneWorkplace.getCompare().compareAllDay(), 0.0);
		Assert.assertEquals("Timebin squares", 0.0, oneWorkplace.getCompare().compareTimebins(), 0.0);
		Assert.assertEquals("EMD", 0.0, oneWorkplace.getCompare().compareEMD(), 0.0);
	}
	
	@Test
	public void testTwoWorkplaces() {
		TwoWorkplaces twoWorkplaces = new TwoWorkplaces();
		twoWorkplaces.run();
		Assert.assertEquals("All-day squares", 0.0, twoWorkplaces.getCompare().compareAllDay(), 0.0);
		Assert.assertEquals("Timebin squares", 0.0, twoWorkplaces.getCompare().compareTimebins(), 0.0);
		Assert.assertEquals("EMD", 0.0, twoWorkplaces.getCompare().compareEMD(), 0.0);
	}

}
