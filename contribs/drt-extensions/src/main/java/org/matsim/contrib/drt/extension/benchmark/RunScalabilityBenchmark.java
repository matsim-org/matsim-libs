/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */

package org.matsim.contrib.drt.extension.benchmark;

import org.matsim.contrib.drt.extension.benchmark.InsertionStrategy.InsertionSearchStrategy;
import org.matsim.contrib.drt.extension.benchmark.InsertionStrategy.RequestInserterType;
import org.matsim.contrib.drt.extension.benchmark.scenario.SyntheticBenchmarkScenario;
import org.matsim.contrib.drt.extension.insertion.spatialFilter.DrtSpatialRequestFleetFilterParams;
import org.matsim.contrib.drt.extension.insertion.spatialFilter.SpatialFilterInsertionSearchQSimModule;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.repeatedselective.RepeatedSelectiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.parallel.DrtParallelInserterParams;
import org.matsim.contrib.drt.optimizer.insertion.parallel.DrtParallelInserterParams.RequestsPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.DrtParallelInserterParams.VehiclesPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.ParallelRequestInserterModule;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.Controler;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DRT Scalability Benchmark: Tests combinations of request inserter types and insertion search strategies.
 * <p>
 * The benchmark has two orthogonal dimensions:
 * <ul>
 *   <li><b>Request Inserter Type</b> – HOW requests are dispatched:
 *       <ul>
 *         <li>{@code Default} – sequential processing via DefaultUnplannedRequestInserter</li>
 *         <li>{@code Parallel} – partitioned parallel processing via ParallelUnplannedRequestInserter</li>
 *       </ul>
 *   </li>
 *   <li><b>Insertion Search Strategy</b> – WHICH algorithm finds the best insertion:
 *       <ul>
 *         <li>{@code Selective} – fast heuristic (single best insertion per request)</li>
 *         <li>{@code Extensive} – evaluates all feasible insertions</li>
 *         <li>{@code RepeatedSelective} – retries selective search multiple times</li>
 *       </ul>
 *   </li>
 * </ul>
 * Every combination is valid (e.g., Parallel + Extensive, Default + Selective, etc.).
 * <p>
 * <b>Usage:</b>
 * <pre>
 * java -cp matsim.jar org.matsim.contrib.drt.extension.benchmark.RunScalabilityBenchmark \
 *     --config-path benchmark-config.xml \
 *     --config:drtBenchmark.requestInserterTypes Default,Parallel \
 *     --config:drtBenchmark.insertionSearchStrategies Selective,Extensive \
 *     --config:drtBenchmark.agentCounts 50000,100000
 * </pre>
 * <p>
 * <b>Sample config.xml:</b>
 * <pre>{@code
 * <config>
 *   <module name="drtBenchmark">
 *     <param name="agentCounts" value="50000,100000"/>
 *     <param name="requestInserterTypes" value="Default,Parallel"/>
 *     <param name="insertionSearchStrategies" value="Selective,Extensive"/>
 *     <param name="vehiclePartitioners" value="ShiftingRoundRobin"/>
 *     <param name="requestPartitioners" value="LoadAware"/>
 *     <param name="maxPartitions" value="8"/>
 *   </module>
 * </config>
 * }</pre>
 *
 * @author Steffen Axer
 * @see DrtBenchmarkConfigGroup
 * @see InsertionStrategy
 */
public class RunScalabilityBenchmark {

	public static void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args)
			.requireOptions("config-path")
			.allowPositionalArguments(false)
			.build();

		Config config = ConfigUtils.loadConfig(
			cmd.getOptionStrict("config-path"),
			new DrtBenchmarkConfigGroup()
		);

		cmd.applyConfiguration(config);

		DrtBenchmarkConfigGroup benchmarkConfig = DrtBenchmarkConfigGroup.get(config);
		printConfig(benchmarkConfig);

		runBenchmark(benchmarkConfig);
	}

	/**
	 * Runs the benchmark with the given configuration.
	 * <p>
	 * Iterates over the cross-product of:
	 * agents × searchStrategy × detourPathCalculator × inserterType (× partitioners × collectionPeriods for Parallel).
	 */
	public static void runBenchmark(DrtBenchmarkConfigGroup config) {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

		DrtBenchmarkRunner runner = DrtBenchmarkRunner.create()
			.warmupRuns(config.getWarmupRuns())
			.measuredRuns(config.getMeasuredRuns())
			.reportTo(Path.of(String.format("%s/benchmark_%s.csv",
				config.getOutputDirectory(), timestamp)));

		for (int agents : config.getAgentCounts()) {
			int vehicles = agents * config.getVehiclesPerHundredAgents() / 100;

			for (InsertionSearchStrategy searchStrategy : config.getInsertionSearchStrategies()) {
				for (ControllerConfigGroup.RoutingAlgorithmType routingType : config.getRoutingAlgorithmTypes()) {
					for (RequestInserterType inserterType : config.getRequestInserterTypes()) {

						if (inserterType == RequestInserterType.Parallel) {
							for (int collectionPeriod : config.getCollectionPeriods()) {
								for (VehiclesPartitioner vp : config.getVehiclePartitioners()) {
									for (RequestsPartitioner rp : config.getRequestPartitioners()) {

										String scenarioName = String.format("Parallel_%s_%s_%s_%s_cp%d_%dk",
											searchStrategy.name(), routingType.name(),
											shortName(vp), shortName(rp),
											collectionPeriod, agents / 1000);
										String outputDir = String.format("%s/%s/%s",
											config.getOutputDirectory(), timestamp, scenarioName);

										final int fAgents = agents, fVehicles = vehicles, fCp = collectionPeriod;
										final VehiclesPartitioner fVp = vp;
										final RequestsPartitioner fRp = rp;
										final InsertionSearchStrategy fSearch = searchStrategy;
										final ControllerConfigGroup.RoutingAlgorithmType fRoutingType = routingType;
										final DrtBenchmarkConfigGroup fConfig = config;

										runner.addScenario(scenarioName, () -> {
											DrtInsertionSearchParams searchParams = createSearchParams(fSearch);
											Controler c = buildControler(fConfig, fAgents, fVehicles,
												outputDir, searchParams, fRoutingType);

											var drtCfg = MultiModeDrtConfigGroup.get(c.getConfig())
												.getModalElements().iterator().next();

											// Configure parallel inserter
											DrtParallelInserterParams params = new DrtParallelInserterParams();
											params.setVehiclesPartitioner(fVp);
											params.setRequestsPartitioner(fRp);
											params.setMaxPartitions(fConfig.getMaxPartitions());
											params.setMaxIterations(fConfig.getMaxIterations());
											params.setCollectionPeriod(fCp);
											params.setLogPerformanceStats(fConfig.isLogPerformanceStats());
											drtCfg.addParameterSet(params);

											applySpatialFilter(fConfig, c, drtCfg);
											c.addOverridingQSimModule(new ParallelRequestInserterModule(drtCfg));
											c.run();
											return outputDir;
										});
									}
								}
							}
						} else {
							// Default (sequential) inserter
							String scenarioName = String.format("Default_%s_%s_%dk",
								searchStrategy.name(), routingType.name(), agents / 1000);
							String outputDir = String.format("%s/%s/%s",
								config.getOutputDirectory(), timestamp, scenarioName);

							final int fAgents = agents, fVehicles = vehicles;
							final InsertionSearchStrategy fSearch = searchStrategy;
							final ControllerConfigGroup.RoutingAlgorithmType fRoutingType = routingType;
							final DrtBenchmarkConfigGroup fConfig = config;

							runner.addScenario(scenarioName, () -> {
								DrtInsertionSearchParams searchParams = createSearchParams(fSearch);
								Controler c = buildControler(fConfig, fAgents, fVehicles,
									outputDir, searchParams, fRoutingType);

								var drtCfg = MultiModeDrtConfigGroup.get(c.getConfig())
									.getModalElements().iterator().next();

								applySpatialFilter(fConfig, c, drtCfg);
								c.run();
								return outputDir;
							});
						}
					}
				}
			}
		}

		runner.run();
	}

	/**
	 * Builds a Controler and sets the routing algorithm type directly on the controller config.
	 */
	private static Controler buildControler(DrtBenchmarkConfigGroup config, int agents,
											int vehicles, String outputDir,
											DrtInsertionSearchParams searchParams,
											ControllerConfigGroup.RoutingAlgorithmType routingAlgorithmType) {
		var builder = SyntheticBenchmarkScenario.builder()
			.agents(agents)
			.vehicles(vehicles)
			.outputDirectory(outputDir)
			.matrixCellSize(config.getMatrixCellSize())
			.insertionSearchParams(searchParams);

		if (config.hasExternalNetwork()) {
			builder.networkUrl(config.getNetworkUrl());
		}

		Controler c = builder.build();
		c.getConfig().controller().setRoutingAlgorithmType(routingAlgorithmType);
		return c;
	}

	/**
	 * Creates the appropriate DrtInsertionSearchParams for the given search strategy.
	 */
	private static DrtInsertionSearchParams createSearchParams(InsertionSearchStrategy strategy) {
		return switch (strategy) {
			case Selective -> new SelectiveInsertionSearchParams();
			case Extensive -> new ExtensiveInsertionSearchParams();
			case RepeatedSelective -> new RepeatedSelectiveInsertionSearchParams();
		};
	}

	/**
	 * Applies spatial filter if enabled in config.
	 */
	private static void applySpatialFilter(DrtBenchmarkConfigGroup config, Controler c,
										   org.matsim.contrib.drt.run.DrtConfigGroup drtCfg) {
		if (config.isUseSpatialFilter()) {
			DrtSpatialRequestFleetFilterParams spatialParams = new DrtSpatialRequestFleetFilterParams();
			drtCfg.addParameterSet(spatialParams);
			c.addOverridingQSimModule(new SpatialFilterInsertionSearchQSimModule(drtCfg));
		}
	}

	private static void printConfig(DrtBenchmarkConfigGroup config) {
		System.out.println("=== DRT Benchmark Configuration ===");
		System.out.println("Agents: " + config.getAgentCounts());
		System.out.println("Vehicles per 100 agents: " + config.getVehiclesPerHundredAgents());
		System.out.println("Request Inserter Types: " + config.getRequestInserterTypesString());
		System.out.println("Insertion Search Strategies: " + config.getInsertionSearchStrategiesString());
		System.out.println("Routing Algorithm Types: " + config.getDetourPathCalculatorTypesString());
		System.out.println("Vehicle Partitioners (Parallel): " + config.getVehiclePartitionersString());
		System.out.println("Request Partitioners (Parallel): " + config.getRequestPartitionersString());
		System.out.println("Collection Periods (Parallel): " + config.getCollectionPeriods());
		System.out.println("Max Partitions: " + config.getMaxPartitions());
		System.out.println("Max Iterations: " + config.getMaxIterations());
		System.out.println("Matrix Cell Size [m]: " + config.getMatrixCellSize());
		System.out.println("Warmup Runs: " + config.getWarmupRuns());
		System.out.println("Measured Runs: " + config.getMeasuredRuns());
		System.out.println("Output Directory: " + config.getOutputDirectory());
		System.out.println("Use Spatial Filter: " + config.isUseSpatialFilter());
		System.out.println("Network URL: " + (config.hasExternalNetwork() ? config.getNetworkUrl() : "(synthetic grid)"));
		System.out.println("====================================");

		int agentCount = config.getAgentCounts().size();
		int searchCount = config.getInsertionSearchStrategies().size();
		int routingTypeCount = config.getRoutingAlgorithmTypes().size();
		int parallelCombinations = config.getVehiclePartitioners().size()
			* config.getRequestPartitioners().size()
			* config.getCollectionPeriods().size();
		int totalScenarios = 0;
		for (RequestInserterType inserterType : config.getRequestInserterTypes()) {
			if (inserterType == RequestInserterType.Parallel) {
				totalScenarios += agentCount * searchCount * routingTypeCount * parallelCombinations;
			} else {
				totalScenarios += agentCount * searchCount * routingTypeCount;
			}
		}
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
