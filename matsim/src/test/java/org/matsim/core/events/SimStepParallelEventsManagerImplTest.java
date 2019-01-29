package org.matsim.core.events;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.testcases.utils.EventsCollector;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;

public class SimStepParallelEventsManagerImplTest {

	@Test
	public void testEventHandlerCanProduceAdditionalEventLateInSimStep() {
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

		assertThat(collector.getEvents(),
			contains(
					new LinkEnterEvent(0.0, Id.createVehicleId(0), Id.createLinkId(0)),
					new LinkLeaveEvent(0.0, Id.createVehicleId(0), Id.createLinkId(0)),
					new PersonStuckEvent(0.0, Id.createPersonId(0), Id.createLinkId(0), "car"),
					new LinkEnterEvent(1.0, Id.createVehicleId(0), Id.createLinkId(0)),
					new LinkLeaveEvent(1.0, Id.createVehicleId(0), Id.createLinkId(0)),
					new PersonStuckEvent(1.0, Id.createPersonId(0), Id.createLinkId(0), "car")));
	}

}
