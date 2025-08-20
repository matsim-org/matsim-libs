package org.matsim.pt.transitSchedule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonContinuesInVehicleEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests if chained departures are correctly simulated.
 */
public class ChainedDepartureIntegrationTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	private List<Event> runScenario(Consumer<Scenario> f) throws MalformedURLException {
		Config config = ConfigUtils.createConfig();
		config.setContext(new File("test/input/ch/sbb/matsim/routing/pt/raptor/").toURI().toURL());

		config.network().setInputFile("network.xml");
		config.transit().setTransitScheduleFile("schedule.xml");
		config.transit().setVehiclesFile("vehicles.xml");
		config.transit().setUseTransit(true);
		config.plans().setInputFile("population.xml");
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		config.controller().setCompressionType(ControllerConfigGroup.CompressionType.none);
		config.qsim().setEndTime(Time.parseTime("36:00:00"));

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("home").setTypicalDuration(8 * 3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("work").setTypicalDuration(8 * 3600));

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		controler.run();

		EventsManager m = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		m.addHandler(collector);

		EventsUtils.readEvents(m, utils.getOutputDirectory() + "output_events.xml");

		return collector.getEvents();
	}

	@Test
	void withoutChainedDepartures() throws Exception {

		// Remove all chained departures
		Consumer<Scenario> removeChainedDepartures = scenario -> {
			for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
				for (TransitRoute route : line.getRoutes().values()) {
					for (Departure d : route.getDepartures().values()) {
						d.setChainedDepartures(List.of());
					}
				}
			}
		};

		List<Event> events = runScenario(removeChainedDepartures);

		ActivityStartEvent p1 = new ActivityStartEvent(
			77899, Id.createPersonId("person1"), Id.createLinkId("15m_8x41a_pt"), null,
			"home", new Coord(679623, 5811182)
		);

		ActivityStartEvent p2 = new ActivityStartEvent(
			71722, Id.createPersonId("person2"), Id.createLinkId("2f8_ip2w3_pt"), null,
			"home", new Coord(970034, 6017382)
		);

		assertThat(events)
			.contains(p1)
			.contains(p2);

	}

	@Test
	void withChainedDepartures() throws Exception {

		List<Event> events = runScenario(scenario -> {
		});

		ActivityStartEvent p1 = new ActivityStartEvent(
			77899, Id.createPersonId("person1"), Id.createLinkId("15m_8x41a_pt"), null,
			"home", new Coord(679623, 5811182)
		);

		ActivityStartEvent p2 = new ActivityStartEvent(
			71722, Id.createPersonId("person2"), Id.createLinkId("2f8_ip2w3_pt"), null,
			"home", new Coord(970034, 6017382)
		);

		// Same arrival times as without chains, but different events
		assertThat(events)
			.contains(new PersonContinuesInVehicleEvent(
				33592, Id.createPersonId("person1"),
				Id.createVehicleId("351761"), Id.createVehicleId("351714")
			))
			.contains(new PersonContinuesInVehicleEvent(
				36727, Id.createPersonId("person2"),
				Id.createVehicleId("351871"), Id.createVehicleId("351874")
			))
			.contains(p1)
			.contains(p2);

	}

	@Test
	void withDelays() throws Exception {

		// TODO: test with introduced delays, persons should arrive late as well

	}

}
