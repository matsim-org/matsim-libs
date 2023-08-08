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
		carrierEventsReaders.readFile(utils.getClassInputDirectory() + "serviceBasedEventsFile.xml");
		eventsManager.finishProcessing();

		assertEquals("number of events should be same", collector.getEvents().size(), collector.getEvents().size());
		MatsimTestUtils.assertEqualEventsFiles(utils.getClassInputDirectory() + "serviceBasedEventsFile.xml", utils.getClassInputDirectory() + "serviceBasedEventsFileWritten.xml");
		MatsimTestUtils.assertEqualFilesLineByLine(utils.getClassInputDirectory() + "serviceBasedEventsFile.xml", utils.getClassInputDirectory() + "serviceBasedEventsFileWritten.xml");
	}

	@Test
	public void testWriteReadShipmentBasedEvents() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		MatsimEventsReader carrierEventsReaders = CarrierEventsReaders.createEventsReader(eventsManager);

		eventsManager.addHandler(collector);
		eventsManager.initProcessing();
		carrierEventsReaders.readFile(utils.getClassInputDirectory() + "shipmentBasedEventsFile.xml");
		eventsManager.finishProcessing();

		assertEquals("number of events should be same", collector.getEvents().size(), collector.getEvents().size());
		MatsimTestUtils.assertEqualEventsFiles(utils.getClassInputDirectory() + "shipmentBasedEventsFile.xml", utils.getClassInputDirectory() + "shipmentBasedEventsFileWritten.xml");
		MatsimTestUtils.assertEqualFilesLineByLine(utils.getClassInputDirectory() + "shipmentBasedEventsFile.xml", utils.getClassInputDirectory() + "shipmentBasedEventsFileWritten.xml");
	}


}
