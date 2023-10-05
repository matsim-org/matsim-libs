package org.matsim.contrib.freight.carrier;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.freight.events.CarrierEventsReaders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;

import static org.junit.Assert.assertEquals;

public class CarrierEventsReadersTest {

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testWriteReadServiceBasedEvents() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		MatsimEventsReader carrierEventsReaders = CarrierEventsReaders.createEventsReader(eventsManager);

		eventsManager.addHandler(collector);
		eventsManager.initProcessing();
		carrierEventsReaders.readFile(utils.getClassInputDirectory() + "serviceBasedEvents.xml");
		eventsManager.finishProcessing();

		//Hier müsste dann wohl "serviceBasedEventsWritten.xml" geschrieben werden und dann auch nochmal der eventsManager für die ausgeschriebene Datei durchlaufen werden

		assertEquals("number of events should be same", collector.getEvents().size(), collector.getEvents().size());
		MatsimTestUtils.assertEqualEventsFiles(utils.getClassInputDirectory() + "serviceBasedEvents.xml", utils.getClassInputDirectory() + "serviceBasedEventsWritten.xml");
		MatsimTestUtils.assertEqualFilesLineByLine(utils.getClassInputDirectory() + "serviceBasedEvents.xml", utils.getClassInputDirectory() + "serviceBasedEventsWritten.xml");
	}

	@Test
	public void testWriteReadShipmentBasedEvents() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		MatsimEventsReader carrierEventsReaders = CarrierEventsReaders.createEventsReader(eventsManager);

		eventsManager.addHandler(collector);
		eventsManager.initProcessing();
		carrierEventsReaders.readFile(utils.getClassInputDirectory() + "shipmentBasedEvents.xml");
		eventsManager.finishProcessing();

		assertEquals("number of events should be same", collector.getEvents().size(), collector.getEvents().size());
		MatsimTestUtils.assertEqualEventsFiles(utils.getClassInputDirectory() + "shipmentBasedEvents.xml", utils.getClassInputDirectory() + "shipmentBasedEventsWritten.xml");
		MatsimTestUtils.assertEqualFilesLineByLine(utils.getClassInputDirectory() + "shipmentBasedEvents.xml", utils.getClassInputDirectory() + "shipmentBasedEventsWritten.xml");
	}


}
