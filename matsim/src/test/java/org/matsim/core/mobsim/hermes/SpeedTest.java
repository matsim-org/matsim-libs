package org.matsim.core.mobsim.hermes;

import org.junit.Assert;
import org.junit.Test;

public class SpeedTest {
	@Test
	public void encodingFastIntegerSpeedLeadsToNoLossOfInformation() {
		int encoded = Agent.prepareVelocityForLinkEntry(15);
		Assert.assertEquals(105, encoded);

		double decoded = Agent.decodeVelocityFromLinkEntry(encoded);
		Assert.assertEquals(15.0, decoded, 0.0);
	}

	@Test
	public void encodingFastDecimalPointSpeedRoundsDown() {
		int encoded = Agent.prepareVelocityForLinkEntry(15.3);
		Assert.assertEquals(105, encoded);

		double decoded = Agent.decodeVelocityFromLinkEntry(encoded);
		Assert.assertEquals(15.0, decoded, 0.0);
	}

	@Test
	public void encodingFastDecimalPointSpeedRoundsUp() {
		int encoded = Agent.prepareVelocityForLinkEntry(15.6);
		Assert.assertEquals(106, encoded);

		double decoded = Agent.decodeVelocityFromLinkEntry(encoded);
		Assert.assertEquals(16.0, decoded, 0.0);
	}

	@Test
	public void encodingSlowIntegerSpeedLeadsToNoLossOfInformation() {
		int encoded = Agent.prepareVelocityForLinkEntry(4);
		Assert.assertEquals(40, encoded);

		double decoded = Agent.decodeVelocityFromLinkEntry(encoded);
		Assert.assertEquals(4.0, decoded, 0.0);
	}

	@Test
	public void encodingSlowDecimalPointSpeedRoundsDown() {
		int encoded = Agent.prepareVelocityForLinkEntry(5.33);
		Assert.assertEquals(53, encoded);

		double decoded = Agent.decodeVelocityFromLinkEntry(encoded);
		Assert.assertEquals(5.3, decoded, 0.0);
	}

	@Test
	public void encodingSlowDecimalPointSpeedRoundsUp() {
		int encoded = Agent.prepareVelocityForLinkEntry(5.66);
		Assert.assertEquals(57, encoded);

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
