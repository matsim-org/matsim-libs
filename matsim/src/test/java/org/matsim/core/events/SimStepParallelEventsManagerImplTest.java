
/* *********************************************************************** *
 * project: org.matsim.*
 * SimStepParallelEventsManagerImplTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.events;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.testcases.utils.EventsCollector;

 public class SimStepParallelEventsManagerImplTest {

	 @Test
	 void testEventHandlerCanProduceAdditionalEventLateInSimStep() {
		final SimStepParallelEventsManagerImpl events = new SimStepParallelEventsManagerImpl(8);
		events.addHandler(new LinkEnterEventHandler() {
			@Override
			public void handleEvent(LinkEnterEvent event) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				events.processEvent(new PersonStuckEvent(event.getTime(), Id.createPersonId(0), Id.createLinkId(0), "car"));
			}

			@Override
			public void reset(int iteration) {}
		});
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);
		events.initProcessing();
		events.processEvent(new LinkEnterEvent(0.0, Id.createVehicleId(0), Id.createLinkId(0)));
		events.processEvent(new LinkLeaveEvent(0.0, Id.createVehicleId(0), Id.createLinkId(0)));
		events.afterSimStep(0.0);
		events.processEvent(new LinkEnterEvent(1.0, Id.createVehicleId(0), Id.createLinkId(0)));
		events.processEvent(new LinkLeaveEvent(1.0, Id.createVehicleId(0), Id.createLinkId(0)));
		events.afterSimStep(1.0);
		events.finishProcessing();

		Assertions.assertThat(collector.getEvents()).contains(
					new LinkEnterEvent(0.0, Id.createVehicleId(0), Id.createLinkId(0)),
					new LinkLeaveEvent(0.0, Id.createVehicleId(0), Id.createLinkId(0)),
					new PersonStuckEvent(0.0, Id.createPersonId(0), Id.createLinkId(0), "car"),
					new LinkEnterEvent(1.0, Id.createVehicleId(0), Id.createLinkId(0)),
					new LinkLeaveEvent(1.0, Id.createVehicleId(0), Id.createLinkId(0)),
					new PersonStuckEvent(1.0, Id.createPersonId(0), Id.createLinkId(0), "car"));
	}

}
