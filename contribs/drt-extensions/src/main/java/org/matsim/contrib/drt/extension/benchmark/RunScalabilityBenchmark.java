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
import org.matsim.core.controler.Controler;

import java.nio.file.Path;
import java.util.List;

/**
 * DRT Scalability Benchmark: Tests all partitioner combinations.
 * <p>
 * Combinations tested:
 * - 3 VehiclesPartitioners × 2 RequestsPartitioners × 2 CollectionPeriods = 12 scenarios
 * <p>
 * Run: {@code java -cp matsim.jar org.matsim.contrib.drt.extension.benchmark.RunScalabilityBenchmark}
 *
 * @author Steffen Axer
 */
public class RunScalabilityBenchmark {

	public static void main(String[] args) {
		int agents = 50_000;
		int vehicles = agents / 100;

		List<Integer> collectionPeriods = List.of(90);
		List<VehiclesPartitioner> vehiclePartitioners = List.of(
			VehiclesPartitioner.ReplicatingVehicleEntryPartitioner
			//VehiclesPartitioner.RoundRobinVehicleEntryPartitioner,
			//VehiclesPartitioner.ShiftingRoundRobinVehicleEntryPartitioner
		);
		List<RequestsPartitioner> requestPartitioners = List.of(
			RequestsPartitioner.RoundRobinRequestsPartitioner
			//RequestsPartitioner.LoadAwareRoundRobinRequestsPartitioner
		);

		DrtBenchmarkRunner runner = DrtBenchmarkRunner.create()
			.warmupRuns(0)
			.measuredRuns(1)
			.reportTo(Path.of("output/benchmark/partitioner_comparison.csv"));

		for (int collectionPeriod : collectionPeriods) {
			for (VehiclesPartitioner vp : vehiclePartitioners) {
				for (RequestsPartitioner rp : requestPartitioners) {
					String scenarioName = String.format("%s_%s_cp%d",
						shortName(vp), shortName(rp), collectionPeriod);
					String outputDir = String.format("output/benchmark/%s", scenarioName);

					runner.addScenario(scenarioName, () -> {
						Controler c = SyntheticBenchmarkScenario.builder()
							.agents(agents).vehicles(vehicles)
							.outputDirectory(outputDir)
							.build();

						var drtCfg = MultiModeDrtConfigGroup.get(c.getConfig()).getModalElements().iterator().next();
						DrtParallelInserterParams params = new DrtParallelInserterParams();
						params.setVehiclesPartitioner(vp);
						params.setRequestsPartitioner(rp);
						params.setMaxPartitions(8);
						params.setMaxIterations(3);
						params.setCollectionPeriod(collectionPeriod);
						params.setLogPerformanceStats(true);
						drtCfg.addParameterSet(params);

						DrtSpatialRequestFleetFilterParams spatialParams = new DrtSpatialRequestFleetFilterParams();
						drtCfg.addParameterSet(spatialParams);
						c.addOverridingQSimModule(new ParallelRequestInserterModule(drtCfg));
						c.addOverridingQSimModule(new SpatialFilterInsertionSearchQSimModule(drtCfg));

						c.run();
					});
				}
			}
		}

		runner.run();
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
