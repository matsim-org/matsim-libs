package org.matsim.contrib.commercialTrafficApplications.jointDemand;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.contrib.commercialTrafficApplications.jointDemand.DefaultCommercialServiceScore;

public class DefaultCommercialServiceScoreTest {

	@Test
	void calcScore() {
        DefaultCommercialServiceScore serviceScore = new DefaultCommercialServiceScore(6, -6, 1800);

        Assertions.assertEquals(6., serviceScore.calcScore(0), 0.001);
        Assertions.assertEquals(0, serviceScore.calcScore(1800), 0.001);
        Assertions.assertEquals(-6., serviceScore.calcScore(3600), 0.001);
        Assertions.assertEquals(-6., serviceScore.calcScore(7200), 0.001);

    }
}
