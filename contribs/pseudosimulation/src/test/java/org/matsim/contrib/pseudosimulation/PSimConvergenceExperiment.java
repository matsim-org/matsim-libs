package org.matsim.contrib.pseudosimulation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

/**
 * Reproduces the QSim-versus-PSim convergence comparison of Fourie, Illenberger and Nagel (2013)
 * on the Sioux Falls 2014 scenario, which simulates car traffic and transit on the same network.
 * <p>
 * This is a long-running experiment driver, not a unit test: the baseline alone is 100 queue
 * simulation iterations. Run it explicitly, for example
 *
 * <pre>
 * mvn -pl contribs/pseudosimulation exec:java \
 *     -Dexec.mainClass=org.matsim.contrib.pseudosimulation.PSimConvergenceExperiment \
 *     -Dexec.classpathScope=test -Dexec.args="&lt;outputDirectory&gt; both"
 * </pre>
 * <p>
 * Each run writes {@code progress.csv} (one row per iteration) and, from the final queue
 * simulation iteration, {@code departures.csv}, {@code linkvolumes.csv} and {@code traveltimes.csv}
 * so the two runs can be compared on simulation state and not on average score alone.
 * <p>
 * The score column is the mean score of the population's selected plans, sampled after
 * MobSimSwitcher has restored the agents PSim did not simulate. MATSim's own score statistic is
 * not comparable across the two run types, because on a PSim iteration it covers only the
 * replanned agents.
 */
public final class PSimConvergenceExperiment {

	private static final Logger LOG = LogManager.getLogger(PSimConvergenceExperiment.class);

	/** Departure profile bin width, as in the paper's departure profile RMSD. */
	private static final int DEPARTURE_BIN_S = 300;

	private static final double TOTAL_REPLANNING_RATE_BASELINE = 0.30;
	private static final double TOTAL_REPLANNING_RATE_PSIM = 0.10;
	private static final int BASELINE_ITERATIONS = 100;
	/** One QSim iteration per 24 PSim iterations, the paper's most effective ratio. */
	private static final int PSIM_ITERATIONS_PER_CYCLE = 25;
	private static final int PSIM_QSIM_ITERATIONS = 40;
	private static final double INNOVATION_OFF_FRACTION = 0.9;

	private PSimConvergenceExperiment() {
	}

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			throw new IllegalArgumentException(
					"usage: <outputDirectory> [baseline|psim|both] [fullTransitPerformance|waitAndStopStopTimes]");
		}
		Path root = Path.of(args[0]);
		String which = args.length > 1 ? args[1] : "both";
		PSimConfigGroup.TransitEmulation emulation = args.length > 2
				? PSimConfigGroup.TransitEmulation.valueOf(args[2])
				: PSimConfigGroup.TransitEmulation.fullTransitPerformance;

		if (which.equals("baseline") || which.equals("both")) {
			runBaseline(root.resolve("qsim-baseline"));
		}
		if (which.equals("psim") || which.equals("both")) {
			runPSim(root.resolve("psim-1to24-" + emulation), emulation);
		}
		LOG.info("Experiment finished under {}", root.toAbsolutePath());
	}

	/**
	 * Applies the replanning strategy of the experiment: rerouting and time allocation mutation
	 * share the innovation budget equally, and an ExpBeta selector takes the remainder.
	 */
	private static void configureReplanning(Config config, double totalReplanningRate) {
		config.replanning().clearStrategySettings();

		StrategySettings selector = new StrategySettings();
		selector.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
		selector.setWeight(1.0 - totalReplanningRate);
		config.replanning().addStrategySettings(selector);

		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
		reRoute.setWeight(totalReplanningRate / 2.0);
		config.replanning().addStrategySettings(reRoute);

		StrategySettings timeAllocation = new StrategySettings();
		timeAllocation.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator);
		timeAllocation.setWeight(totalReplanningRate / 2.0);
		config.replanning().addStrategySettings(timeAllocation);

		config.replanning().setFractionOfIterationsToDisableInnovation(INNOVATION_OFF_FRACTION);
	}

	private static Config baseConfig(Path output, int lastIteration, double totalReplanningRate) {
		Config config = ConfigUtils.loadConfig(
				IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("siouxfalls-2014"), "config_default.xml"));
		config.controller().setOutputDirectory(output.toString());
		config.controller().setLastIteration(lastIteration);
		config.controller().setCreateGraphs(false);
		config.controller().setWriteEventsInterval(0);
		config.controller().setWritePlansInterval(0);
		config.controller().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.global().setNumberOfThreads(Runtime.getRuntime().availableProcessors());
		config.qsim().setNumberOfThreads(Runtime.getRuntime().availableProcessors());
		config.global().setRandomSeed(4711L);
		configureReplanning(config, totalReplanningRate);
		return config;
	}

	private static void runBaseline(Path output) throws IOException {
		LOG.info("=== QSim-only baseline: {} iterations, replanning rate {} ===",
				BASELINE_ITERATIONS, TOTAL_REPLANNING_RATE_BASELINE);
		Config config = baseConfig(output, BASELINE_ITERATIONS, TOTAL_REPLANNING_RATE_BASELINE);
		Controler controler = new Controler(config);
		Recorder recorder = new Recorder(controler, output, () -> true);
		recorder.attach();
		controler.run();
		recorder.write();
	}

	private static void runPSim(Path output, PSimConfigGroup.TransitEmulation emulation) throws IOException {
		LOG.info("=== PSim: QSim:PSim 1:{}, {} QSim iterations, replanning rate {}, transit emulation {} ===",
				PSIM_ITERATIONS_PER_CYCLE - 1, PSIM_QSIM_ITERATIONS, TOTAL_REPLANNING_RATE_PSIM, emulation);
		int lastIteration = PSIM_QSIM_ITERATIONS * PSIM_ITERATIONS_PER_CYCLE;
		Config config = baseConfig(output, lastIteration, TOTAL_REPLANNING_RATE_PSIM);

		PSimConfigGroup pSimConfigGroup = new PSimConfigGroup();
		config.addModule(pSimConfigGroup);
		pSimConfigGroup.setIterationsPerCycle(PSIM_ITERATIONS_PER_CYCLE);
		pSimConfigGroup.setTransitEmulation(emulation);

		RunPSim runPSim = new RunPSim(config, pSimConfigGroup);
		Controler controler = (Controler) runPSim.getMatsimControler();
		MobSimSwitcher[] switcher = new MobSimSwitcher[1];
		Recorder recorder = new Recorder(controler, output, () -> {
			if (switcher[0] == null) {
				switcher[0] = controler.getInjector().getInstance(MobSimSwitcher.class);
			}
			return switcher[0].isQSimIteration();
		});
		recorder.attach();
		runPSim.run();
		recorder.write();
	}

	/** Tells whether the iteration currently running used the queue simulation. */
	private interface QSimIterationTest {
		boolean isQSimIteration();
	}

	/**
	 * Records the score trajectory of a run, and the simulation state produced by its final
	 * queue simulation iteration.
	 */
	private static final class Recorder implements IterationEndsListener {

		private final Controler controler;
		private final Path output;
		private final QSimIterationTest qsimTest;
		private final List<String> progress = new ArrayList<>();
		private final StateCollector state = new StateCollector();
		private long iterationStartedAt = System.currentTimeMillis();

		Recorder(Controler controler, Path output, QSimIterationTest qsimTest) {
			this.controler = controler;
			this.output = output;
			this.qsimTest = qsimTest;
			progress.add("iteration,mobsim,meanSelectedPlanScore,carDepartures,ptDepartures,walkDepartures,"
					+ "carModeShare,wallClockMillis");
		}

		void attach() {
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(state);
				}
			});
			// Added last so it runs after MobSimSwitcher has restored unsimulated agents' scores.
			controler.addControllerListener(this);
		}

		@Override
		public void notifyIterationEnds(org.matsim.core.controler.events.IterationEndsEvent event) {
			boolean isQSim = qsimTest.isQSimIteration();
			double meanScore = meanSelectedPlanScore();
			long elapsed = System.currentTimeMillis() - iterationStartedAt;
			iterationStartedAt = System.currentTimeMillis();

			int car = state.departuresByMode.getOrDefault("car", 0);
			int pt = state.departuresByMode.getOrDefault("pt", 0);
			int walk = state.departuresByMode.getOrDefault("walk", 0);
			double motorised = car + pt;
			progress.add(String.format(Locale.ROOT, "%d,%s,%.6f,%d,%d,%d,%.6f,%d",
					event.getIteration(), isQSim ? "QSim" : "PSim", meanScore, car, pt, walk,
					motorised == 0 ? Double.NaN : car / motorised, elapsed));

			if (event.isLastIteration()) {
				state.freeze();
			}
		}

		private double meanSelectedPlanScore() {
			double sum = 0;
			int n = 0;
			for (Person person : controler.getScenario().getPopulation().getPersons().values()) {
				Double score = person.getSelectedPlan().getScore();
				if (score != null) {
					sum += score;
					n++;
				}
			}
			return n == 0 ? Double.NaN : sum / n;
		}

		void write() throws IOException {
			Files.createDirectories(output);
			Files.write(output.resolve("progress.csv"), progress);
			state.write(output);
			LOG.info("Wrote experiment records to {}", output.toAbsolutePath());
		}
	}

	/**
	 * Accumulates departure times, link volumes and per-agent travel times. Only the snapshot of
	 * the final iteration is kept, so the two runs are compared on the same kind of iteration.
	 */
	private static final class StateCollector
			implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler {

		private final Map<String, Integer> departuresByMode = new LinkedHashMap<>();
		private Map<Integer, Integer> departureBins = new TreeMap<>();
		private Map<Id<Link>, Integer> linkVolumes = new LinkedHashMap<>();
		private Map<Id<Person>, Double> departureTimes = new LinkedHashMap<>();
		private Map<Id<Person>, Double> travelTimes = new LinkedHashMap<>();
		private boolean frozen;

		@Override
		public void reset(int iteration) {
			if (frozen) {
				return;
			}
			departuresByMode.clear();
			departureBins = new TreeMap<>();
			linkVolumes = new LinkedHashMap<>();
			departureTimes = new LinkedHashMap<>();
			travelTimes = new LinkedHashMap<>();
		}

		void freeze() {
			frozen = true;
		}

		@Override
		public synchronized void handleEvent(PersonDepartureEvent event) {
			departuresByMode.merge(event.getLegMode(), 1, Integer::sum);
			departureBins.merge((int) (event.getTime() / DEPARTURE_BIN_S), 1, Integer::sum);
			departureTimes.put(event.getPersonId(), event.getTime());
		}

		@Override
		public synchronized void handleEvent(PersonArrivalEvent event) {
			Double departure = departureTimes.remove(event.getPersonId());
			if (departure != null) {
				travelTimes.merge(event.getPersonId(), event.getTime() - departure, Double::sum);
			}
		}

		@Override
		public synchronized void handleEvent(LinkEnterEvent event) {
			linkVolumes.merge(event.getLinkId(), 1, Integer::sum);
		}

		void write(Path output) {
			try {
				writeLines(output.resolve("departures.csv"), "bin,departures",
						departureBins.entrySet().stream()
								.map(e -> e.getKey() + "," + e.getValue()).toList());
				writeLines(output.resolve("linkvolumes.csv"), "link,volume",
						linkVolumes.entrySet().stream()
								.map(e -> e.getKey() + "," + e.getValue()).toList());
				writeLines(output.resolve("traveltimes.csv"), "person,totalTravelTimeSeconds",
						travelTimes.entrySet().stream()
								.map(e -> e.getKey() + "," + String.format(Locale.ROOT, "%.1f", e.getValue()))
								.toList());
			} catch (IOException exception) {
				throw new UncheckedIOException(exception);
			}
		}

		private static void writeLines(Path file, String header, List<String> rows) throws IOException {
			try (BufferedWriter writer = Files.newBufferedWriter(file)) {
				writer.write(header);
				writer.newLine();
				for (String row : rows) {
					writer.write(row);
					writer.newLine();
				}
			}
		}
	}
}
