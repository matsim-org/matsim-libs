package org.matsim.drtExperiments.run;

import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.drt.analysis.afterSimAnalysis.DrtVehicleStoppingTaskWriter;
import org.matsim.contrib.drt.extension.preplanned.optimizer.WaitForStopTask;
import org.matsim.contrib.drt.extension.preplanned.run.PreplannedDrtControlerCreator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.benchmark.DvrpBenchmarkTravelTimeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.drtExperiments.run.modules.LinearStopDurationModule;
import org.matsim.drtExperiments.run.modules.OnlineAndOfflineDrtOperationModule;
import org.matsim.drtExperiments.utils.DrtPerformanceQuantification;
import picocli.CommandLine;

import java.nio.file.Path;

/**
 * This run script can be used to test the new online strategy. It is a special case for the mixed DRT operation.
 * The pre-booked trips are set to empty. Therefore, all the trips will be solved by the online solver. *
 * * *
 */
public class RunOnlineStrategy implements MATSimAppCommand {
    @CommandLine.Option(names = "--config", description = "path to config file", required = true)
    private String configPath;

    @CommandLine.Option(names = "--output", description = "path to output directory", required = true)
    private String outputDirectory;

    public static void main(String[] args) {
        new RunOnlineStrategy().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        long startTime = System.currentTimeMillis();

        Config config = ConfigUtils.loadConfig(configPath, new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
        MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
        config.controller().setOutputDirectory(outputDirectory);
        Controler controler = PreplannedDrtControlerCreator.createControler(config, false);
        controler.addOverridingModule(new DvrpModule(new DvrpBenchmarkTravelTimeModule()));

        // Install the new DRT optimizer and the linear stop duration
        Population prebookedPlans = PopulationUtils.createPopulation(ConfigUtils.createConfig()); // Set the pre-booked plans empty --> no pre-booked trips
        for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
            controler.addOverridingQSimModule(new OnlineAndOfflineDrtOperationModule(prebookedPlans, drtCfg,
                    86400, 86400, 0, false, 0, OnlineAndOfflineDrtOperationModule.OfflineSolverType.SEQ_INSERTION));
            controler.addOverridingModule(new LinearStopDurationModule(drtCfg));
        }
        controler.run();

        // Post-run Analysis
        // Compute time used
        long timeUsed = (System.currentTimeMillis() - startTime) / 1000;

        // Compute the score based on the objective function of the VRP solver
        DrtPerformanceQuantification resultsQuantification = new DrtPerformanceQuantification();
        resultsQuantification.analyzeRollingHorizon(Path.of(outputDirectory), timeUsed, "NA", "NA", "NA");
        resultsQuantification.writeResultsRollingHorizon(Path.of(outputDirectory));

        // Plot DRT stopping tasks
        new DrtVehicleStoppingTaskWriter(Path.of(outputDirectory)).addingCustomizedTaskToAnalyze(WaitForStopTask.TYPE).run(WaitForStopTask.TYPE);

        return 0;
    }
}
