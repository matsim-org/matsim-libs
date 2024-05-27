/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package org.matsim.freight.carriers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.freight.carriers.events.*;
import org.matsim.freight.carriers.events.eventhandler.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Kai Martins-Turner (kturner)
 * @author Niclas Richter (nixlaos)
 */
public class CarrierEventsReadersTest {


	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private final Id<Link> linkId = Id.createLinkId("demoLink");
	private final Id<Link> linkId2 = Id.createLinkId("demoLink2");
	private final Id<Carrier> carrierId = Id.create("testCarrier", Carrier.class);
	private final Id<Vehicle> vehicleId = Id.createVehicleId("myVehicle");

	private final Id<Tour> tourId = Id.create("myCarrierTour", Tour.class);
	private final CarrierService service = CarrierService.Builder.newInstance(Id.create("service42", CarrierService.class), linkId2 ).build();
	private final CarrierShipment shipment = CarrierShipment.Builder.newInstance(Id.create("shipment11", CarrierShipment.class), linkId, linkId2,7 ).build();

	private final List<Event> carrierEvents = List.of(
		new CarrierTourStartEvent(10, carrierId, linkId, vehicleId, tourId),
		new CarrierTourEndEvent(500, carrierId, linkId, vehicleId, tourId),
		new CarrierServiceStartEvent(20, carrierId, service, vehicleId),
		new CarrierServiceEndEvent(25, carrierId, service, vehicleId),
		new CarrierShipmentPickupStartEvent(100, carrierId, shipment, vehicleId),
		new CarrierShipmentPickupEndEvent(115, carrierId, shipment, vehicleId),
		new CarrierShipmentDeliveryStartEvent(210, carrierId, shipment, vehicleId),
		new CarrierShipmentDeliveryEndEvent(225, carrierId, shipment, vehicleId)
	);

	@Test
	void testWriteReadServiceBasedEvents() {
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

		Assertions.assertEquals(collector1.getEvents(), collector2.getEvents());
	}


	@Test
	void testReadServiceBasedEvents() {

		EventsManager eventsManager = EventsUtils.createEventsManager();
		TestEventHandlerTours eventHandlerTours = new TestEventHandlerTours();
		TestEventHandlerServices eventHandlerServices = new TestEventHandlerServices();

		eventsManager.addHandler(eventHandlerTours);
		eventsManager.addHandler(eventHandlerServices);
		eventsManager.initProcessing();
		CarrierEventsReaders.createEventsReader(eventsManager)
				.readFile(utils.getClassInputDirectory() + "serviceBasedEvents.xml");
		eventsManager.finishProcessing();

		Assertions.assertEquals(4 , eventHandlerTours.handledEvents.size(), "Number of tour related carrier events is not correct");
		Assertions.assertEquals(14 , eventHandlerServices.handledEvents.size(), "Number of service related carrier events is not correct");
	}

	@Test
	void testWriteReadShipmentBasedEvents() {
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

		Assertions.assertEquals(collector1.getEvents(), collector2.getEvents());
	}

	@Test
	void testReadShipmentBasedEvents() {

		EventsManager eventsManager = EventsUtils.createEventsManager();
		TestEventHandlerTours eventHandlerTours = new TestEventHandlerTours();
		TestEventHandlerShipments testEventHandlerShipments = new TestEventHandlerShipments();

		eventsManager.addHandler(eventHandlerTours);
		eventsManager.addHandler(testEventHandlerShipments);
		eventsManager.initProcessing();
		CarrierEventsReaders.createEventsReader(eventsManager)
			.readFile(utils.getClassInputDirectory() + "shipmentBasedEvents.xml");
		eventsManager.finishProcessing();

		Assertions.assertEquals(2 , eventHandlerTours.handledEvents.size(), "Number of tour related carrier events is not correct");
		Assertions.assertEquals(20 , testEventHandlerShipments.handledEvents.size(), "Number of shipments related carrier events is not correct");
	}


	/**
	 * This test is testing the reader with some locally created events (see above).
	 * This test is inspired by the DrtEventsReaderTest from michalm.
	 */
	@Test
	void testReader() {
		var outputStream = new ByteArrayOutputStream();
		EventWriterXML writer = new EventWriterXML(outputStream);
		carrierEvents.forEach(writer::handleEvent);
		writer.closeFile();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		TestEventHandlerTours eventHandlerTours = new TestEventHandlerTours();
		TestEventHandlerServices eventHandlerServices = new TestEventHandlerServices();
		TestEventHandlerShipments eventHandlerShipments = new TestEventHandlerShipments();

		eventsManager.addHandler(eventHandlerTours);
		eventsManager.addHandler(eventHandlerServices);
		eventsManager.addHandler(eventHandlerShipments);

		eventsManager.initProcessing();
		CarrierEventsReaders.createEventsReader(eventsManager)
			.readStream(new ByteArrayInputStream(outputStream.toByteArray()),
				ControllerConfigGroup.EventsFileFormat.xml);
		eventsManager.finishProcessing();

		var handledEvents = new ArrayList<Event>();
		handledEvents.addAll(eventHandlerTours.handledEvents);
		handledEvents.addAll(eventHandlerServices.handledEvents);
		handledEvents.addAll(eventHandlerShipments.handledEvents);

		//Please note: This test is sensitive to the order of events as they are added in carrierEvents (input) and the resukts of the handler...
		Assertions.assertArrayEquals(carrierEvents.toArray(), handledEvents.toArray());
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
