/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */

package org.matsim.contrib.drt.extension.benchmark.scenario;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates synthetic DRT benchmark scenarios.
 * <p>
 * Example:
 * <pre>
 * Controler c = SyntheticBenchmarkScenario.builder()
 *     .agents(50_000)
 *     .vehicles(500)
 *     .build();
 * c.run();
 * </pre>
 *
 * @author Steffen Axer
 */
public class SyntheticBenchmarkScenario {
	private static final Logger LOG = LogManager.getLogger(SyntheticBenchmarkScenario.class);

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private int agents = 10_000;
		private int vehicles = 100;
		private int gridSize = 100;
		private double cellSize = 100.0;
		private String outputDirectory = "output/benchmark";
		private boolean timeDependent = true;
		private int vehicleCapacity = 4;

		public Builder agents(int n) { this.agents = n; return this; }
		public Builder vehicles(int n) { this.vehicles = n; return this; }
		public Builder gridSize(int n) { this.gridSize = n; return this; }
		public Builder cellSize(double d) { this.cellSize = d; return this; }
		public Builder outputDirectory(String s) { this.outputDirectory = s; return this; }
		public Builder timeDependent(boolean b) { this.timeDependent = b; return this; }
		public Builder vehicleCapacity(int n) { this.vehicleCapacity = n; return this; }

		public Controler build() {
			LOG.info("Creating benchmark: {} agents, {} vehicles, {}x{} grid", agents, vehicles, gridSize, gridSize);

			Path outputPath = Path.of(outputDirectory).toAbsolutePath();
			// Input directory is a sibling of output, not inside it (to avoid deletion)
			Path inputPath = outputPath.getParent().resolve(outputPath.getFileName() + "_input");
			try {
				Files.createDirectories(inputPath);
			} catch (IOException e) {
				throw new RuntimeException("Failed to create input directory: " + inputPath, e);
			}

			Config config = createConfig(outputPath);
			Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);

			new GridNetworkGenerator(gridSize, cellSize, 50.0 / 3.6, 1800.0, 2.0).generate(scenario);

			Path fleetFile = new FleetGenerator(vehicleCapacity, 0, 86400).generate(vehicles, scenario, inputPath);
			MultiModeDrtConfigGroup.get(config).getModalElements().iterator().next()
				.setVehiclesFile(fleetFile.toAbsolutePath().toString());

			new PopulationGenerator(timeDependent, gridSize * cellSize / 4.0).generate(agents, scenario);

			return DrtControlerCreator.createControler(config, scenario, false);
		}

		private Config createConfig(Path outputPath) {
			DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
			dvrpConfig.getTravelTimeMatrixParams()
				.addParameterSet(dvrpConfig.getTravelTimeMatrixParams()
					.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

			MultiModeDrtConfigGroup multiModeDrtConfig = new MultiModeDrtConfigGroup();
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.setMode("drt");
			drtConfig.setStopDuration(60.0);

			var constraints = drtConfig.addOrGetDrtOptimizationConstraintsParams()
				.addOrGetDefaultDrtOptimizationConstraintsSet();
			constraints.setMaxWaitTime(900);
			constraints.setMaxTravelTimeAlpha(1.5);
			constraints.setMaxTravelTimeBeta(300);
			constraints.setRejectRequestIfMaxWaitOrTravelTimeViolated(false);

			// Set insertion search params
			drtConfig.addParameterSet(new SelectiveInsertionSearchParams());

			multiModeDrtConfig.addParameterSet(drtConfig);

			Config config = ConfigUtils.createConfig(multiModeDrtConfig, dvrpConfig);
			config.controller().setOutputDirectory(outputPath.toString());
			config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controller().setLastIteration(0);
			config.controller().setWriteEventsInterval(0);
			config.controller().setWritePlansInterval(0);
			config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
			config.qsim().setStartTime(0);
			config.qsim().setEndTime(86400);
			config.replanning().clearStrategySettings();

			// Add scoring parameters for DRT mode
			config.scoring().addModeParams(new ScoringConfigGroup.ModeParams("drt"));

			return config;
		}
	}
}
