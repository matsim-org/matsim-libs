package org.matsim.contrib.emissions.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.utils.TestUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class RawEmissionEventsReaderTest {

    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void handleNonEventNode() {

        // read in an xml file which doesn't have events. It will parse the whole file but will not handle any of the
        // parsed nodes
        var networkUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "network.xml");
        new RawEmissionEventsReader((time, linkId, vehicleId, pollutant, value) -> {
            // this method should not be called
            fail();
        }).readFile(networkUrl.toString());

        // this test passes if the in the callback 'fail()' is not reached.
    }

	@Test
	void handleNonEmissionEvent() {

        // read in events file wihtout emission events. Those events should be ignored
        var eventsUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "output_events.xml.gz");
        new RawEmissionEventsReader((time, linkId, vehicleId, pollutant, value) -> {
            // this method should not be called
            fail();
        }).readFile(eventsUrl.toString());

        // this test passes if the in the callback 'fail()' is not reached.
    }

	@Test
	void handleColdEmissionEvent() {

        final double expectedValue = 10;
        final int expectedTime = 1;
        final Pollutant expectedPollutant = Pollutant.NOx;
        final int expectedEventsCount = 100;
        var network = TestUtils.createRandomNetwork(expectedEventsCount, 100, 100);
        var file = Paths.get(utils.getOutputDirectory()).resolve("emissions.events.xml.gz");
        TestUtils.writeColdEventsToFile(file, network, expectedPollutant, expectedValue, expectedTime, expectedTime);

        AtomicInteger counter = new AtomicInteger();
        new RawEmissionEventsReader((time, linkId, vehicleId, pollutant, value) -> {

            assertEquals(expectedTime, (int) time);
            assertEquals(expectedValue, value, 0.000001);
            assertTrue(network.getLinks().containsKey(Id.createLinkId(linkId)));
            assertEquals(expectedPollutant, pollutant);
            counter.getAndIncrement();

        }).readFile(file.toString());

        assertEquals(expectedEventsCount, counter.get());
    }

	@Test
	void handleWarmEmissionEvent() {

        final double expectedValue = 10;
        final int expectedTime = 1;
        final Pollutant expectedPollutant = Pollutant.NOx;
        final int expectedEventsCount = 100;
        var network = TestUtils.createRandomNetwork(expectedEventsCount, 100, 100);
        var file = Paths.get(utils.getOutputDirectory()).resolve("emissions.events.xml.gz");
        TestUtils.writeWarmEventsToFile(file, network, expectedPollutant, expectedValue, expectedTime, expectedTime);

        AtomicInteger counter = new AtomicInteger();
        new RawEmissionEventsReader((time, linkId, vehicleId, pollutant, value) -> {

            assertEquals(expectedTime, (int) time);
            assertEquals(expectedValue, value, 0.000001);
            assertTrue(network.getLinks().containsKey(Id.createLinkId(linkId)));
            assertEquals(expectedPollutant, pollutant);
            counter.getAndIncrement();

        }).readFile(file.toString());

        assertEquals(expectedEventsCount, counter.get());
    }
}
