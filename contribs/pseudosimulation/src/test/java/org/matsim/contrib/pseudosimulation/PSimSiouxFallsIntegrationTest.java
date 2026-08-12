package org.matsim.contrib.pseudosimulation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.NoTransitEmulator;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitEmulator;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitPerformanceFromPSimSpecificImplementation;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.testcases.MatsimTestUtils;

/**
 * End-to-end cover for PSim on a scenario that simulates both car traffic and transit on the
 * network. It pins the behaviour that a plain {@link RunPSim} has to provide on its own, without
 * the caller supplying any binding:
 * <ul>
 * <li>the injector can be built at all;</li>
 * <li>transit legs are emulated from recorded QSim performance rather than taking no time;</li>
 * <li>iterations alternate between the queue simulation and the pseudo-simulation;</li>
 * <li>the run survives the core analysis listeners, which see events for replanned agents only.</li>
 * </ul>
 */
public class PSimSiouxFallsIntegrationTest {

	private static final double POPULATION_SAMPLE = 0.05;
	private static final int ITERATIONS_PER_CYCLE = 3;
	private static final int LAST_ITERATION = 7;

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private Config siouxFallsConfig() {
		Config config = ConfigUtils.loadConfig(
				IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("siouxfalls-2014"), "config_default.xml"));
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(LAST_ITERATION);
		config.controller().setCreateGraphs(false);
		config.controller().setDumpDataAtEnd(false);
		config.controller().setWriteEventsInterval(0);
		config.controller().setWritePlansInterval(0);
		config.controller().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.global().setNumberOfThreads(2);
		config.qsim().setNumberOfThreads(2);
		return config;
	}

	/** Counts events per iteration, so a PSim iteration can be told apart from a QSim one. */
	private static final class EventCounter implements BasicEventHandler {
		private final Map<String, Integer> counts = new LinkedHashMap<>();

		@Override
		public void handleEvent(Event event) {
			synchronized (counts) {
				counts.merge(event.getEventType(), 1, Integer::sum);
			}
		}

		@Override
		public void reset(int iteration) {
			synchronized (counts) {
				counts.clear();
			}
		}

		int count(String eventType) {
			synchronized (counts) {
				return counts.getOrDefault(eventType, 0);
			}
		}
	}

	/**
	 * Asks the bound emulator for the trip behind every transit leg in the population and reports
	 * how many of them come back with a usable travel time.
	 */
	private static double finiteTransitTripShare(Controler controler) {
		TransitEmulator emulator = controler.getInjector().getInstance(TransitEmulator.class);
		int finite = 0;
		int total = 0;
		for (Person person : controler.getScenario().getPopulation().getPersons().values()) {
			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				if (!(element instanceof Leg leg) || !(leg.getRoute() instanceof TransitPassengerRoute)) {
					continue;
				}
				total++;
				double departure = leg.getDepartureTime().orElse(8 * 3600.0);
				TransitEmulator.Trip trip = emulator.findTrip(leg, departure);
				if (trip != null && Double.isFinite(trip.egressTime_s() - departure)
						&& trip.egressTime_s() > departure) {
					finite++;
				}
			}
		}
		return total == 0 ? 0.0 : (double) finite / total;
	}

	@Test
	void emulatesTransitAndAlternatesMobsimsWithoutCallerSuppliedBindings() {
		Config config = siouxFallsConfig();
		PSimConfigGroup pSimConfigGroup = new PSimConfigGroup();
		config.addModule(pSimConfigGroup);
		pSimConfigGroup.setIterationsPerCycle(ITERATIONS_PER_CYCLE);

		// No overriding module supplies a TransitEmulator here on purpose: RunPSim used to be
		// unable to build its injector without one.
		RunPSim runPSim = new RunPSim(config, pSimConfigGroup);
		Controler controler = (Controler) runPSim.getMatsimControler();
		PopulationUtils.sampleDown(controler.getScenario().getPopulation(), POPULATION_SAMPLE);

		EventCounter events = new EventCounter();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(events);
			}
		});

		List<Boolean> qsimIteration = new ArrayList<>();
		List<Double> transitShareOnPSimIterations = new ArrayList<>();
		List<Integer> transitBoardingsOfPrecedingIteration = new ArrayList<>();
		controler.addControllerListener((IterationStartsListener) event -> {
			boolean isQSim = controler.getInjector().getInstance(MobSimSwitcher.class).isQSimIteration();
			qsimIteration.add(isQSim);
			// Event handlers are reset before the mobsim, so at the start of an iteration the
			// counter still holds what the preceding iteration produced.
			if (event.getIteration() > 0) {
				transitBoardingsOfPrecedingIteration.add(events.count("PersonEntersPtVehicle"));
			}
			if (!isQSim) {
				transitShareOnPSimIterations.add(finiteTransitTripShare(controler));
			}
		});

		runPSim.run();

		assertInstanceOf(TransitPerformanceFromPSimSpecificImplementation.class,
				controler.getInjector().getInstance(TransitEmulator.class),
				"a scenario that uses transit must emulate it from recorded QSim performance");

		// it 0 QSim, 1-2 PSim, 3 QSim, 4-5 PSim, 6 QSim, 7 QSim (the last is always a QSim).
		assertEquals(List.of(true, false, false, true, false, false, true, true), qsimIteration,
				"queue and pseudo simulation must alternate as configured");

		LogManager.getLogger(PSimSiouxFallsIntegrationTest.class)
				.info("Share of transit legs with a usable emulated travel time, per PSim iteration: {}",
						transitShareOnPSimIterations);

		assertFalse(transitShareOnPSimIterations.isEmpty(), "the run must contain PSim iterations");
		double lastShare = transitShareOnPSimIterations.get(transitShareOnPSimIterations.size() - 1);
		assertTrue(lastShare > 0.5,
				"most transit legs should get a usable emulated travel time once the network has "
						+ "relaxed, but only " + String.format("%.1f%%", 100 * lastShare) + " did");

		assertTrue(transitBoardingsOfPrecedingIteration.stream().anyMatch(boardings -> boardings > 0),
				"the QSim iterations must carry transit passengers for the recorder to observe");
	}

	@Test
	void doesNotEmulateTransitWhenTheScenarioDoesNotSimulateIt() {
		Config config = siouxFallsConfig();
		config.transit().setUseTransit(false);
		config.controller().setLastIteration(2);
		PSimConfigGroup pSimConfigGroup = new PSimConfigGroup();
		config.addModule(pSimConfigGroup);
		pSimConfigGroup.setIterationsPerCycle(ITERATIONS_PER_CYCLE);

		RunPSim runPSim = new RunPSim(config, pSimConfigGroup);
		Controler controler = (Controler) runPSim.getMatsimControler();
		PopulationUtils.sampleDown(controler.getScenario().getPopulation(), POPULATION_SAMPLE);

		runPSim.run();

		assertInstanceOf(NoTransitEmulator.class,
				controler.getInjector().getInstance(TransitEmulator.class));
	}

	@Test
	void transitEmulationFollowsTheScenarioAndTheConfigGroup() {
		Config config = ConfigUtils.createConfig();
		PSimConfigGroup pSimConfigGroup = new PSimConfigGroup();

		config.transit().setUseTransit(true);
		pSimConfigGroup.setFullTransitPerformanceTransmission(true);
		assertTrue(RunPSim.emulatesTransit(config, pSimConfigGroup));

		assertAll(() -> {
			config.transit().setUseTransit(false);
			assertFalse(RunPSim.emulatesTransit(config, pSimConfigGroup),
					"a scenario without transit has no transit performance to record");
		}, () -> {
			config.transit().setUseTransit(true);
			pSimConfigGroup.setFullTransitPerformanceTransmission(false);
			assertFalse(RunPSim.emulatesTransit(config, pSimConfigGroup),
					"transit emulation is opt-out through the PSim config group");
		});
	}
}
