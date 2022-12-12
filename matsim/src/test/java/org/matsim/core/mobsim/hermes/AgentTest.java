/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.mobsim.hermes;

import org.junit.Test;
import org.locationtech.jts.util.Assert;


public class AgentTest {

	@Test
	public void prepareLinkEntry() {
		for (int eventid = 1; eventid < HermesConfigGroup.MAX_EVENTS_AGENT; eventid *= 8) {
			for (int lid = 1; lid < HermesConfigGroup.MAX_LINK_ID; lid *= 8) {
				for (int j = 0; j < 255; j++) {
					double v = j < 100 ? ((double) j) / 10.0 : ((double) j) - 90;
					for (int i = 0; i < 15; i++) {
						long flatplanentry = Agent.prepareLinkEntry(eventid, lid, v, i);
						int testedEventId = Agent.getPlanEvent(flatplanentry);
						double testedVelocity = Agent.getVelocityPlanEntry(flatplanentry);
						int testedLinkId = Agent.getLinkPlanEntry(flatplanentry);
						int testedPCECat = Agent.getLinkPCEEntry(flatplanentry);
						Assert.equals(v, testedVelocity);
						Assert.equals(eventid, testedEventId);
						Assert.equals(lid, testedLinkId);
						Assert.equals(i, testedPCECat);
					}
				}
			}
		}

	}

	@Test
	public void fastSpeedIsEncodedByAdding90() {
		int encoded = Agent.prepareVelocityForLinkEntry(15);
		org.junit.Assert.assertEquals(15 + 90, encoded);
	}

	@Test
	public void fastSpeedIsDecodedBySubtracting90() {
		double decoded = Agent.decodeVelocityFromLinkEntry(15 + 90);
		org.junit.Assert.assertEquals(15.0, decoded, 0.0);
	}

	@Test
	public void encodingFastDecimalPointSpeedRoundsDownToNearestInteger() {
		int encoded = Agent.prepareVelocityForLinkEntry(15.3);
		org.junit.Assert.assertEquals(15 + 90, encoded);

	}

	@Test
	public void encodingFastDecimalPointSpeedRoundsUpToNearestInteger() {
		int encoded = Agent.prepareVelocityForLinkEntry(15.6);
		org.junit.Assert.assertEquals(16 + 90, encoded);
	}

	@Test
	public void slowSpeedIsEncodedByMultiplyingBy10() {
		int encoded = Agent.prepareVelocityForLinkEntry(4.6);
		org.junit.Assert.assertEquals(46, encoded);
	}

	@Test
	public void slowSpeedIsDecodedByDividingBy10() {
		double decoded = Agent.decodeVelocityFromLinkEntry(46);
		org.junit.Assert.assertEquals(4.6, decoded, 0.0);
	}

	@Test
	public void encodingSlowDecimalPointSpeedRoundsDown() {
		int encoded = Agent.prepareVelocityForLinkEntry(5.33);
		org.junit.Assert.assertEquals(53, encoded);
	}

	@Test
	public void encodingSlowDecimalPointSpeedRoundsUp() {
		int encoded = Agent.prepareVelocityForLinkEntry(5.66);
		org.junit.Assert.assertEquals(57, encoded);
	}

	@Test
	public void fastSpeedIsIntegerWithFlatPlan() {
		int eventid = 0;
		int linkid = 0;
		double velocity = 15.2;
		int pcecategory = 0;
		long flatplanentry = Agent.prepareLinkEntry(eventid, linkid, velocity, pcecategory);
		double testedVelocity = Agent.getVelocityPlanEntry(flatplanentry);
		org.junit.Assert.assertEquals(15.0, testedVelocity, 0.0);
	}

	@Test
	public void smallSpeedHasOneDecimalPointAccuracyWithFlatPlan() {
		int eventid = 0;
		int linkid = 0;
		double velocity = 3.43;
		int pcecategory = 0;
		long flatplanentry = Agent.prepareLinkEntry(eventid, linkid, velocity, pcecategory);
		double testedVelocity = Agent.getVelocityPlanEntry(flatplanentry);
		org.junit.Assert.assertEquals(3.4, testedVelocity, 0.0);
	}
}
