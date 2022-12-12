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
        double decoded = Agent.decodeVelocityFromLinkEntry(encoded);
        Assert.assertEquals(15.0, decoded, 0.0);
    }

    @Test
    public void encodingFastDecimalPointSpeedRoundsUpToNearestInteger() {
        int encoded = Agent.prepareVelocityForLinkEntry(15.6);
        double decoded = Agent.decodeVelocityFromLinkEntry(encoded);
        Assert.assertEquals(16.0, decoded, 0.0);
    }

	@Test
	public void slowSpeedIsEncodedByMultiplyingBy10() {
		int encoded = Agent.prepareVelocityForLinkEntry(4.6);
		Assert.assertEquals((int) 4.6 * 10, encoded);
	}

	@Test
	public void slowSpeedIsDecodedByDividingBy10() {
		double decoded = Agent.decodeVelocityFromLinkEntry((int) 4.6 * 10);
		Assert.assertEquals(4.6, decoded, 0.0);
	}

    @Test
    public void slowIntegerSpeedIsEncodedCorrectly() {
        int encoded = Agent.prepareVelocityForLinkEntry(4);
        Assert.assertEquals(40, encoded);

        double decoded = Agent.decodeVelocityFromLinkEntry(encoded);
        Assert.assertEquals(4.0, decoded, 0.0);
    }

    @Test
    public void encodingSlowDecimalPointSpeedRoundsDown() {
        int encoded = Agent.prepareVelocityForLinkEntry(5.33);
        double decoded = Agent.decodeVelocityFromLinkEntry(encoded);
        Assert.assertEquals(5.3, decoded, 0.0);
    }

    @Test
    public void encodingSlowDecimalPointSpeedRoundsUp() {
        int encoded = Agent.prepareVelocityForLinkEntry(5.66);
        double decoded = Agent.decodeVelocityFromLinkEntry(encoded);
        Assert.assertEquals(5.7, decoded, 0.0);
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
