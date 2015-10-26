package org.matsim.contrib.locationchoice.facilityload;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.contrib.locationchoice.facilityload.ScoringPenalty;
import org.matsim.testcases.MatsimTestUtils;

public class ScoringPenaltyTest {

	@Test
	public void testGetPenalty() {
		FacilityPenalty facilityPenalty = new FacilityPenalty(0.0, 1, new DestinationChoiceConfigGroup());
		ScoringPenalty scoringpenalty = new ScoringPenalty(0.0, 1.0, facilityPenalty, 1.0);
		Assert.assertEquals(scoringpenalty.getPenalty(), 0.0, MatsimTestUtils.EPSILON);
	}
}