package org.matsim.core.mobsim.hermes;

import org.junit.Test;
import org.locationtech.jts.util.Assert;

public class AgentTest {

	@Test
	public void prepareLinkEntry() {
		for (int eventid = 1; eventid < HermesConfigGroup.MAX_EVENTS_AGENT; eventid *= 8) {
			for (int lid = 1; lid < HermesConfigGroup.MAX_LINK_ID; lid *= 8) {
				for (int j = 0; j < 255; j++) {
					for (int i = 0; i < 15; i++) {
						long flatplanentry = Agent.prepareLinkEntry(eventid, lid, j, i);
						int testedEventId = Agent.getPlanEvent(flatplanentry);
						int testedVelocity = Agent.getVelocityPlanEntry(flatplanentry);
						int testedLinkId = Agent.getLinkPlanEntry(flatplanentry);
						int testedPCECat = Agent.getLinkPCEEntry(flatplanentry);
						Assert.equals(j, testedVelocity);
						Assert.equals(eventid, testedEventId);
						Assert.equals(lid, testedLinkId);
						Assert.equals(i, testedPCECat);
					}
				}
			}
		}

	}
}