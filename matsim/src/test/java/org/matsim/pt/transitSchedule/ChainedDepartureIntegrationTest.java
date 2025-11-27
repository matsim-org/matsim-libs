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
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.VehicleType;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests if chained departures are correctly simulated.
 */
public class ChainedDepartureIntegrationTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	private Scenario createScenario() throws MalformedURLException {
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
		config.controller().setWritePlansInterval(1);
		config.scoring().setWriteExperiencedPlans(true);
		config.qsim().setEndTime(Time.parseTime("36:00:00"));

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("home").setTypicalDuration(8 * 3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("work").setTypicalDuration(8 * 3600));

		return ScenarioUtils.loadScenario(config);
	}

	private List<Event> runScenario(Consumer<Scenario> f) throws MalformedURLException {

		Scenario scenario = createScenario();

		if (f != null) {
			f.accept(scenario);
		}

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
			76961, Id.createPersonId("person1"), Id.createLinkId("15m_8x41a_pt"), null,
			"home", new Coord(679623, 5811182)
		);

		ActivityStartEvent p2 = new ActivityStartEvent(
			71213, Id.createPersonId("person2"), Id.createLinkId("2f8_ip2w3_pt"), null,
			"home", new Coord(970034, 6017382)
		);

		assertThat(events)
			.contains(p1)
			.contains(p2);
	}

	@Test
	void withChainedDepartures() throws Exception {

		List<Event> events = runScenario(scenario -> {});

		ActivityStartEvent p1 = new ActivityStartEvent(
			76902, Id.createPersonId("person1"), Id.createLinkId("15m_8x41a_pt"), null,
			"home", new Coord(679623, 5811182)
		);

		ActivityStartEvent p2 = new ActivityStartEvent(
			71213, Id.createPersonId("person2"), Id.createLinkId("2f8_ip2w3_pt"), null,
			"home", new Coord(970034, 6017382)
		);

		// Same arrival times as without chains, but different events
		assertThat(events)
			.contains(new PersonContinuesInVehicleEvent(
				32581, Id.createPersonId("person1"),
				Id.createVehicleId("351761"), Id.createVehicleId("351714"),
				Id.create("f15", TransitStopFacility.class)
			))
			.contains(new PersonContinuesInVehicleEvent(
				36362, Id.createPersonId("person2"),
				Id.createVehicleId("351871"), Id.createVehicleId("351874"),
				Id.create("f74", TransitStopFacility.class)
			))
			.contains(p1)
			.contains(p2);

	}

	@Test
	void withDelays() throws Exception {

		// Reduce the maximum speed of the vehicle type to 20 m/s
		Consumer<Scenario> reduceSpeed = scenario -> {
			Map<Id<VehicleType>, VehicleType> vehicleTypes = scenario.getTransitVehicles().getVehicleTypes();
			vehicleTypes.get(Id.create("pt", VehicleType.class)).setMaximumVelocity(20);
		};

		List<Event> events = runScenario(reduceSpeed);

		ActivityStartEvent p1 = new ActivityStartEvent(
			83744, Id.createPersonId("person1"), Id.createLinkId("15m_8x41a_pt"), null,
			"home", new Coord(679623, 5811182)
		);

		ActivityStartEvent p2 = new ActivityStartEvent(
			72797, Id.createPersonId("person2"), Id.createLinkId("2f8_ip2w3_pt"), null,
			"home", new Coord(970034, 6017382)
		);

		assertThat(events)
			.contains(p1)
			.contains(p2);

	}

}
