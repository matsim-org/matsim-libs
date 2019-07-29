package commercialtraffic.scoring;

import org.junit.Assert;
import org.junit.Test;

public class DefaultCommercialServiceScoreTest {

    @Test
    public void calcScore() {
        DefaultCommercialServiceScore serviceScore = new DefaultCommercialServiceScore(6, -6, 1800);

        Assert.assertEquals(serviceScore.calcScore(0), 6., 0.001);
        Assert.assertEquals(serviceScore.calcScore(1800), 0, 0.001);
        Assert.assertEquals(serviceScore.calcScore(3600), -6., 0.001);
        Assert.assertEquals(serviceScore.calcScore(7200), -6., 0.001);

    }
}