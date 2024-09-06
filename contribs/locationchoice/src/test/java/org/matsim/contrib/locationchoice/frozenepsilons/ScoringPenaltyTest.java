package org.matsim.contrib.locationchoice.frozenepsilons;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

public class ScoringPenaltyTest {

	@Test
	void testGetPenalty() {
		FacilityPenalty facilityPenalty = new FacilityPenalty(0.0, new DestinationChoiceConfigGroup());
		ScoringPenalty scoringpenalty = new ScoringPenalty(0.0, 1.0, facilityPenalty, 1.0);
		Assertions.assertEquals(scoringpenalty.getPenalty(), 0.0, MatsimTestUtils.EPSILON);
	}
}
