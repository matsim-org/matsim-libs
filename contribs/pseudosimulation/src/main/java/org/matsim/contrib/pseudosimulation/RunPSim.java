/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *  * ***********************************************************************
 */

package org.matsim.contrib.pseudosimulation;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.common.diversitygeneration.planselectors.DiversityGeneratingPlansRemover;
import org.matsim.contrib.common.randomizedtransitrouter.RandomizedTransitRouterModule;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculatorSerializable;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculatorSerializable;
import org.matsim.contrib.pseudosimulation.distributed.listeners.controler.GenomeAnalysis;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformanceRecorder;
import org.matsim.contrib.pseudosimulation.mobsim.PSimFactory;
import org.matsim.contrib.pseudosimulation.mobsim.SwitchingMobsimProvider;
import org.matsim.contrib.pseudosimulation.replanning.DistributedPlanStrategyTranslationAndRegistration;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimStopStopTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimTravelTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimWaitTimeCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.router.TransitRouter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author pieterfourie
 *
 * A controler that alternates between the QSim and PSim for the mobility simulation.
 * Run this class with no arguments to get printed help listing current command line options.
 */
public class RunPSim {
    private final Config config;
    private final Scenario scenario;
    private final boolean Diversity;
    private Map<Id<Person>, Double> selectedPlanScoreMemory;
    private TransitPerformanceRecorder transitPerformanceRecorder;
    private MobSimSwitcher mobSimSwitcher;
    private Controler matsimControler;
    private boolean IntelligentRouters;
    private boolean TrackGenome;
    private boolean QuickReplanning;
    private int IterationsPerCycle = -1;
    private boolean FullTransitPerformanceTransmission;
    private WaitTimeCalculatorSerializable waitTimeCalculator;
    private StopStopTimeCalculatorSerializable stopStopTimeCalculator;
    private PSimTravelTimeCalculator carTravelTimeCalculator;
    private PlanCatcher plancatcher;
    private RunPSim(String[] args) throws ParseException {
        System.setProperty("matsim.preferLocalDtds", "true");
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("config")
                .hasArg(true)
                .withArgName("CONFIG.XML")
                .withDescription("Config file location")
                .isRequired(true)
                .create("c"));
        options.addOption("g", "genomeTracking", false, "Track plan genomes");
        options.addOption("I", "IntelligentRouters", false, "Intelligent routers");
        options.addOption("D", "Diversify", false, "Use the DiversityGeneratingPlansRemover");
        options.addOption(OptionBuilder.withLongOpt("appendString")
                .withDescription("Optional string without spaces to append to end of output directory name.")
                .hasArg(true)
                .withArgName("STRING")
                .create("a"));
        options.addOption(OptionBuilder.withLongOpt("iterationsPerCycle")
                .withDescription("Number of PSim iterations for every QSim iteration.")
                .hasArg(true)
                .withArgName("iters")
                .create("i"));
        options.addOption("f", "fullTransit", false, "Full transit performance transmission (more complete meta-model, more expensive)");
        options.addOption("q", "quickReplanning", false, "Quick replanning: each replanning strategy operates at 1/(number of PSim iters),  " +
                "effectively producing the same number of new plans per QSim iteration as a normal MATSim run," +
                "but having a multinomial distribution");
//        CommandLineParser parser = new BasicParser();
        CommandLineParser parser = new GnuParser();
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            printHelp(options);
            System.exit(1);
        }

        config = ConfigUtils.loadConfig(commandLine.getOptionValue("c"));
        scenario = ScenarioUtils.loadScenario(config);

        //command line option handling
        //--------------------------------------------------------------------------------------------------------------
        String appendString = commandLine.hasOption("a") ? commandLine.getOptionValue("a") : "";
        StringBuilder initialLogString = new StringBuilder("");

        if (commandLine.hasOption("i")) {
            try {
                IterationsPerCycle = Integer.parseInt(commandLine.getOptionValue("i"));
            } catch (NumberFormatException e) {
                System.err.println("iterationsPerCycle hould be integer...\n");
                printHelp(options);
                System.exit(1);
            }
            initialLogString.append("Running  " + IterationsPerCycle + " PSim iterations for every QSim iteration.\n");
        } else {
            initialLogString.append("No iterationsPerCycle defined, not running PSim\n");
        }

        if (commandLine.hasOption("f") || commandLine.hasOption("fullTransit")) {
            FullTransitPerformanceTransmission = true;
            initialLogString.append("Transmitting full transit performance to slaves.\n");
        } else {
            FullTransitPerformanceTransmission = false;
            initialLogString.append("Transmitting standard transit travel time structures only to slave\n");
        }
        if (commandLine.hasOption("q")) {
            QuickReplanning = true;
            initialLogString.append("QUICK replanning: each replanning strategy operates at 1/" + IterationsPerCycle + " (IterationsPerCycle), \n " +
                    "effectively producing the same number of new plans per QSim iteration as a normal MATSim run, but having a multinomial distribution\n");
        } else {
            QuickReplanning = false;
            initialLogString.append("NORMAL Replanning: each replanning strategy operates at the rate specified in the config for each PSim iteration\n");
        }
        if (commandLine.hasOption("g")) {
            TrackGenome = true;
            initialLogString.append("Tracking plan genomes and comparing QSim and Psim scores\n");
        } else {
            TrackGenome = false;
            initialLogString.append("No genome tracking\n");
        }
        if (commandLine.hasOption("I")) {
            IntelligentRouters = true;
            initialLogString.append("Using intelligent routers for transit and car\n");
        } else {
            IntelligentRouters = false;
            initialLogString.append("Using random routers for transit and car\n");
        }
        if (commandLine.hasOption("D")) {
            Diversity = true;
            initialLogString.append("Using experimental diversity generating plans remover with default options\n");
        } else {
            Diversity = false;
        }
        //--------------------------------------------------------------------------------------------------------------
        if (IterationsPerCycle > 0)
            initPSim();
        else {
            this.matsimControler = new Controler(scenario);
            if (TrackGenome) {
                DistributedPlanStrategyTranslationAndRegistration.TrackGenome = true;
                DistributedPlanStrategyTranslationAndRegistration.substituteStrategies(config, false, 1);
                DistributedPlanStrategyTranslationAndRegistration.registerStrategiesWithControler(this.matsimControler, null, false, 1);
            }
        }

        Logger logger = Logger.getLogger(this.getClass());
        logger.info(initialLogString);

        if (IntelligentRouters) {
            if (IterationsPerCycle < 0) {
                stopStopTimeCalculator =
                        new StopStopTimeCalculatorSerializable(scenario.getTransitSchedule(),
                                config.travelTimeCalculator().getTraveltimeBinSize(),
                                (int) (config.qsim().getEndTime() - config.qsim().getStartTime()));
                waitTimeCalculator =
                        new WaitTimeCalculatorSerializable(
                                scenario.getTransitSchedule(),
                                scenario.getConfig());
            }
            matsimControler.getEvents().addHandler(waitTimeCalculator);
            matsimControler.getEvents().addHandler(stopStopTimeCalculator);
            matsimControler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    bind(TransitRouter.class).toProvider(new TransitRouterEventsWSFactory(matsimControler.getScenario(),
                            waitTimeCalculator.getWaitTimes(),
                            stopStopTimeCalculator.getStopStopTimes()));
                }
            });
        } else {
            //randomized routing for car and transit
            final Builder disutilityFactory =
                    new Builder( TransportMode.car, config.planCalcScore() );
            matsimControler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    bindCarTravelDisutilityFactory().toInstance(disutilityFactory);
                }
            });
            disutilityFactory.setSigma(0.1);
            matsimControler.addOverridingModule(new RandomizedTransitRouterModule());
        }

        if (TrackGenome) {
            matsimControler.addControlerListener(new GenomeAnalysis(true, false, false));
        }

        matsimControler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                if (getConfig().strategy().getPlanSelectorForRemoval().equals("DiversityGeneratingPlansRemover")) {
                    bindPlanSelectorForRemoval().toProvider(DiversityGeneratingPlansRemover.Builder.class);
                }
            }
        });
        if (Diversity)
            matsimControler.getConfig().strategy().setPlanSelectorForRemoval("DiversityGeneratingPlansRemover");

        String outputDirectory = config.controler().getOutputDirectory();
        outputDirectory += "_ref" +
                (TrackGenome ? "_g" : "") +
                (IntelligentRouters ? "_I" : "") +
                (FullTransitPerformanceTransmission ? "_f" : "") +
                (Diversity ? "_D" : "") +
                appendString;
        config.controler().setOutputDirectory(outputDirectory);
        matsimControler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
    }

    public static void main(String args[]) throws ParseException {
        new RunPSim(args).run();
    }

    private static void printHelp(Options options) {
        String header = "The PSimControler takes the following options:\n\n";
        String footer = "";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("PSimControler", header, options, footer, true);
    }

    public MatsimServices getMatsimControler() {
        return matsimControler;
    }

    private void run() {
        matsimControler.run();
    }

    private void initPSim() {
        //The following line will make the controler use the events manager that doesn't check for event order.
        //This is essential for pseudo-simulation as the PSim module generates events on a person-basis,
        //not a system basis
        config.parallelEventHandling().setSynchronizeOnSimSteps(false);
        config.parallelEventHandling().setNumberOfThreads(1);
        this.plancatcher = new PlanCatcher();
        DistributedPlanStrategyTranslationAndRegistration.TrackGenome = TrackGenome;
        DistributedPlanStrategyTranslationAndRegistration.substituteStrategies(config, QuickReplanning, IterationsPerCycle);
        this.matsimControler = new Controler(scenario);
        DistributedPlanStrategyTranslationAndRegistration.registerStrategiesWithControler(this.matsimControler, plancatcher, QuickReplanning, IterationsPerCycle);
        mobSimSwitcher = new MobSimSwitcher(IterationsPerCycle);
        matsimControler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bindMobsim().toProvider(SwitchingMobsimProvider.class);
            }
        });
        matsimControler.addControlerListener(mobSimSwitcher);
        matsimControler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(MobSimSwitcher.class).toInstance(mobSimSwitcher);
            }
        });
        matsimControler.addControlerListener(new QSimScoreWriter(this.matsimControler));

        this.carTravelTimeCalculator = new PSimTravelTimeCalculator(matsimControler.getScenario().getNetwork(),
                config.travelTimeCalculator(), (int) (config.qsim().getEndTime() - config.qsim().getStartTime()), mobSimSwitcher);
        matsimControler.getEvents().addHandler(carTravelTimeCalculator);
        if (config.transit().isUseTransit()) {
            if (FullTransitPerformanceTransmission) {
                transitPerformanceRecorder = new TransitPerformanceRecorder(matsimControler.getScenario(), matsimControler.getEvents(), mobSimSwitcher);
            }
            this.waitTimeCalculator = new PSimWaitTimeCalculator(
                    matsimControler.getScenario().getTransitSchedule(),
                    config,
                    mobSimSwitcher);
            matsimControler.getEvents().addHandler(waitTimeCalculator);
            this.stopStopTimeCalculator = new PSimStopStopTimeCalculator(
                    matsimControler.getScenario().getTransitSchedule(),
                    matsimControler
                            .getConfig().travelTimeCalculator()
                            .getTraveltimeBinSize(),
                    (int) (config.qsim().getEndTime() - config.qsim().getStartTime()),
                    mobSimSwitcher);
            matsimControler.getEvents().addHandler(stopStopTimeCalculator);
        }
        config.linkStats().setWriteLinkStatsInterval(config.linkStats().getWriteLinkStatsInterval() * IterationsPerCycle);
        config.controler().setCreateGraphs(false);
        config.controler().setWriteEventsInterval(config.controler().getWriteEventsInterval() * IterationsPerCycle);
        config.controler().setWritePlansInterval(config.controler().getWritePlansInterval() * IterationsPerCycle);
        config.controler().setWriteSnapshotsInterval(config.controler().getWriteSnapshotsInterval() * IterationsPerCycle);
        Logger.getLogger(this.getClass()).warn("Please note: this script violates the matsim convention that the config output should be useable "
        		+ "as input to another run.   Instead, the write invervals would be multiplied every time this is done.  kai, may'15");
    }




    /**
     * @author fouriep
     *         <p/>
     *         Switches between two mobility simulations, the first being the
     *         expensive one, the second being cheap.
     *         <p/>
     *         Switches between the expensive sim and the cheap sim according to the
     *         config parameters used in the constructore. Always executes the
     *         expensive sim at the last iteration.
     *         <p/>
     *         Raises a static boolean flag for others to read if it's currently on
     *         an expensive sim; this flag defaults to true if the mobsimswitcher is
     *         not instantiated
     */

    public class MobSimSwitcher implements IterationEndsListener,
            IterationStartsListener, BeforeMobsimListener, PSimDataProvider {
        private final static String START_RATE = "startRate";
        private final static String END_RATE = "endRate";
        private final static String START_ITER = "startIter";
        private final static String END_ITER = "endIter";
        private final static String INCREASE_EVERY_N = "increaseEveryNExpensiveIters";
        private final ArrayList<Integer> qsimIters = new ArrayList<>();
        private final Logger log = Logger.getLogger(this.getClass());
        private boolean isQSimIteration = true;
        private int increaseEveryNExpensiveIters = 1;
        private int qsimIterCount = 0;
        private int cheapIterCount = 0;
        private int currentRate = 0;
        private int endRate = 0;
        private int startIter;
        private int endIter;
        private Map<Id<Person>, Double> selectedPlanScoreMemory;
        private PSimFactory pSimFactory;


        public MobSimSwitcher(int overridingRate) {
            pSimFactory = new PSimFactory();
            currentRate = overridingRate;
            endRate = overridingRate;
            endIter = matsimControler.getConfig().controler().getLastIteration();
            startIter = matsimControler.getConfig().controler().getFirstIteration();
        }

        //TODO: add a config group to enable all this stuff again
        private MobSimSwitcher() {
            pSimFactory = new PSimFactory();
            int startRate = 0;
            if (matsimControler.getConfig().getParam("MobSimSwitcher", START_RATE) != null)
                startRate = Math.max(
                        0,
                        Integer.parseInt(matsimControler.getConfig().getParam("MobSimSwitcher",
                                START_RATE)));
            if (matsimControler.getConfig().getParam("MobSimSwitcher", END_RATE) != null)
                endRate = Math.max(
                        0,
                        Integer.parseInt(matsimControler.getConfig().getParam("MobSimSwitcher",
                                END_RATE)));
            currentRate = startRate;

            startIter = matsimControler.getConfig().controler().getFirstIteration();
            if (matsimControler.getConfig().getParam("MobSimSwitcher", START_ITER) != null)
                startIter = Math.max(
                        startIter,
                        Integer.parseInt(matsimControler.getConfig().getParam("MobSimSwitcher",
                                START_ITER)));
            endIter = matsimControler.getConfig().controler().getLastIteration();
            if (matsimControler.getConfig().getParam("MobSimSwitcher", END_ITER) != null)
                endIter = Math.min(
                        endIter,
                        Integer.parseInt(matsimControler.getConfig().getParam("MobSimSwitcher",
                                END_ITER)));
            if (matsimControler.getConfig().getParam("MobSimSwitcher", INCREASE_EVERY_N) != null)
                increaseEveryNExpensiveIters = Math.max(
                        increaseEveryNExpensiveIters,
                        Integer.parseInt(matsimControler.getConfig().getParam("MobSimSwitcher",
                                INCREASE_EVERY_N)));

        }

        public boolean isQSimIteration() {
            return isQSimIteration;
        }

        public ArrayList<Integer> getQSimIters() {
            return qsimIters;
        }

        @Override
        public void notifyIterationStarts(IterationStartsEvent event) {
            if (determineIfQSimIter(event.getIteration())) {
                log.warn("Running full queue simulation");

            } else {
                log.info("Running PSim");
                plancatcher.init();
            }
        }

        private boolean determineIfQSimIter(int iteration) {

            if (iteration == matsimControler.getConfig().controler().getLastIteration()) {
                isQSimIteration = true;
                return isQSimIteration;
            }
            if (iteration < endIter && qsimIterCount > 0) {
                if (qsimIterCount >= increaseEveryNExpensiveIters
                        && iteration > startIter) {
                    log.warn("Increasing rate of switching between QSim and PSim");
                    if (currentRate < endRate) {
                        currentRate++;
                    }
                    qsimIterCount = 0;
                }
            }
            if (isQSimIteration && cheapIterCount == 0
                    && iteration > startIter) {
                isQSimIteration = false;
                cheapIterCount++;
                return isQSimIteration;
            }
            if (cheapIterCount >= currentRate - 1) {
                isQSimIteration = true;
                qsimIters.add(iteration);
                cheapIterCount = 0;
                qsimIterCount++;
                return isQSimIteration;
            }
            if (isQSimIteration) {
                qsimIters.add(iteration);
                qsimIterCount++;
            } else {
                cheapIterCount++;

            }
            return isQSimIteration;
        }

        @Override
        public void notifyBeforeMobsim(BeforeMobsimEvent event) {
            //only for psim iterations
            if(mobSimSwitcher.isQSimIteration())
                return;

            Scenario scenario = matsimControler.getScenario();
            selectedPlanScoreMemory = new HashMap<>(scenario.getPopulation().getPersons().size());

            for (Person person : scenario.getPopulation().getPersons().values()) {
                selectedPlanScoreMemory.put(person.getId(), person.getSelectedPlan().getScore());
            }
            for (Plan plan : plancatcher.getPlansForPSim()) {
                selectedPlanScoreMemory.remove(plan.getPerson().getId());
            }
            pSimFactory.setPlans(plancatcher.getPlansForPSim());
            if (matsimControler.getConfig().transit().isUseTransit() ) {
                if(FullTransitPerformanceTransmission)
                    pSimFactory.setTransitPerformance(transitPerformanceRecorder.getTransitPerformance());
                else{
                    pSimFactory.setStopStopTime(stopStopTimeCalculator.getStopStopTimes());
                    pSimFactory.setWaitTime(waitTimeCalculator.getWaitTimes());
                }
            }
            pSimFactory.setTravelTime(carTravelTimeCalculator.getLinkTravelTimes());
        }

        @Override
        public void notifyIterationEnds(IterationEndsEvent event) {
            if(mobSimSwitcher.isQSimIteration())
                return;
            Iterator<Map.Entry<Id<Person>, Double>> iterator = selectedPlanScoreMemory.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Id<Person>, Double> entry = iterator.next();
                matsimControler.getScenario().getPopulation().getPersons().get(entry.getKey()).getSelectedPlan().setScore(entry.getValue());
            }
        }

        @Override
        public StopStopTime getStopStopTime() {
            return stopStopTimeCalculator.getStopStopTimes();
        }

        @Override
        public WaitTime getWaitTime() {
            return waitTimeCalculator.getWaitTimes();
        }

        @Override
        public TransitPerformance getTransitPerformance() {
            return transitPerformanceRecorder.getTransitPerformance();
        }

        @Override
        public PlanCatcher getPlanCatcher() {
            return plancatcher;
        }

        @Override
        public TravelTime getTravelTime() {
            return carTravelTimeCalculator.getLinkTravelTimes();
        }
    }


    class QSimScoreWriter implements IterationEndsListener,
            ShutdownListener, StartupListener {
        final private static int INDEX_WORST = 0;
        final private static int INDEX_BEST = 1;
        final private static int INDEX_AVERAGE = 2;
        final private static int INDEX_EXECUTED = 3;
        private final MatsimServices controler;
        private BufferedWriter out;

        public QSimScoreWriter(MatsimServices controler) {
            super();
            this.controler = controler;

        }

        @Override
        public void notifyIterationEnds(IterationEndsEvent event) {
            if (!mobSimSwitcher.isQSimIteration() || event.getIteration() == controler.getConfig().controler().getLastIteration()) {
                return;
            }
            ArrayList<Integer> expensiveIters = mobSimSwitcher.getQSimIters();
            int index = expensiveIters.size();
            double[][] history = controler.getScoreStats().getHistory();
            int idx = event.getIteration() - controler.getConfig().controler().getFirstIteration();
            try {
                out.write(event.getIteration() + "\t"
                        + history[INDEX_EXECUTED][idx] + "\t"
                        + history[INDEX_WORST][idx] + "\t"
                        + history[INDEX_AVERAGE][idx] + "\t"
                        + history[INDEX_BEST][idx] + "\n");
                out.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // create chart when data of more than one iteration is available.
            if (index < 2) {
                return;
            }
            XYLineChart chart = new XYLineChart("QSIM Score Statistics",
                    "iteration", "score");
            double[] iterations = new double[index];
            for (int i = 0; i < index; i++) {
                iterations[i] = i + controler.getConfig().controler().getFirstIteration();
            }
            double[] values = new double[index];
            double[] fullhist = new double[event.getIteration()
                    - controler.getConfig().controler().getFirstIteration() + 1];
            int[] series = {INDEX_WORST, INDEX_BEST, INDEX_AVERAGE, INDEX_EXECUTED};
            String[] seriesNames = {"avg. worst score", "avg. best score",
                    "avg. of plans' average score", "avg. executed score"};
            for (int s = 0; s < series.length; s++) {
                System.arraycopy(history[series[s]], 0, fullhist, 0,
                        fullhist.length);
                int valuecounter = 0;
                for (int i : expensiveIters) {
                    values[valuecounter++] = fullhist[i
                            - controler.getConfig().controler().getFirstIteration()];
                }
                chart.addSeries(seriesNames[s], iterations, values);

            }


            chart.addMatsimLogo();
            chart.saveAsPng(controler.getControlerIO().getOutputPath()
                    + "/qsimstats.png", 1200, 800);

        }

        @Override
        public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
            try {
                this.out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void notifyStartup(StartupEvent event) {
            String fileName = controler.getControlerIO().getOutputPath()
                    + "/qsimstats.txt";
            this.out = IOUtils.getBufferedWriter(fileName);

            try {
                this.out.write("ITERATION\tavg. EXECUTED\tavg. WORST\tavg. AVG\tavg. BEST\n");
                this.out.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

        }
    }
}
