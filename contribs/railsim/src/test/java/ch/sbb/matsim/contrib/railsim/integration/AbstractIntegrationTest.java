package ch.sbb.matsim.contrib.railsim.integration;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;

import ch.sbb.matsim.contrib.railsim.RailsimModule;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;
import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimQSimModule;

abstract class AbstractIntegrationTest {

	@RegisterExtension
	protected final MatsimTestUtils utils = new MatsimTestUtils();

	protected SimulationResult runSimulation(File scenarioDir) {
		return runSimulation(scenarioDir, null);
	}

	protected SimulationResult runSimulation(File scenarioDir, Consumer<Scenario> f) {
		return runSimulation(scenarioDir, null, f);
	}

	protected SimulationResult runSimulation(File scenarioDir, Consumer<Config> c, Consumer<Scenario> f) {
		File configPath = new File(scenarioDir, "config.xml");

		Config config;
		if (!configPath.exists()) {
			// Load default config from resources
			config = ConfigUtils.loadConfig(getClass().getResource("/config.xml"));
			try {
				config.setContext(scenarioDir.toURI().toURL());
			} catch (Exception e) {
				throw new RuntimeException("Failed to set context for config", e);
			}
		} else {
			config = ConfigUtils.loadConfig(configPath.toString());
		}

		if (new File(scenarioDir, "population.xml").exists()) {
			config.plans().setInputFile("./population.xml");

			config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("home").setTypicalDuration(8 * 3600));
			config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("work").setTypicalDuration(8 * 3600));
		}

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setRunId(utils.getMethodName());
		config.controller().setDumpDataAtEnd(true);
		config.controller().setCreateGraphs(false);
		config.controller().setLastIteration(0);

		if (c != null) c.accept(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		if (f != null) f.accept(scenario);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new RailsimModule());
		controler.configureQSimComponents(components -> new RailsimQSimModule().configure(components));

		EventsCollector collector = new EventsCollector();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(collector);
			}
		});

		controler.run();

		return new SimulationResult(scenario, collector);
	}

	/**
	 * Convenience method to load network change events from the scenario directory.
	 */
	protected Consumer<Config> useEvents(String name) {
		return config -> {
			config.network().setTimeVariantNetwork(true);
			config.network().setChangeEventsInputFile(String.format("events_%s.xml", name));
		};
	}

	protected void modifyScheduleAndRunSimulation(File scenarioDir, int maxRndDelayStepSize, int maxMaxRndDelay) {
		Random rnd = MatsimRandom.getRandom();
		rnd.setSeed(1242);

		for (double maxRndDelaySeconds = 0; maxRndDelaySeconds <= maxMaxRndDelay; maxRndDelaySeconds = maxRndDelaySeconds + maxRndDelayStepSize) {
			File configPath = new File(scenarioDir, "config.xml");

			Config config;
			if (!configPath.exists()) {
				// Load default config from resources
				URL resourceUrl = getClass().getResource("/config.xml");
				if (resourceUrl == null) {
					throw new RuntimeException("Default config.xml not found in resources");
				}
				config = ConfigUtils.loadConfig(resourceUrl);
			} else {
				config = ConfigUtils.loadConfig(configPath.toString());
			}

			config.controller().setOutputDirectory(utils.getOutputDirectory() + "maxRndDelay_" + maxRndDelaySeconds);
			config.controller().setDumpDataAtEnd(true);
			config.controller().setCreateGraphs(false);
			config.controller().setLastIteration(0);

			Scenario scenario = ScenarioUtils.loadScenario(config);

			Controler controler = new Controler(scenario);
			controler.addOverridingModule(new RailsimModule());
			controler.configureQSimComponents(components -> new RailsimQSimModule().configure(components));

			// modify scenario
			for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
				for (TransitRoute route : line.getRoutes().values()) {
					List<Departure> departuresToRemove = new ArrayList<>();
					List<Departure> departuresToAdd = new ArrayList<>();
					for (Departure departure : route.getDepartures().values()) {
						departuresToRemove.add(departure);
						double departureTime = departure.getDepartureTime() + rnd.nextDouble() * maxRndDelaySeconds;
						Departure modifiedDeparture = scenario.getTransitSchedule().getFactory().createDeparture(departure.getId(), departureTime);
						modifiedDeparture.setVehicleId(departure.getVehicleId());
						departuresToAdd.add(modifiedDeparture);
					}

					for (Departure departureToRemove : departuresToRemove) {
						route.removeDeparture(departureToRemove);
					}
					for (Departure departureToAdd : departuresToAdd) {
						route.addDeparture(departureToAdd);
					}
				}
			}

			controler.run();
		}
	}

	protected double timeToAccelerate(double v0, double v, double a) {
		return (v - v0) / a;
	}

	protected double distanceTravelled(double v0, double a, double t) {
		return 0.5 * a * t * t + v0 * t;
	}

	protected double timeForDistance(double d, double v) {
		return d / v;
	}

	protected void assertTrainState(double time, double speed, double targetSpeed, double acceleration, double headPosition, List<RailsimTrainStateEvent> events) {

		RailsimTrainStateEvent prev = null;
		for (RailsimTrainStateEvent event : events) {

			if (event.getTime() > Math.ceil(time)) {
				Assertions.fail(
					String.format("No matching event found for time %f, speed %f pos %f, Closest event is%s", time, speed, headPosition, prev));
			}

			// If all assertions are true, returns successfully
			try {
				Assertions.assertEquals(Math.ceil(time), event.getTime(), 1e-7);
				Assertions.assertEquals(speed, event.getSpeed(), 1e-5);
				Assertions.assertEquals(targetSpeed, event.getTargetSpeed(), 1e-7);
				Assertions.assertEquals(acceleration, event.getAcceleration(), 1e-5);
				Assertions.assertEquals(headPosition, event.getHeadPosition(), 1e-5);
				return;
			} catch (AssertionError e) {
				// Check further events in loop
			}

			prev = event;
		}
	}

	protected List<RailsimTrainStateEvent> filterTrainEvents(List<Event> events, String train) {
		return events.stream().filter(event -> event instanceof RailsimTrainStateEvent).map(event -> (RailsimTrainStateEvent) event)
			.filter(event -> event.getVehicleId().toString().equals(train)).toList();
	}

	/**
	 * Create an assertion object for a map of stop times organized by train and station as returned.
	 */
	protected SimulationResultAssert assertThat(SimulationResult result) {
		return SimulationResultAssert.assertThat(result);
	}

}
