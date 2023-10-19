package org.matsim.freight.carriers;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.freight.carriers.events.*;
import org.matsim.freight.carriers.events.eventhandler.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class CarrierEventsReadersTest {

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testWriteReadServiceBasedEvents() {
		EventsManager eventsManager1 = EventsUtils.createEventsManager();
		EventsManager eventsManager2 = EventsUtils.createEventsManager();
		EventsCollector collector1 = new EventsCollector();
		EventsCollector collector2 = new EventsCollector();

		eventsManager1.addHandler(collector1);
		eventsManager1.initProcessing();
		CarrierEventsReaders.createEventsReader(eventsManager1)
				.readFile(utils.getClassInputDirectory() + "serviceBasedEvents.xml");
		eventsManager1.finishProcessing();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		EventWriterXML writer = new EventWriterXML(outputStream);
		collector1.getEvents().forEach(writer::handleEvent);
		writer.closeFile();

		eventsManager2.addHandler(collector2);
		eventsManager2.initProcessing();
		CarrierEventsReaders.createEventsReader(eventsManager2)
				.readStream(new ByteArrayInputStream(outputStream.toByteArray()), ControllerConfigGroup.EventsFileFormat.xml);
		eventsManager2.finishProcessing();

		Assert.assertEquals(collector1.getEvents(), collector2.getEvents());
	}


	@Test
	public void testReadServiceBasedEvents() {

		EventsManager eventsManager = EventsUtils.createEventsManager();
		TestEventHandlerTours eventHandlerTours = new TestEventHandlerTours();
		TestEventHandlerServices eventHandlerServices = new TestEventHandlerServices();

		eventsManager.addHandler(eventHandlerTours);
		eventsManager.addHandler(eventHandlerServices);
		eventsManager.initProcessing();
		CarrierEventsReaders.createEventsReader(eventsManager)
				.readFile(utils.getClassInputDirectory() + "serviceBasedEvents.xml");
		eventsManager.finishProcessing();

		Assert.assertEquals("Number of tour related carrier events is not correct", 4 , eventHandlerTours.handledEvents.size());
		Assert.assertEquals("Number of service related carrier events is not correct", 14 , eventHandlerServices.handledEvents.size());
	}

	@Test
	public void testWriteReadShipmentBasedEvents() {
		EventsManager eventsManager1 = EventsUtils.createEventsManager();
		EventsManager eventsManager2 = EventsUtils.createEventsManager();
		EventsCollector collector1 = new EventsCollector();
		EventsCollector collector2 = new EventsCollector();

		eventsManager1.addHandler(collector1);
		eventsManager1.initProcessing();
		CarrierEventsReaders.createEventsReader(eventsManager1)
			.readFile(utils.getClassInputDirectory() + "shipmentBasedEvents.xml");
		eventsManager1.finishProcessing();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		EventWriterXML writer = new EventWriterXML(outputStream);
		collector1.getEvents().forEach(writer::handleEvent);
		writer.closeFile();

		eventsManager2.addHandler(collector2);
		eventsManager2.initProcessing();
		CarrierEventsReaders.createEventsReader(eventsManager2)
			.readStream(new ByteArrayInputStream(outputStream.toByteArray()), ControllerConfigGroup.EventsFileFormat.xml);
		eventsManager2.finishProcessing();

		Assert.assertEquals(collector1.getEvents(), collector2.getEvents());
	}

	@Test
	public void testReadShipmentBasedEvents() {

		EventsManager eventsManager = EventsUtils.createEventsManager();
		TestEventHandlerTours eventHandlerTours = new TestEventHandlerTours();
		TestEventHandlerShipments testEventHandlerShipments = new TestEventHandlerShipments();

		eventsManager.addHandler(eventHandlerTours);
		eventsManager.addHandler(testEventHandlerShipments);
		eventsManager.initProcessing();
		CarrierEventsReaders.createEventsReader(eventsManager)
			.readFile(utils.getClassInputDirectory() + "shipmentBasedEvents.xml");
		eventsManager.finishProcessing();

		Assert.assertEquals("Number of tour related carrier events is not correct", 2 , eventHandlerTours.handledEvents.size());
		Assert.assertEquals("Number of shipments related carrier events is not correct", 20 , testEventHandlerShipments.handledEvents.size());
	}

	private static class TestEventHandlerTours
		implements CarrierTourStartEventHandler, CarrierTourEndEventHandler {
		private final List<Event> handledEvents = new ArrayList<>();


		@Override public void handleEvent(CarrierTourEndEvent event) {
			handledEvents.add(event);
		}

		@Override public void handleEvent(CarrierTourStartEvent event) {
			handledEvents.add(event);
		}
	}

	private static class TestEventHandlerServices
		implements CarrierServiceStartEventHandler, CarrierServiceEndEventHandler {
		private final List<Event> handledEvents = new ArrayList<>();

		@Override public void handleEvent(CarrierServiceEndEvent event) {
			handledEvents.add(event);
		}

		@Override public void handleEvent(CarrierServiceStartEvent event) {
			handledEvents.add(event);
		}

	}

	private static class TestEventHandlerShipments
		implements CarrierShipmentDeliveryStartEventHandler, CarrierShipmentDeliveryEndEventHandler, CarrierShipmentPickupStartEventHandler, CarrierShipmentPickupEndEventHandler {
		private final List<Event> handledEvents = new ArrayList<>();


		@Override public void handleEvent(CarrierShipmentDeliveryEndEvent event) {
			handledEvents.add(event);
		}

		@Override public void handleEvent(CarrierShipmentPickupEndEvent event) {
			handledEvents.add(event);
		}

		@Override public void handleEvent(CarrierShipmentDeliveryStartEvent event) {
			handledEvents.add(event);
		}

		@Override public void handleEvent(CarrierShipmentPickupStartEvent event) {
			handledEvents.add(event);
		}
	}
}
