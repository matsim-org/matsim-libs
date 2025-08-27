package ch.sbb.matsim.mobsim.qsim.pt;

import ch.sbb.matsim.config.SBBTransitConfigGroup;
import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import org.junit.jupiter.api.Assertions;
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
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Tests if chained departures are correctly simulated.
 */
public class SBBTransitChainedDepartureIntegrationTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	private Scenario createScenario() throws MalformedURLException {
		Config config = ConfigUtils.createConfig();
		config.setContext(new File("../../matsim/test/input/ch/sbb/matsim/routing/pt/raptor/").toURI().toURL());

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

		SBBTransitConfigGroup sbb = ConfigUtils.addOrGetModule(config, SBBTransitConfigGroup.class);
		sbb.setDeterministicServiceModes(Set.of("rail"));

		return ScenarioUtils.loadScenario(config);
	}

	private List<Event> runScenario(Consumer<Scenario> f) throws MalformedURLException {

		Scenario scenario = createScenario();

		if (f != null) {
			f.accept(scenario);
		}

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new SBBTransitModule());
		controler.configureQSimComponents(components -> {
			new SBBTransitEngineQSimModule().configure(components);
		});

		controler.run();

		Mobsim mobsim = controler.getInjector().getInstance(Mobsim.class);
		Assertions.assertNotNull(mobsim);
		Assertions.assertEquals(QSim.class, mobsim.getClass());

		QSim qsim = (QSim) mobsim;
		QSimComponentsConfig components = qsim.getChildInjector().getInstance(QSimComponentsConfig.class);
		Assertions.assertTrue(components.hasNamedComponent(SBBTransitEngineQSimModule.COMPONENT_NAME));

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
			80822, Id.createPersonId("person1"), Id.createLinkId("15m_8x41a_pt"), null,
			"home", new Coord(679623, 5811182)
		);

		ActivityStartEvent p2 = new ActivityStartEvent(
			71522, Id.createPersonId("person2"), Id.createLinkId("2f8_ip2w3_pt"), null,
			"home", new Coord(970034, 6017382)
		);

		assert events.contains(p1) : "person 1 did not arrive at specified time";
		assert events.contains(p2) : "person 2 did not arrive at specified time";

	}

	@Test
	void withChainedDepartures() throws Exception {

		List<Event> events = runScenario(scenario -> {
		});

		PersonContinuesInVehicleEvent c1 = new PersonContinuesInVehicleEvent(
			32580, Id.createPersonId("person1"),
			Id.createVehicleId("351761"), Id.createVehicleId("351714"),
			Id.create("f15", TransitStopFacility.class)

		);
		PersonContinuesInVehicleEvent c2 = new PersonContinuesInVehicleEvent(
			36720, Id.createPersonId("person2"),
			Id.createVehicleId("351871"), Id.createVehicleId("351874"),
			Id.create("f74", TransitStopFacility.class)
		);

		assert events.contains(c1) : "person 1 did not continue in vehicle";
		assert events.contains(c2) : "person 2 did not continue in vehicle";

		// This person is actual person faster, because of a different connection
		ActivityStartEvent p1 = new ActivityStartEvent(
			77222, Id.createPersonId("person1"), Id.createLinkId("15m_8x41a_pt"), null,
			"home", new Coord(679623, 5811182)
		);

		ActivityStartEvent p2 = new ActivityStartEvent(
			71522, Id.createPersonId("person2"), Id.createLinkId("2f8_ip2w3_pt"), null,
			"home", new Coord(970034, 6017382)
		);

		assert events.contains(p1) : "person 1 did not arrive at specified time";
		assert events.contains(p2) : "person 2 did not arrive at specified time";
	}

}
