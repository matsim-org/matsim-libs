package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks.SimpleDeadlockAvoidance;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.AdaptiveSpeedProfile;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.MaxSpeedProfile;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.SpeedProfile;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManagerImpl;
import com.google.inject.Provider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.dsim.ExecutionContext;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TrainTimeDistanceHandlerTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void simpleMaxSpeed() throws IOException {
		Path csv = runHandlerForScenario("microSimpleUniDirectionalTrack", new MaxSpeedProfile());
		String content = Files.readString(csv);

		assertThat(content)
			.contains("vehicle_id,line_id,route_id,departure_id,time,distance,type,link_id,stop_id")
			.contains(",target,");
	}

	@Test
	void simpleAdaptiveSpeed() throws IOException {
		Path csv = runHandlerForScenario("microSimpleUniDirectionalTrack", new AdaptiveSpeedProfile());
		String content = Files.readString(csv);

		assertThat(content)
			.contains("vehicle_id,line_id,route_id,departure_id,time,distance,type,link_id,stop_id")
			.contains(",target,");
	}

	private Path runHandlerForScenario(String scenarioName, SpeedProfile profile) throws IOException {
		Path base = Path.of("test/input/ch/sbb/matsim/contrib/railsim/integration").resolve(scenarioName);
		Path configPath = base.resolve("config.xml");
		Config config;
		if (!Files.exists(configPath)) {
			config = ConfigUtils.createConfig();
			config.network().setInputFile("trainNetwork.xml");
			config.transit().setVehiclesFile("transitVehicles.xml");
			config.transit().setTransitScheduleFile("transitSchedule.xml");
		} else {
			config = ConfigUtils.loadConfig(configPath.toString());
		}

		String outputDirectory = utils.getOutputDirectory(scenarioName);
		Files.createDirectories(Path.of(outputDirectory));

		config.controller().setOutputDirectory(outputDirectory);
		config.controller().setCompressionType(ControllerConfigGroup.CompressionType.none);

		Scenario scenario = org.matsim.core.scenario.ScenarioUtils.loadScenario(config);

		RailsimConfigGroup railsim = ConfigUtils.addOrGetModule(config, RailsimConfigGroup.class);
		OutputDirectoryHierarchy io = new OutputDirectoryHierarchy(config);

		Path output = Path.of(io.getIterationFilename(0, "railsimTimeDistance.csv", config.controller().getCompressionType()));
		Files.createDirectories(output.getParent());

		RailResourceManager resources = new RailResourceManagerImpl(
			EventsUtils.createEventsManager(), railsim, scenario.getNetwork(), new SimpleDeadlockAvoidance(scenario.getNetwork()), new TrainManager(scenario)
		);

		MatsimServices services = new StubServices(io, config, scenario);
		TrainTimeDistanceHandler handler = new TrainTimeDistanceHandler(services, resources);

		handler.prepareTimeDistanceApproximation(scenario.getTransitSchedule(), scenario.getTransitVehicles(), profile);
		handler.writeInitialData(scenario.getTransitSchedule(), scenario.getTransitVehicles());

		handler.close();

		return output;
	}

	private static final class StubServices implements MatsimServices {
		private final OutputDirectoryHierarchy io;
		private final Config config;
		private final Scenario scenario;

		StubServices(OutputDirectoryHierarchy io, Config config, Scenario scenario) {
			this.io = io;
			this.config = config;
			this.scenario = scenario;
		}

		@Override
		public OutputDirectoryHierarchy getControllerIO() {
			return io;
		}

		@Override
		public ExecutionContext getSimulationContext() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Integer getIterationNumber() {
			return 0;
		}

		@Override
		public Config getConfig() {
			return config;
		}

		@Override
		public Scenario getScenario() {
			return scenario;
		}

		// Unused methods
		@Override
		public org.matsim.analysis.CalcLinkStats getLinkStats() {
			throw new UnsupportedOperationException();
		}

		@Override
		public org.matsim.analysis.IterationStopWatch getStopwatch() {
			throw new UnsupportedOperationException();
		}

		@Override
		public org.matsim.analysis.ScoreStats getScoreStats() {
			throw new UnsupportedOperationException();
		}

		@Override
		public org.matsim.analysis.VolumesAnalyzer getVolumes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public org.matsim.core.api.experimental.events.EventsManager getEvents() {
			throw new UnsupportedOperationException();
		}

		@Override
		public com.google.inject.Injector getInjector() {
			throw new UnsupportedOperationException();
		}

		@Override
		public org.matsim.core.replanning.StrategyManager getStrategyManager() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Provider<TripRouter> getTripRouterProvider() {
			return () -> {
				throw new UnsupportedOperationException();
			};
		}

		@Override
		public TravelDisutility createTravelDisutilityCalculator() {
			throw new UnsupportedOperationException();
		}

		@Override
		public org.matsim.core.router.util.LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
			throw new UnsupportedOperationException();
		}

		@Override
		public TravelDisutilityFactory getTravelDisutilityFactory() {
			throw new UnsupportedOperationException();
		}

		@Override
		public org.matsim.core.scoring.ScoringFunctionFactory getScoringFunctionFactory() {
			throw new UnsupportedOperationException();
		}

		@Override
		public TravelTime getLinkTravelTimes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addControllerListener(org.matsim.core.controler.listener.ControllerListener controllerListener) {
			throw new UnsupportedOperationException();
		}
	}
}
