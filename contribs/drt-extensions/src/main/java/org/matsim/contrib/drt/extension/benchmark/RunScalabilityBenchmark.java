/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */

package org.matsim.contrib.drt.extension.benchmark;

import org.matsim.contrib.drt.extension.benchmark.scenario.SyntheticBenchmarkScenario;
import org.matsim.contrib.drt.extension.insertion.spatialFilter.DrtSpatialRequestFleetFilterParams;
import org.matsim.contrib.drt.extension.insertion.spatialFilter.SpatialFilterInsertionSearchQSimModule;
import org.matsim.contrib.drt.optimizer.insertion.parallel.DrtParallelInserterParams;
import org.matsim.contrib.drt.optimizer.insertion.parallel.DrtParallelInserterParams.RequestsPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.DrtParallelInserterParams.VehiclesPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.ParallelRequestInserterModule;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DRT Scalability Benchmark: Tests partitioner combinations with configurable parameters.
 * <p>
 * <b>Usage:</b>
 * <pre>
 * # Run with MATSim config file (required)
 * java -cp matsim.jar org.matsim.contrib.drt.extension.benchmark.RunScalabilityBenchmark --config-path benchmark-config.xml
 *
 * # Override specific parameters via command line
 * java -cp matsim.jar org.matsim.contrib.drt.extension.benchmark.RunScalabilityBenchmark \
 *     --config-path benchmark-config.xml \
 *     --config:drtBenchmark.agentCounts 50000,100000,200000 \
 *     --config:drtBenchmark.maxPartitions 16
 * </pre>
 * <p>
 * <b>Sample config.xml:</b>
 * <pre>{@code
 * <config>
 *   <module name="drtBenchmark">
 *     <param name="agentCounts" value="50000,100000"/>
 *     <param name="vehiclePartitioners" value="Replicating,RoundRobin"/>
 *     <param name="requestPartitioners" value="RoundRobin,LoadAware"/>
 *     <param name="maxPartitions" value="8"/>
 *   </module>
 * </config>
 * }</pre>
 *
 * @author Steffen Axer
 * @see DrtBenchmarkConfigGroup
 */
public class RunScalabilityBenchmark {

	public static void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args)
			.requireOptions("config-path")
			.allowPositionalArguments(false)
			.build();

		// Load config with benchmark module
		Config config = ConfigUtils.loadConfig(
			cmd.getOptionStrict("config-path"),
			new DrtBenchmarkConfigGroup()
		);

		// Apply command line overrides (e.g., --config:drtBenchmark.agentCounts 100000)
		cmd.applyConfiguration(config);

		DrtBenchmarkConfigGroup benchmarkConfig = DrtBenchmarkConfigGroup.get(config);
		printConfig(benchmarkConfig);

		// Run benchmark
		runBenchmark(benchmarkConfig);
	}

	/**
	 * Runs the benchmark with the given configuration.
	 */
	public static void runBenchmark(DrtBenchmarkConfigGroup config) {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

		DrtBenchmarkRunner runner = DrtBenchmarkRunner.create()
			.warmupRuns(config.getWarmupRuns())
			.measuredRuns(config.getMeasuredRuns())
			.reportTo(Path.of(String.format("%s/partitioner_comparison_%s.csv",
				config.getOutputDirectory(), timestamp)));

		for (int agents : config.getAgentCounts()) {
			int vehicles = agents * config.getVehiclesPerHundredAgents() / 100;

			for (int collectionPeriod : config.getCollectionPeriods()) {
				for (VehiclesPartitioner vp : config.getVehiclePartitioners()) {
					for (RequestsPartitioner rp : config.getRequestPartitioners()) {

						String scenarioName = String.format("%s_%s_cp%d_%dk",
							shortName(vp), shortName(rp), collectionPeriod, agents / 1000);
						String outputDir = String.format("%s/%s/%s",
							config.getOutputDirectory(), timestamp, scenarioName);

						// Capture config values for lambda
						final int finalAgents = agents;
						final int finalVehicles = vehicles;
						final int finalCollectionPeriod = collectionPeriod;
						final VehiclesPartitioner finalVp = vp;
						final RequestsPartitioner finalRp = rp;
						final DrtBenchmarkConfigGroup finalConfig = config;

						runner.addScenario(scenarioName, () -> {
							Controler c = SyntheticBenchmarkScenario.builder()
								.agents(finalAgents)
								.vehicles(finalVehicles)
								.outputDirectory(outputDir)
								.build();

							var drtCfg = MultiModeDrtConfigGroup.get(c.getConfig())
								.getModalElements().iterator().next();

							DrtParallelInserterParams params = new DrtParallelInserterParams();
							params.setVehiclesPartitioner(finalVp);
							params.setRequestsPartitioner(finalRp);
							params.setMaxPartitions(finalConfig.getMaxPartitions());
							params.setMaxIterations(finalConfig.getMaxIterations());
							params.setCollectionPeriod(finalCollectionPeriod);
							params.setLogPerformanceStats(finalConfig.isLogPerformanceStats());
							drtCfg.addParameterSet(params);

							if (finalConfig.isUseSpatialFilter()) {
								DrtSpatialRequestFleetFilterParams spatialParams =
									new DrtSpatialRequestFleetFilterParams();
								drtCfg.addParameterSet(spatialParams);
								c.addOverridingQSimModule(
									new SpatialFilterInsertionSearchQSimModule(drtCfg));
							}

							c.addOverridingQSimModule(new ParallelRequestInserterModule(drtCfg));
							c.run();
							return outputDir;
						});
					}
				}
			}
		}

		runner.run();
	}

	private static void printConfig(DrtBenchmarkConfigGroup config) {
		System.out.println("=== DRT Benchmark Configuration ===");
		System.out.println("Agents: " + config.getAgentCounts());
		System.out.println("Vehicles per 100 agents: " + config.getVehiclesPerHundredAgents());
		System.out.println("Vehicle Partitioners: " + config.getVehiclePartitionersString());
		System.out.println("Request Partitioners: " + config.getRequestPartitionersString());
		System.out.println("Collection Periods: " + config.getCollectionPeriods());
		System.out.println("Max Partitions: " + config.getMaxPartitions());
		System.out.println("Max Iterations: " + config.getMaxIterations());
		System.out.println("Warmup Runs: " + config.getWarmupRuns());
		System.out.println("Measured Runs: " + config.getMeasuredRuns());
		System.out.println("Output Directory: " + config.getOutputDirectory());
		System.out.println("Use Spatial Filter: " + config.isUseSpatialFilter());
		System.out.println("====================================");

		int totalScenarios = config.getAgentCounts().size()
			* config.getVehiclePartitioners().size()
			* config.getRequestPartitioners().size()
			* config.getCollectionPeriods().size();
		System.out.println("Total scenarios to run: " + totalScenarios);
	}

	private static String shortName(VehiclesPartitioner vp) {
		return switch (vp) {
			case ReplicatingVehicleEntryPartitioner -> "Repl";
			case RoundRobinVehicleEntryPartitioner -> "RR";
			case ShiftingRoundRobinVehicleEntryPartitioner -> "ShiftRR";
		};
	}

	private static String shortName(RequestsPartitioner rp) {
		return switch (rp) {
			case RoundRobinRequestsPartitioner -> "RR";
			case LoadAwareRoundRobinRequestsPartitioner -> "LoadAware";
		};
	}
}
