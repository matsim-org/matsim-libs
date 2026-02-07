/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */

package org.matsim.contrib.drt.extension.benchmark;

import org.matsim.contrib.drt.extension.benchmark.scenario.SyntheticBenchmarkScenario;
import org.matsim.contrib.drt.extension.insertion.spatialFilter.DrtSpatialRequestFleetFilterParams;
import org.matsim.contrib.drt.extension.insertion.spatialFilter.SpatialFilterInsertionSearchQSimModule;
import org.matsim.contrib.drt.optimizer.insertion.parallel.DrtParallelInserterParams;
import org.matsim.contrib.drt.optimizer.insertion.parallel.ParallelRequestInserterModule;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.controler.Controler;

import java.nio.file.Path;
import java.util.List;

/**
 * DRT Scalability Benchmark: Tests performance with 10k, 20k, 50k, 100k agents.
 * <p>
 * Run: {@code java -cp matsim.jar org.matsim.contrib.drt.extension.benchmark.RunScalabilityBenchmark}
 *
 * @author Steffen Axer
 */
public class RunScalabilityBenchmark {

	public static void main(String[] args) {
		List<Integer> demandLevels = List.of(100_000);

		DrtBenchmarkRunner runner = DrtBenchmarkRunner.create()
			.warmupRuns(0)
			.measuredRuns(3)
			.reportTo(Path.of("output/benchmark/scalability_results.csv"));

		for (int agents : demandLevels) {
			int vehicles = agents / 100;
			String suffix = agents / 1000 + "k";

			// Default inserter
			runner.addScenario("Default-" + suffix, () -> {
				Controler c = SyntheticBenchmarkScenario.builder()
					.agents(agents).vehicles(vehicles)
					.outputDirectory("output/benchmark/default_" + suffix)
					.build();
				c.run();
			});

			// Parallel inserter (8 partitions)
			runner.addScenario("Parallel8-" + suffix, () -> {
				Controler c = SyntheticBenchmarkScenario.builder()
					.agents(agents).vehicles(vehicles)
					.outputDirectory("output/benchmark/parallel8_" + suffix)
					.build();

				var drtCfg = MultiModeDrtConfigGroup.get(c.getConfig()).getModalElements().iterator().next();
				DrtParallelInserterParams params = new DrtParallelInserterParams();
				params.setMaxPartitions(8);
				params.setMaxIterations(3);
				params.setCollectionPeriod(120);
				drtCfg.addParameterSet(params);

				DrtSpatialRequestFleetFilterParams drtSpatialRequestFleetFilterParams = new DrtSpatialRequestFleetFilterParams();
				drtCfg.addParameterSet(drtSpatialRequestFleetFilterParams);
				c.addOverridingQSimModule(new ParallelRequestInserterModule(drtCfg));
				c.addOverridingQSimModule(new SpatialFilterInsertionSearchQSimModule(drtCfg));

				c.run();
			});
		}

		runner.run();
	}
}
