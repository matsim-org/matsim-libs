package org.matsim.drtExperiments.run.rollingHorizonExperiments;

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
//import org.matsim.drtExperiments.run.modules.BypassTravelTimeMatrixModule;
//import org.matsim.drtExperiments.run.modules.LinearStopDurationModule;
import org.matsim.drtExperiments.run.modules.OfflineDrtOperationModule;
import org.matsim.drtExperiments.utils.DrtPerformanceQuantification;
import picocli.CommandLine;

import java.nio.file.Path;

public class RunDrtWithPrebooking implements MATSimAppCommand {
    @CommandLine.Option(names = "--config", description = "path to config file", required = true)
    private String configPath;

    @CommandLine.Option(names = "--prebooked-trips", description = "path to pre-booked plans file", defaultValue = "all")
    private String prebookedPlansFile;

    @CommandLine.Option(names = "--output", description = "path to output directory", required = true)
    private String outputDirectory;

    @CommandLine.Option(names = "--horizon", description = "horizon length of the solver", defaultValue = "1800")
    private double horizon;

    @CommandLine.Option(names = "--interval", description = "re-planning interval", defaultValue = "1800")
    private double interval;

    @CommandLine.Option(names = "--prebooked-solver", defaultValue = "SEQ_INSERTION", description = "Prebooked trips solver")
    private OfflineDrtOperationModule.OfflineSolverType offlineSolver;

    @CommandLine.Option(names = "--iterations", description = "number of iterations for iterative offline solver", defaultValue = "0")
    private int iterations;

    @CommandLine.Option(names = "--seed", description = "random seed", defaultValue = "0")
    private int seed;

    public static void main(String[] args) {
        if (args==null || args.length==0 ){
            args = new String[]{
                    "--output", "output"
                    , "--config", "scenarios/mielec/mielec_drt_config.xml"
            };
        }
            new RunDrtWithPrebooking().execute( args );
    }

    @Override
    public Integer call() throws Exception {
        long startTime = System.currentTimeMillis();

        Config config = ConfigUtils.loadConfig(configPath, new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
        MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
        config.controller().setOutputDirectory(outputDirectory);
        Controler controler = PreplannedDrtControlerCreator.createControler(config, false );
        controler.addOverridingModule(new DvrpModule(new DvrpBenchmarkTravelTimeModule()));

        // Read pre-booked trips (if not specified, then we assume all the trips are pre-booked)
        Population prebookedPlans;
        if (!prebookedPlansFile.equals("all")) {
            prebookedPlans = PopulationUtils.readPopulation(prebookedPlansFile);
        } else {
            prebookedPlans = controler.getScenario().getPopulation();
        }

        // Install the new DRT optimizer and the linear stop duration
        for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
            controler.addOverridingQSimModule(new OfflineDrtOperationModule(prebookedPlans, drtCfg,
                    horizon, interval, iterations, false, seed, offlineSolver));
//            controler.addOverridingModule(new LinearStopDurationModule(drtCfg));
            // If we are doing fully offline optimization, then no need to generate the standard travel time matrix
            if (prebookedPlansFile.equals("all")) {
//                controler.addOverridingQSimModule(new BypassTravelTimeMatrixModule(drtCfg));
                throw new RuntimeException( "only implemented in drt-operation-experiments" );
            }
        }
        controler.run();

        // Post-run Analysis
        // Compute time used
        long timeUsed = (System.currentTimeMillis() - startTime) / 1000;

        // Compute the score based on the objective function of the VRP solver
        DrtPerformanceQuantification resultsQuantification = new DrtPerformanceQuantification();
        resultsQuantification.analyzeRollingHorizon(Path.of(outputDirectory), timeUsed, Integer.toString(iterations), Double.toString(horizon), Double.toString(interval));
        resultsQuantification.writeResultsRollingHorizon(Path.of(outputDirectory));

        // Plot DRT stopping tasks
        new DrtVehicleStoppingTaskWriter(Path.of(outputDirectory)).addingCustomizedTaskToAnalyze( WaitForStopTask.TYPE ).run(WaitForStopTask.TYPE );

        return 0;
    }

}
