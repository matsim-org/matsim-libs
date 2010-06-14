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

package playground.mrieser.core.sim.impl;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;

import playground.mrieser.core.sim.api.DepartureHandler;
import playground.mrieser.core.sim.api.NewSimEngine;

public class LegHandlerTest {

	@Test
	public void testSetGetDepartureHandlers() {
		LegHandler legHandler = new LegHandler(new SimTestEngine());

		CountingDepartureHandler carHandler = new CountingDepartureHandler();
		CountingDepartureHandler transitHandler = new CountingDepartureHandler();
		CountingDepartureHandler otherHandler = new CountingDepartureHandler();

		Assert.assertNull(legHandler.getDepartureHandler(TransportMode.car));
		Assert.assertNull(legHandler.getDepartureHandler(TransportMode.pt));
		Assert.assertNull(legHandler.getDepartureHandler(TransportMode.walk));
		Assert.assertNull(legHandler.getDepartureHandler(TransportMode.bike));

		legHandler.setDepartureHandler(TransportMode.car, carHandler);
		legHandler.setDepartureHandler(TransportMode.pt, transitHandler);
		legHandler.setDepartureHandler(TransportMode.walk, otherHandler);
		legHandler.setDepartureHandler(TransportMode.bike, otherHandler);

		Assert.assertEquals(carHandler, legHandler.getDepartureHandler(TransportMode.car));
		Assert.assertEquals(transitHandler, legHandler.getDepartureHandler(TransportMode.pt));
		Assert.assertEquals(otherHandler, legHandler.getDepartureHandler(TransportMode.walk));
		Assert.assertEquals(otherHandler, legHandler.getDepartureHandler(TransportMode.bike));
		Assert.assertNull(legHandler.getDepartureHandler(TransportMode.train));
	}

	private class SimTestEngine implements NewSimEngine {

		private final EventsManager em = new EventsManagerImpl();
		private double time;
		public int countHandleNextPlanElement = 0;

		@Override
		public double getCurrentTime() {
			return this.time;
		}

		@Override
		public EventsManager getEventsManager() {
			return this.em;
		}

		@Override
		public void handleNextPlanElement(final Plan plan) {
			this.countHandleNextPlanElement++;
		}

		@Override
		public void doSimStep(final double time) {
			this.time = time;
		}
	}

	private class CountingDepartureHandler implements DepartureHandler {

		public int count = 0;

		@Override
		public void handleDeparture(Leg leg, Plan plan) {
			this.count++;
		}

	}
}
