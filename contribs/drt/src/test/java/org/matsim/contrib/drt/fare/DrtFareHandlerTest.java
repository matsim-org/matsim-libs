/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.fare;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.core.events.ParallelEventsManager;

import java.util.List;

/**
 * @author jbischoff
 */
public class DrtFareHandlerTest {

	/**
	 * Test method for {@link DrtFareHandler}.
	 */
	@Test
	public void testDrtFareHandler() {
		String mode = "mode_0";
		DrtFareParams fareParams = new DrtFareParams();
		fareParams.baseFare = 1;
		fareParams.minFarePerTrip = 1.5;
		fareParams.dailySubscriptionFee = 1;
		fareParams.distanceFare_m = 1.0 / 1000.0;
		fareParams.timeFare_h = 15;

		ParallelEventsManager events = new ParallelEventsManager(false);
		events.addHandler(new DrtFareHandler(mode, fareParams, events));

		final MutableDouble fare = new MutableDouble(0);
		events.addHandler(new PersonMoneyEventHandler() {
			@Override
			public void handleEvent(PersonMoneyEvent event) {
				fare.add(event.getAmount());
			}

			@Override
			public void reset(int iteration) {
			}
		});

		events.initProcessing();

		var personId = Id.createPersonId("p1");
		{
			var requestId = Id.create(0, Request.class);
			events.processEvent(new DrtRequestSubmittedEvent(0.0, mode, requestId, List.of(personId), Id.createLinkId("12"),
					Id.createLinkId("23"), 240, 1000, 0.0, 0.0, 0.0));
			events.processEvent(new PassengerDroppedOffEvent(300.0, mode, requestId, personId, null));
			events.flush();

			//fare: 1 (daily fee) + 1 (distance()+ 1 basefare + 1 (time)
			Assert.assertEquals(-4.0, fare.getValue(), 0);
		}
		{
			// test minFarePerTrip
			var requestId = Id.create(1, Request.class);
			events.processEvent(new DrtRequestSubmittedEvent(0.0, mode, requestId, List.of(personId), Id.createLinkId("45"),
					Id.createLinkId("56"), 24, 100, 0.0, 0.0, 0.0));
			events.processEvent(new PassengerDroppedOffEvent(300.0, mode, requestId, personId, null));
			events.finishProcessing();

			/*
			 * fare new trip: 0 (daily fee already paid) + 0.1 (distance)+ 1 basefare + 0.1 (time) = 1.2 < minFarePerTrip = 1.5
			 * --> new total fare: 4 (previous trip) + 1.5 (minFarePerTrip for new trip) = 5.5
			 */
			Assert.assertEquals(-5.5, fare.getValue(), 0);
		}
	}

}
