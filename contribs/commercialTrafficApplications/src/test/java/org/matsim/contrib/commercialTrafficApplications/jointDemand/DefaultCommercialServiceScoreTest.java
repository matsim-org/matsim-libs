package org.matsim.contrib.commercialTrafficApplications.jointDemand;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.matsim.contrib.commercialTrafficApplications.jointDemand.DefaultCommercialServiceScore;

public class DefaultCommercialServiceScoreTest {

	@Test
	void calcScore() {
        DefaultCommercialServiceScore serviceScore = new DefaultCommercialServiceScore(6, -6, 1800);

        Assert.assertEquals(serviceScore.calcScore(0), 6., 0.001);
        Assert.assertEquals(serviceScore.calcScore(1800), 0, 0.001);
        Assert.assertEquals(serviceScore.calcScore(3600), -6., 0.001);
        Assert.assertEquals(serviceScore.calcScore(7200), -6., 0.001);

    }
}