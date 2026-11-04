package org.matsim.freight.logistics.events;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourEndEventHandler;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class LspEventsReaderTest {

	private final List<Event> lspEvents = List.of(
			new HandlingInHubStartsEvent(110.0, Id.createLinkId("TestLink1"), Id.create("shipment1", LspShipment.class), Id.create("Hub1", LSPResource.class), 42.0),
			new HandlingInHubStartsEvent(142.0, Id.createLinkId("TestLink2"), Id.create("shipment2", LspShipment.class), Id.create("Hub2", LSPResource.class), 13.0),

			//Check if also some of the regular CarrierEvents get read correctly -> Load their mapping in the LspEventsReaderMapping via createCustomEventMappers() works
			new CarrierTourEndEvent(500, Id.create("c1", Carrier.class), Id.createLinkId("TestLinkC1"), Id.createVehicleId("myVehicle"), Id.create("myCarrierTour", Tour.class))
	);

	@Test
	public void testReader() {
		var outputStream = new ByteArrayOutputStream();
		EventWriterXML writer = new EventWriterXML(outputStream);
		lspEvents.forEach(writer::handleEvent);
		writer.closeFile();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		TestEventHandler handler = new TestEventHandler();
		eventsManager.addHandler(handler);
		eventsManager.initProcessing();
		LspEventsReader.createEventsReader(eventsManager)
				.readStream(new ByteArrayInputStream(outputStream.toByteArray()),
						ControllerConfigGroup.EventsFileFormat.xml);
		eventsManager.finishProcessing();

		Assertions.assertArrayEquals(lspEvents.toArray(), handler.handledEvents.toArray());
	}

	private static class TestEventHandler
			implements HandlingInHubStartedEventHandler, CarrierTourEndEventHandler {
		private final List<Event> handledEvents = new ArrayList<>();

		@Override public void handleEvent(HandlingInHubStartsEvent event) {
			handledEvents.add(event);
		}

		@Override public void handleEvent(CarrierTourEndEvent event) {
			handledEvents.add(event);
		}
	}
}
