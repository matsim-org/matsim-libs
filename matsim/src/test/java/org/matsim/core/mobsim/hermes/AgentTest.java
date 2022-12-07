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
}
