package org.matsim.freight.carriers;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.freight.carriers.events.CarrierEventsReaders;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

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

}
