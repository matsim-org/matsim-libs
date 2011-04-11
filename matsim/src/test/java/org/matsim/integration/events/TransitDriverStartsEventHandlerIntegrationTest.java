/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.integration.events;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;

/**
 * @author mrieser
 */
public class TransitDriverStartsEventHandlerIntegrationTest {

	@Test
	public void testProcessEventIntegration() {
		EventsManager em = (EventsManager) EventsUtils.createEventsManager();
		TransitDriverStartsEvent e1 = new TransitDriverStartsEvent(12345, new IdImpl("driver"),
				new IdImpl("veh"), new IdImpl("line"), new IdImpl("route"), new IdImpl("dep"));
		TransitDriverStartsTestEventHandler eh = new TransitDriverStartsTestEventHandler();
		em.addHandler(eh);

		Assert.assertEquals(0, eh.events.size());

		em.processEvent(e1);

		Assert.assertEquals(1, eh.events.size());
		Assert.assertEquals(e1, eh.events.get(0));
	}

	/*package*/ static class TransitDriverStartsTestEventHandler implements TransitDriverStartsEventHandler {
		/*package*/ List<TransitDriverStartsEvent> events = new ArrayList<TransitDriverStartsEvent>(3);
		@Override
		public void reset(int iteration) {
			this.events.clear();
		}
		@Override
		public void handleEvent(TransitDriverStartsEvent event) {
			this.events.add(event);
		}
	}
}
