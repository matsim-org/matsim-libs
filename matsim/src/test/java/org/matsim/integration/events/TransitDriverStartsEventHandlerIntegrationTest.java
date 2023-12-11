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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;

/**
 * @author mrieser
 */
public class TransitDriverStartsEventHandlerIntegrationTest {

	@Test
	void testProcessEventIntegration() {
		EventsManager em = EventsUtils.createEventsManager();
		TransitDriverStartsEvent e1 = new TransitDriverStartsEvent(12345, Id.create("driver", Person.class),
				Id.create("veh", Vehicle.class), Id.create("line", TransitLine.class), Id.create("route", TransitRoute.class), Id.create("dep", Departure.class));
		TransitDriverStartsTestEventHandler eh = new TransitDriverStartsTestEventHandler();
		em.addHandler(eh);

		Assertions.assertEquals(0, eh.events.size());

		em.initProcessing();
		em.processEvent(e1);
		em.finishProcessing();

		Assertions.assertEquals(1, eh.events.size());
		Assertions.assertEquals(e1, eh.events.get(0));
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
