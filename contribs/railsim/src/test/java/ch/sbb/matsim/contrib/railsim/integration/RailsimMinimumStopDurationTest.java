package ch.sbb.matsim.contrib.railsim.integration;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.events.Event;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

public class RailsimMinimumStopDurationTest extends AbstractIntegrationTest {

	@Test
	void minimumStopDuration() {

		List<Event> events = runSimulation(new File(utils.getPackageInputDirectory(), "congested")).getEvents();

		Map<String, SequencedMap<String, StopTimeData>> stopTimes = processStopTimes(events);

		System.out.println(stopTimes);


	}
}
