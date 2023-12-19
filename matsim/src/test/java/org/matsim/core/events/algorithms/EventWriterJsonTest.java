package org.matsim.core.events.algorithms;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @author mrieser / Simunto GmbH
 */
public class EventWriterJsonTest {

	/**
	 * Some people use the ids as names, including special characters in there... so make sure attribute
	 * values are correctly encoded when written to a file.
	 */
	@Test
	void testSpecialCharacters() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		EventWriterJson writer = new EventWriterJson(baos);

		writer.handleEvent(new LinkLeaveEvent(3600.0, Id.create("vehicle>3", Vehicle.class), Id.create("link<2", Link.class)));
		writer.handleEvent(new LinkLeaveEvent(3601.0, Id.create("vehicle\"4", Vehicle.class), Id.create("link'3", Link.class)));
		writer.closeFile();

		ByteArrayInputStream bios = new ByteArrayInputStream(baos.toByteArray());

		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);
		events.initProcessing();
		new MatsimEventsReader(events).readStream(bios, ControllerConfigGroup.EventsFileFormat.json);
		events.finishProcessing();

		Assertions.assertEquals(2, collector.getEvents().size(), "there must be 2 events.");
		LinkLeaveEvent event1 = (LinkLeaveEvent) collector.getEvents().get(0);
		LinkLeaveEvent event2 = (LinkLeaveEvent) collector.getEvents().get(1);

		Assertions.assertEquals("link<2", event1.getLinkId().toString());
		Assertions.assertEquals("vehicle>3", event1.getVehicleId().toString());

		Assertions.assertEquals("link'3", event2.getLinkId().toString());
		Assertions.assertEquals("vehicle\"4", event2.getVehicleId().toString());
	}

	@Test
	void testNullAttribute() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		EventWriterJson writer = new EventWriterJson(baos);

		GenericEvent event = new GenericEvent("TEST", 3600.0);
		event.getAttributes().put("dummy", null);
		writer.handleEvent(event);
		writer.closeFile();

		System.out.println(new String(baos.toByteArray()));

		ByteArrayInputStream bios = new ByteArrayInputStream(baos.toByteArray());

		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);
		events.initProcessing();
		new MatsimEventsReader(events).readStream(bios, ControllerConfigGroup.EventsFileFormat.json);
		events.finishProcessing();

		Assertions.assertEquals(1, collector.getEvents().size(), "there must be 1 event.");

		GenericEvent event1 = (GenericEvent) collector.getEvents().get(0);
		Assertions.assertTrue(event1.getAttributes().containsKey("dummy"));
		Assertions.assertNull(event1.getAttributes().get("dummy"));
	}
}
