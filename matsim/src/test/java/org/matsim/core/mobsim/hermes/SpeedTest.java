package org.matsim.core.mobsim.hermes;

import org.junit.Assert;
import org.junit.Test;

public class SpeedTest {
    @Test
    public void fastSpeedIsEncodedByAdding90() {
        int encoded = Agent.prepareVelocityForLinkEntry(15);
        Assert.assertEquals(15 + 90, encoded);
    }

    @Test
    public void fastSpeedIsDecodedBySubtracting90() {
        double decoded = Agent.decodeVelocityFromLinkEntry(15 + 90);
        Assert.assertEquals(15.0, decoded, 0.0);
    }

    @Test
    public void encodingFastDecimalPointSpeedRoundsDownToNearestInteger() {
        int encoded = Agent.prepareVelocityForLinkEntry(15.3);
        Assert.assertEquals(15 + 90, encoded);

    }

    @Test
    public void encodingFastDecimalPointSpeedRoundsUpToNearestInteger() {
        int encoded = Agent.prepareVelocityForLinkEntry(15.6);
        Assert.assertEquals(16 + 90, encoded);
    }

    @Test
    public void slowSpeedIsEncodedByMultiplyingBy10() {
        int encoded = Agent.prepareVelocityForLinkEntry(4.6);
        Assert.assertEquals((int) (4.6 * 10), encoded);
    }

    @Test
    public void slowSpeedIsDecodedByDividingBy10() {
        double decoded = Agent.decodeVelocityFromLinkEntry((int) (4.6 * 10));
        Assert.assertEquals(4.6, decoded, 0.0);
    }

    @Test
    public void encodingSlowDecimalPointSpeedRoundsDown() {
        int encoded = Agent.prepareVelocityForLinkEntry(5.33);
        Assert.assertEquals((int) (5.3 * 10), encoded);
    }

    @Test
    public void encodingSlowDecimalPointSpeedRoundsUp() {
        int encoded = Agent.prepareVelocityForLinkEntry(5.66);
        Assert.assertEquals((int) (5.7 * 10), encoded);
    }

    @Test
    public void fastSpeedIsIntegerWithFlatPlan() {
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
