package org.matsim.core.mobsim.hermes;

import org.junit.Assert;
import org.junit.Test;

public class SpeedTest {
    @Test
    public void encodesIntegerSpeed() {
        int encoded = Agent.prepareVelocityForLinkEntry(15);
        Assert.assertEquals(105, encoded);
    }
    @Test
    public void encodesDecimalPointSpeed() {
        int encoded = Agent.prepareVelocityForLinkEntry(5.5);
        Assert.assertEquals(55, encoded);
    }
    @Test
    public void roundsDownDecimalPointSpeed() {
        int encoded = Agent.prepareVelocityForLinkEntry(5.43);
        Assert.assertEquals(54, encoded);
    }
    @Test
    public void roundsUpDecimalPointSpeed() {
        int encoded = Agent.prepareVelocityForLinkEntry(5.45);
        Assert.assertEquals(55, encoded);
    }
    @Test
    public void decodesIntegerSpeed() {
        double decoded = Agent.decodeVelocityFromLinkEntry(105);
        Assert.assertEquals(15.0, decoded, 0.0);
    }
    @Test
    public void decodesDecimalPointSpeed() {
        double decoded = Agent.decodeVelocityFromLinkEntry(55);
        Assert.assertEquals(5.5, decoded, 0.0);
    }
    @Test
    public void largerSpeedIsIntegerWithFlatPlan() {
        int eventid = 0;
        int linkid = 0;
        double velocity = 15.2;
        int pcecategory = 0;
        long flatplanentry = Agent.prepareLinkEntry(eventid, linkid, velocity, pcecategory);
        double testedVelocity = Agent.getVelocityPlanEntry(flatplanentry);
        Assert.assertEquals(15.0, testedVelocity, 0.0);
    }
    @Test
    public void smallSpeedHasOneDecimalPointAccuracyWithFlatPlan() {
        int eventid = 0;
        int linkid = 0;
        double velocity = 3.43;
        int pcecategory = 0;
        long flatplanentry = Agent.prepareLinkEntry(eventid, linkid, velocity, pcecategory);
        double testedVelocity = Agent.getVelocityPlanEntry(flatplanentry);
        Assert.assertEquals(3.4, testedVelocity, 0.0);
    }
}
