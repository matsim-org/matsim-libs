package ch.sbb.matsim.contrib.railsim.integration;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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

	protected EventsCollector runSimulation(File scenarioDir) {
		return runSimulation(scenarioDir, null);
	}

	protected EventsCollector runSimulation(File scenarioDir, Consumer<Scenario> f) {
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

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setDumpDataAtEnd(true);
		config.controller().setCreateGraphs(false);
		config.controller().setLastIteration(0);

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

		return collector;
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

	protected List<RailsimTrainStateEvent> filterTrainEvents(EventsCollector collector, String train) {
		return collector.getEvents().stream().filter(event -> event instanceof RailsimTrainStateEvent).map(event -> (RailsimTrainStateEvent) event)
			.filter(event -> event.getVehicleId().toString().equals(train)).toList();
	}

	/**
	 * Processes events to extract stop time information for each train and facility combination.
	 *
	 * @param events List of all simulation events
	 * @return Map of train ID -> facility ID -> stop time data
	 */
	protected Map<String, SequencedMap<String, StopTimeData>> processStopTimes(List<Event> events) {
		Map<String, SequencedMap<String, StopTimeData>> stopTimesByTrainAndFacility = new LinkedHashMap<>();

		for (Event event : events) {
			if (event instanceof VehicleArrivesAtFacilityEvent arrivalEvent) {
				String trainId = arrivalEvent.getVehicleId().toString();
				String facilityId = arrivalEvent.getFacilityId().toString();

				SequencedMap<String, StopTimeData> stops = stopTimesByTrainAndFacility
					.computeIfAbsent(trainId, k -> new LinkedHashMap<>());

				stops.computeIfAbsent(facilityId, k -> new StopTimeData(stops.lastEntry()))
					.arrivalTime = arrivalEvent.getTime();

			} else if (event instanceof VehicleDepartsAtFacilityEvent departureEvent) {
				String trainId = departureEvent.getVehicleId().toString();
				String facilityId = departureEvent.getFacilityId().toString();

				SequencedMap<String, StopTimeData> stops = stopTimesByTrainAndFacility
					.computeIfAbsent(trainId, k -> new LinkedHashMap<>());

				stops.computeIfAbsent(facilityId, k -> new StopTimeData(stops.lastEntry()))
					.departureTime = departureEvent.getTime();
			}
		}

		return stopTimesByTrainAndFacility;
	}

	/**
	 * Data class to hold arrival and departure times for a train at a specific facility.
	 */
	protected final static class StopTimeData {

		double arrivalTime = -1.0;
		double departureTime = -1.0;

		/**
		 * Previous stop time data, if any. This can be used to chain stop times together.
		 */
		private final StopTimeData prev;

		public StopTimeData(Map.Entry<String, StopTimeData> prev) {
			this.prev = prev != null ? prev.getValue() : null;
		}

		/**
		 * @return true if both arrival and departure times are set
		 */
		boolean isComplete() {
			return arrivalTime >= 0 && departureTime >= 0;
		}

		/**
		 * @return true if the train has arrived at the facility.
		 */
		boolean hasArrived() {
			return arrivalTime >= 0;
		}

		/**
		 * @return the stop duration in seconds, or -1 if incomplete
		 */
		double getStopDuration() {
			return isComplete() ? departureTime - arrivalTime : -1.0;
		}

		/**
		 * Calculates the travel time from the previous stop to this one.
		 */
		double getTravelTime() {
			return hasArrived() && prev != null ? arrivalTime - prev.departureTime : -1.0;
		}

		@Override
		public String toString() {
			return "StopTimeData{" +
				"arrivalTime=" + arrivalTime +
				", departureTime=" + departureTime +
				", travelTime=" + getTravelTime() +
				", stopDuration=" + getStopDuration() +
				'}';
		}
	}

}
