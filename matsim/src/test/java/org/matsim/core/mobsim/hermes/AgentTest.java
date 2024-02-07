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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.util.Assert;

public class AgentTest {

	@Test
	void prepareLinkEntry() {
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
	void fastSpeedIsEncodedByAdding90() {
		int encoded = Agent.prepareVelocityForLinkEntry(15);
		assertEquals(15 + 90, encoded);
	}

	@Test
	void fastSpeedIsDecodedBySubtracting90() {
		double decoded = Agent.decodeVelocityFromLinkEntry(15 + 90);
		assertEquals(15.0, decoded, 0.0);
	}

	@Test
	void encodingFastDecimalPointSpeedRoundsDownToNearestInteger() {
		int encoded = Agent.prepareVelocityForLinkEntry(15.3);
		assertEquals(15 + 90, encoded);
	}

	@Test
	void encodingFastDecimalPointSpeedRoundsUpToNearestInteger() {
		int encoded = Agent.prepareVelocityForLinkEntry(15.6);
		assertEquals(16 + 90, encoded);
	}

	@Test
	void slowSpeedIsEncodedByMultiplyingBy10() {
		int encoded = Agent.prepareVelocityForLinkEntry(4.6);
		assertEquals(46, encoded);
	}

	@Test
	void slowSpeedIsDecodedByDividingBy10() {
		double decoded = Agent.decodeVelocityFromLinkEntry(46);
		assertEquals(4.6, decoded, 0.0);
	}

	@Test
	void encodingSlowDecimalPointSpeedRoundsDown() {
		int encoded = Agent.prepareVelocityForLinkEntry(5.33);
		assertEquals(53, encoded);
	}

	@Test
	void encodingSlowDecimalPointSpeedRoundsUp() {
		int encoded = Agent.prepareVelocityForLinkEntry(5.66);
		assertEquals(57, encoded);
	}

	@Test
	void fastSpeedIsIntegerWithFlatPlan() {
		int eventId = 0;
		int linkId = 0;
		double velocity = 15.2;
		int PceCategory = 0;
		long flatPlanEntry = Agent.prepareLinkEntry(eventId, linkId, velocity, PceCategory);
		double testedVelocity = Agent.getVelocityPlanEntry(flatPlanEntry);
		assertEquals(15.0, testedVelocity, 0.0);
	}

	@Test
	void smallSpeedHasOneDecimalPointAccuracyWithFlatPlan() {
		int eventId = 0;
		int linkId = 0;
		double velocity = 3.43;
		int PceCategory = 0;
		long flatPlanEntry = Agent.prepareLinkEntry(eventId, linkId, velocity, PceCategory);
		double testedVelocity = Agent.getVelocityPlanEntry(flatPlanEntry);
		assertEquals(3.4, testedVelocity, 0.0);
	}
}
