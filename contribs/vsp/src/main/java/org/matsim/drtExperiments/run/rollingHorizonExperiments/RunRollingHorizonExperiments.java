//package org.matsim.drtExperiments.run.rollingHorizonExperiments;
//
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.application.MATSimAppCommand;
//import org.matsim.contrib.drt.analysis.afterSimAnalysis.DrtVehicleStoppingTaskWriter;
//import org.matsim.contrib.drt.extension.preplanned.optimizer.WaitForStopTask;
//import org.matsim.contrib.drt.extension.preplanned.run.PreplannedDrtControlerCreator;
//import org.matsim.contrib.drt.run.DrtConfigGroup;
//import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
//import org.matsim.contrib.dvrp.benchmark.DvrpBenchmarkTravelTimeModule;
//import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
//import org.matsim.contrib.dvrp.run.DvrpModule;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.population.PopulationUtils;
//import org.matsim.drtExperiments.run.modules.BypassTravelTimeMatrixModule;
//import org.matsim.drtExperiments.run.modules.LinearStopDurationModule;
//import org.matsim.drtExperiments.run.modules.OnlineAndOfflineDrtOperationModule;
//import org.matsim.drtExperiments.utils.DrtPerformanceQuantification;
//import picocli.CommandLine;
//
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.List;
//
//public class RunRollingHorizonExperiments implements MATSimAppCommand {
//    @CommandLine.Option(names = "--config", description = "path to config file", required = true)
//    private String configPath;
//
//    @CommandLine.Option(names = "--prebooked-trips", description = "path to pre-booked plans file", defaultValue = "all")
//    private String prebookedPlansFile;
//
//    @CommandLine.Option(names = "--output", description = "path to root directory", required = true)
//    private String rootDirectory;
//
//    @CommandLine.Option(names = "--prebooked-solver", defaultValue = "RUIN_AND_RECREATE", description = "Prebooked trips solver")
//    private OnlineAndOfflineDrtOperationModule.OfflineSolverType offlineSolver;
//
//    @CommandLine.Option(names = "--seed", description = "random seed", defaultValue = "0")
//    private int seed;
//
//    @CommandLine.Option(names = "--horizon", description = "horizons length of the solver", arity = "1..*", defaultValue = "1800")
//    private List<String> horizonsInput;
//
//    @CommandLine.Option(names = "--interval", description = "re-planning interval", arity = "1..*", defaultValue = "1800")
//    private List<String> intervalsInput;
//
//    @CommandLine.Option(names = "--iterations", description = "number of iterations for iterative offline solver. " +
//            "Separate with empty space", arity = "1..*", defaultValue = "0")
//    private List<String> iterationsInput;
//
//
//    public static void main(String[] args) {
//        new RunRollingHorizonExperiments().execute(args);
//    }
//
//    @Override
//    public Integer call() throws Exception {
//        if (!Files.exists(Path.of(rootDirectory))) {
//            Files.createDirectories(Path.of(rootDirectory));
//        }
//
//        // Prepare the result writer
//        DrtPerformanceQuantification resultsQuantification = new DrtPerformanceQuantification();
//        resultsQuantification.writeTitleForRollingHorizon(Path.of(rootDirectory));
//
//        // Run simulations
//        for (String iterationsString : iterationsInput) {
//            for (String horizonString : horizonsInput) {
//                for (String intervalString : intervalsInput) {
//                    int iterations = Integer.parseInt(iterationsString);
//                    double horizon = Double.parseDouble(horizonString);
//                    double interval = Double.parseDouble(intervalString);
//
//                    String outputDirectory = rootDirectory + "/run-iteration_" +
//                            iterationsString + "-horizon_" + horizonString + "-interval_" + intervalString;
//
//                    // Main run script
//                    long startTime = System.currentTimeMillis();
//
//                    Config config = ConfigUtils.loadConfig(configPath, new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
//                    MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
//                    config.controler().setOutputDirectory(outputDirectory);
//                    Controler controler = PreplannedDrtControlerCreator.createControler(config, false);
//                    controler.addOverridingModule(new DvrpModule(new DvrpBenchmarkTravelTimeModule()));
//
//                    // Read pre-booked trips (if not specified, then we assume all the trips are pre-booked)
//                    Population prebookedPlans;
//                    if (!prebookedPlansFile.equals("all")) {
//                        prebookedPlans = PopulationUtils.readPopulation(prebookedPlansFile);
//                    } else {
//                        prebookedPlans = controler.getScenario().getPopulation();
//                    }
//
//                    // Install the new DRT optimizer and the linear stop duration
//                    for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
//                        controler.addOverridingQSimModule(new OnlineAndOfflineDrtOperationModule(prebookedPlans, drtCfg,
//                                horizon, interval, iterations, false, seed, offlineSolver));
//                        controler.addOverridingModule(new LinearStopDurationModule(drtCfg));
//                        // If we are doing fully offline optimization, then no need to generate the standard travel time matrix
//                        if (prebookedPlansFile.equals("all")) {
//                            controler.addOverridingQSimModule(new BypassTravelTimeMatrixModule(drtCfg));
//                        }
//                    }
//                    controler.run();
//
//                    // Post-run Analysis
//                    // Compute time used
//                    long timeUsed = (System.currentTimeMillis() - startTime) / 1000;
//
//                    // Compute the score based on the objective function of the VRP solver
//                    resultsQuantification.analyzeRollingHorizon(Path.of(outputDirectory), timeUsed, Integer.toString(iterations), Double.toString(horizon), Double.toString(interval));
//                    resultsQuantification.writeResultEntryRollingHorizon(Path.of(rootDirectory));
//
//                    // Plot DRT stopping tasks
//                    new DrtVehicleStoppingTaskWriter(Path.of(outputDirectory)).addingCustomizedTaskToAnalyze(WaitForStopTask.TYPE).run(WaitForStopTask.TYPE);
//                }
//            }
//        }
//        return 0;
//    }
//}
