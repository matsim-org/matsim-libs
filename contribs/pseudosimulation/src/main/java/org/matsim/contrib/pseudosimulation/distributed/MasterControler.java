package org.matsim.contrib.pseudosimulation.distributed;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculatorSerializable;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculatorSerializable;
import org.matsim.contrib.pseudosimulation.distributed.instrumentation.scorestats.SlaveScoreStats;
import org.matsim.contrib.pseudosimulation.distributed.listeners.controler.GenomeAnalysis;
import org.matsim.contrib.pseudosimulation.distributed.listeners.controler.SlaveScoreWriter;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformanceRecorder;
import org.matsim.contrib.pseudosimulation.replanning.DistributedPlanStrategyTranslationAndRegistration;
import org.matsim.contrib.pseudosimulation.util.CollectionUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class MasterControler implements AfterMobsimListener, ShutdownListener, StartupListener, IterationStartsListener {
    public static final Logger masterLogger = Logger.getLogger(MasterControler.class);
    private static CommandLine commandLine;
    private static Options options;
    private static StringBuilder masterInitialLogString;
    private static String appendString;
    private static double MasterMutationRate = -1;
    private static double SlaveMutationRate = -1;
    private final HashMap<String, Plan> newPlans = new HashMap<>();
    private final Hydra hydra;
    private int numSlaves = 1;
    private static double MasterBorrowingRate = 0.6667;
    private int innovationEndsAtIter = -1;
    private int slaveNumberOfPlans=3;

    public static boolean isTrackGenome() {
        return TrackGenome;
    }

    private static boolean TrackGenome;
    private static boolean IntelligentRouters;
    private long bytesPerPerson;
    private Scenario scenario;
    private long scenarioMemoryUse;
    private long bytesPerPlan;
    private static boolean initialRoutingOnSlaves = false;
    private static int numberOfPSimIterations = 5;
    private Config config;
    private Controler matsimControler;
    private TreeMap<Integer, SlaveHandler> slaveHandlerTreeMap;
    private WaitTimeCalculatorSerializable waitTimeCalculator;
    private StopStopTimeCalculatorSerializable stopStopTimeCalculator;
    private TransitPerformanceRecorder transitPerformanceRecorder;
    private SerializableLinkTravelTimes linkTravelTimes;
    private AtomicInteger numThreads = new AtomicInteger(0);
    private List<PersonSerializable> personPool;
    private static int loadBalanceInterval = 5;
    private boolean somethingWentWrong = false;
    private static int socketNumber = 12345;
    public static double planAllocationLimiter = 10.0;
    public static final long bytesPerSlaveBuffer = (long) 2e8;
    public int slaveUniqueNumber = 0;
    SlaveScoreStats slaveScoreStats;

    public enum SimulationMode {SERIAL, PARALLEL}

    public static SimulationMode SelectedSimulationMode = SimulationMode.PARALLEL;

    public static boolean QuickReplanning;
    private static boolean FullTransitPerformanceTransmission;

    /**
     * value between 0 and 1; increasing it increases the dampening effect of preventing
     * large transfers of persons during load balance iterations
     */
    private static final double loadBalanceDampeningFactor = 0.4;
    private int currentIteration = -1;

    private static void printHelp() {
        String header = "The MasterControler takes the following options:\n\n";
        String footer = "";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("MasterControler", header, options, footer, true);
    }

    private void parseCommandLineOptions(String[] args) {
        System.setProperty("matsim.preferLocalDtds", "true");
        options = new Options();
        options.addOption(OptionBuilder.withLongOpt("config")
                .hasArg(true)
                .withArgName("CONFIG.XML")
                .withDescription("Config file location")
                .isRequired(true)
                .create("c"));
        options.addOption(OptionBuilder.withLongOpt("port")
                .withDescription("Port number of MasterControler")
                .hasArg(true)
                .withArgName("port")
                .create("p"));
        options.addOption(OptionBuilder.withLongOpt("iterationsPerCycle")
                .withDescription("Number of PSim iterations for every QSim iteration.")
                .hasArg(true)
                .withArgName("iters")
                .create("i"));
        options.addOption(OptionBuilder.withLongOpt("slavePlans")
                .withDescription("Number of plans on slave.")
                .hasArg(true)
                .withArgName("slavePlans")
                .create("sp"));
        options.addOption(OptionBuilder.withLongOpt("masterMutationRate")
                .withDescription("The proportion of mutation allocated in replanning on master. " +
                        "Defaults to whatever is specified in the config.")
                .hasArg(true)
                .withArgName("0 ≤ x ≤ 1")
                .create("mr"));
        options.addOption(OptionBuilder.withLongOpt("masterBorrowingRate")
                .withDescription("The proportion of plans borrowed from slaves. " +
                        "Defaults to half.")
                .hasArg(true)
                .withArgName("0 ≤ x ≤ 1")
                .create("mb"));
        options.addOption(OptionBuilder.withLongOpt("slaveMutationRate")
                .withDescription("The proportion of mutation allocated in replanning on slaves. " +
                        "Defaults to whatever is specified in the config.")
                .hasArg(true)
                .withArgName("0 ≤ x ≤ 1")
                .create("sr"));
        options.addOption(OptionBuilder.withLongOpt("numberOfSlaves")
                .withDescription("Number of slaves to distribute to.")
                .hasArg(true)
                .withArgName("number")
                .create("n"));
        options.addOption(OptionBuilder.withLongOpt("dumpSlavePlansInterval")
                .withDescription("Number of iterations between dumping plans from all slaves. " +
                        "Defaults to the value in the config (so disabled if set to zero).")
                .hasArg(true)
                .withArgName("iters")
                .create("d"));
        options.addOption(OptionBuilder.withLongOpt("loadBalanceInterval")
                .withDescription("Number of iterations between load balancing. Default = 5")
                .hasArg(true)
                .withArgName("iters")
                .create("l"));
        options.addOption(OptionBuilder.withLongOpt("appendString")
                .withDescription("Optional string without spaces to append to end of output directory name.")
                .hasArg(true)
                .withArgName("STRING")
                .create("a"));

        options.addOption("m", "mode", false, "A switch to change SimulationMode from PARALLEL (PSim execution during QSim execution) " +
                "to SERIAL (PSim waits for QSim to finish and vice-versa.)");
        options.addOption("r", "routeOnSlaves", false, "Perform initial routing of plans on slaves.");
        options.addOption("f", "fullTransit", false, "Full transit performance transmission (more complete meta-model, more expensive)");
        options.addOption("q", "quickReplanning", false, "Quick replanning: each replanning strategy operates at 1/(number of PSim iters),  " +
                "effectively producing the same number of new plans per QSim iteration as a normal MATSim run," +
                "but having more mutations per plan on average");
        options.addOption("g", "genomeTracking", false, "Track plan genomes");
        options.addOption("I", "IntelligentRouters", false, "Intelligent routers");
        CommandLineParser parser = new BasicParser();
        commandLine = null;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            printHelp();
            System.exit(1);
        }
        appendString = (commandLine.hasOption("appendString") || commandLine.hasOption("a")) ? commandLine.getOptionValue("a") : "";

        masterInitialLogString = new StringBuilder();

        if (commandLine.hasOption("c")) {
            config = ConfigUtils.loadConfig(commandLine.getOptionValue("c"));
        } else {
            masterLogger.error("Config file not specified");
            printHelp();
            System.exit(1);
        }


        if (commandLine.hasOption("n"))
            numSlaves = Integer.parseInt(commandLine.getOptionValue("n"));
        else {
            masterInitialLogString.append("Unspecified number of slaves. Will start with the default of a single slave.\n");
        }


        if (commandLine.hasOption("f") || commandLine.hasOption("fullTransit")) {
            FullTransitPerformanceTransmission = true;
            masterInitialLogString.append("Transmitting full transit performance to slaves.\n");
        } else {
            FullTransitPerformanceTransmission = false;
            masterInitialLogString.append("Transmitting standard transit travel time structures only to slave\n");
        }
        if (commandLine.hasOption("m")) {
            SelectedSimulationMode = SimulationMode.SERIAL;
            masterInitialLogString.append("Running in SERIAL mode (PSim waits for QSim to finish and vice-versa).\n");
        } else {
            masterInitialLogString.append("Running in PARALLEL mode (PSim execution during QSim execution).\n");
        }
        if (commandLine.hasOption("i")) {
            numberOfPSimIterations = Integer.parseInt(commandLine.getOptionValue("i"));
            masterInitialLogString.append("Running  " + numberOfPSimIterations + " PSim iterations for every QSim iteration run on the master\n");
        } else {
            masterInitialLogString.append("Unspecified number of PSim iterations for every QSim iteration run on the master.\n");
            masterInitialLogString.append("Using default value of " + numberOfPSimIterations);
        }

        if (commandLine.hasOption("q")) {
            QuickReplanning = true;
            masterInitialLogString.append("QUICK replanning: each replanning strategy operates at 1/" + numberOfPSimIterations + " (numberOfPSimIterations), \n " +
                    "effectively producing the same number of new plans per QSim iteration as a normal MATSim run, but having a multinomial distribution\n");
        } else {
            QuickReplanning = false;
            masterInitialLogString.append("NORMAL Replanning: each replanning strategy operates at the rate specified in the config for each PSim iteration\n");
        }

        if (commandLine.hasOption("mr")) {
            MasterMutationRate = Double.parseDouble(commandLine.getOptionValue("mr"));
            masterInitialLogString.append("MASTER mutation rate set to" + MasterMutationRate + " \n");
        }

        if (commandLine.hasOption("mb")) {
            MasterBorrowingRate = Double.parseDouble(commandLine.getOptionValue("mb"));
            masterInitialLogString.append("MASTER borrowing rate set to" + MasterBorrowingRate + " \n");
        }

        if (commandLine.hasOption("sp")) {
            slaveNumberOfPlans = Integer.parseInt(commandLine.getOptionValue("sp"));
            masterInitialLogString.append("Number of plans per agent on slaves " + slaveNumberOfPlans + " \n");
        }

        if (commandLine.hasOption("sr")) {
            SlaveMutationRate = Double.parseDouble(commandLine.getOptionValue("sr"));
            masterInitialLogString.append("SLAVE mutation rate set to" + SlaveMutationRate + " \n");
        }

        if (commandLine.hasOption("g")) {
            TrackGenome = true;
            masterInitialLogString.append("Tracking plan genomes and comparing QSim and Psim scores\n");
        } else {
            TrackGenome = false;
            masterInitialLogString.append("No genome tracking\n");
        }

        if (commandLine.hasOption("I")) {
            IntelligentRouters = true;
            masterInitialLogString.append("Using intelligent routers for transit and car\n");
        } else {
            IntelligentRouters = false;
            masterInitialLogString.append("Using random routers for transit and car\n");
        }
        if (commandLine.hasOption("p"))
            try {
                socketNumber = Integer.parseInt(commandLine.getOptionValue("p"));
            } catch (NumberFormatException e) {
                masterLogger.error("Port number should be integer\n");
                printHelp();
                System.exit(1);
            }
        else {
            masterInitialLogString.append("Will accept connections on default port number 12345\n");
        }
        if (commandLine.hasOption("l"))
            try {
                loadBalanceInterval = Integer.parseInt(commandLine.getOptionValue("l"));
                masterInitialLogString.append("Will perform LOAD BALANCING every " + loadBalanceInterval + " iterations\n");
            } catch (NumberFormatException e) {
                masterLogger.error("loadBalanceInterval number should be integer\n");
                printHelp();
                System.exit(1);
            }
        else {
            masterInitialLogString.append("Will perform LOAD BALANCING every 5 iterations as per default\n");
        }
        if (commandLine.hasOption("r")) {
            masterInitialLogString.append("ROUTING initial plans on slaves.\n");
            initialRoutingOnSlaves = true;
        }

    }

    public MasterControler(String[] args) throws NumberFormatException, IOException, InterruptedException {
        parseCommandLineOptions(args);

        slaveHandlerTreeMap = new TreeMap<>();
        slaveScoreStats = new SlaveScoreStats(config);

        if (MasterMutationRate <= 0 ) {
            masterInitialLogString.append("No or bad (>1) master mutation rate specified. Assuming the default of nearly zero, " +
                    "and 2/3 of plans in a qsim coming from slaves, unless borrowing rate has been set already.");
            MasterMutationRate = 0.0001;
        }
        setReplanningWeights(config, MasterMutationRate, MasterBorrowingRate);

//        register initial number of slaves
        ServerSocket writeServer = new ServerSocket(socketNumber);
        for (int i = 0; i < numSlaves; i++) {
            Socket socket = writeServer.accept();
            System.out.println("Slave " + (i + 1) + " out of an initial " + numSlaves + " accepted.\n");
            SlaveHandler slaveHandler = new SlaveHandler(socket, slaveUniqueNumber);
            slaveHandlerTreeMap.put(slaveUniqueNumber, slaveHandler);
            //order is important
            initializeSlave(slaveHandler, slaveUniqueNumber++, initialRoutingOnSlaves);
            Thread.sleep(10);
        }
        writeServer.close();
        masterInitialLogString.append("MASTER accepted minimum number of incoming connections. All further slaves will be registered on the Hydra.\n");
        hydra = new Hydra();
        Thread hydraThread = new Thread(hydra);
        hydraThread.setName("HYDRA");
        hydraThread.start();


        scenario = ScenarioUtils.loadScenario(config);
//        determine the memory use of the population for some initial load balancing
        MemoryUsageCalculator memoryUsageCalculator = new MemoryUsageCalculator();
        scenarioMemoryUse = memoryUsageCalculator.getMemoryUse();
        long currentPopulationMemoryUse = memoryUsageCalculator.getMemoryUse() - scenarioMemoryUse;
        bytesPerPlan = Math.max(1000, currentPopulationMemoryUse / getTotalNumberOfPlansOnMaster());
        bytesPerPerson = bytesPerPlan;
        masterInitialLogString.append("Estimated memory use per plan is " + bytesPerPlan + " bytes\n");

        matsimControler = new Controler(scenario);
        matsimControler.addControlerListener(new SlaveScoreWriter(this));

        //split the population to be sent to the slaveHandlers

        double[] totalIterationTime = new double[numSlaves];
        int[] personsPerSlave = new int[numSlaves];
        long[] usedMemoryPerSlave = new long[numSlaves];
        long[] maxMemoryPerSlave = new long[numSlaves];
        int j = 0;
        for (int i : slaveHandlerTreeMap.keySet()) {
            totalIterationTime[j] = 1 / (double) slaveHandlerTreeMap.get(i).numThreadsOnSlave;
            personsPerSlave[j] = scenario.getPopulation().getPersons().size() / numSlaves;
            usedMemoryPerSlave[j] = slaveHandlerTreeMap.get(i).usedMemory;
            maxMemoryPerSlave[j] = slaveHandlerTreeMap.get(i).maxMemory;
            j++;
        }
        int[] initialWeights = getSlaveTargetPopulationSizes(totalIterationTime, personsPerSlave, maxMemoryPerSlave, usedMemoryPerSlave,
                bytesPerPlan, bytesPerPerson, 0.0, scenario.getPopulation().getPersons().size());
        List<Person>[] personSplit = (List<Person>[]) CollectionUtils.split(scenario.getPopulation().getPersons().values(), initialWeights);
        j = 0;
        for (int i : slaveHandlerTreeMap.keySet()) {
            List<PersonSerializable> personsToSend = new ArrayList<>();
            for (Person p : personSplit[j]) {
                personsToSend.add(new PersonSerializable(p));
            }
            slaveHandlerTreeMap.get(i).slavePersonPool = personsToSend;
            j++;
        }


        if (config.transit().isUseTransit()) {
            waitTimeCalculator = new WaitTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(), config.travelTimeCalculator().getTraveltimeBinSize(),
                    (int) (config.qsim().getEndTime() - config.qsim().getStartTime()));
            matsimControler.getEvents().addHandler(waitTimeCalculator);
            stopStopTimeCalculator = new StopStopTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(),
                    config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config.qsim()
                    .getEndTime() - config.qsim().getStartTime()));
            matsimControler.getEvents().addHandler(stopStopTimeCalculator);
            //tell PlanSerializable to record transit routes
            PlanSerializable.isUseTransit = true;
            if (FullTransitPerformanceTransmission) {
                transitPerformanceRecorder = new TransitPerformanceRecorder(scenario, matsimControler.getEvents());
            }
        }

        matsimControler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addPlanStrategyBinding("ReplacePlanFromSlave").toProvider(new ReplacePlanFromSlaveFactory(newPlans));
            }
        });
        matsimControler.addControlerListener(this);


        String outputDirectory = config.controler().getOutputDirectory();
        outputDirectory += "_P" + numberOfPSimIterations +
                String.format("_mr%.3f_mb%.3f_sr%.3f_sp%03d", MasterMutationRate, MasterBorrowingRate, SlaveMutationRate,slaveNumberOfPlans) +
                (SelectedSimulationMode.equals(SimulationMode.SERIAL) ? "_m" : "") +
                (FullTransitPerformanceTransmission ? "_f" : "") +
                (TrackGenome ? "_g" : "") +
                (QuickReplanning ? "_q" : "") +
                (IntelligentRouters ? "_I" : "") +
                appendString;
        config.controler().setOutputDirectory(outputDirectory);
        matsimControler.getConfig().controler().setOverwriteFileSetting(
                OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
//                true ?
//                        OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
//                        OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
        if (TrackGenome) {
            matsimControler.addControlerListener(new GenomeAnalysis(true, false, true));
        }

    }

    /**
     * Experimental or parameter optimization. Probably won't work with subpopulations
     *
     * @param config
     * @param masterMutationRate
     * @param borrowingRate
     */
    private void setReplanningWeights(Config config, double masterMutationRate, double borrowingRate) {
        int disableAfterIteration = config.controler().getLastIteration();
        int maximumIterationForMutationDisabling = -1;
        if (borrowingRate + masterMutationRate >= 1) {
            borrowingRate = 0.9999 * borrowingRate / (masterMutationRate + borrowingRate);
            masterMutationRate = 0.9999 * masterMutationRate / (masterMutationRate + borrowingRate);
        }
        List<StrategyConfigGroup.StrategySettings> strategySettings = new ArrayList<>();
        strategySettings.addAll(config.strategy().getStrategySettings());
        Map<Integer, Double> selectors = new HashMap<>();
        Map<Integer, Double> mutators = new HashMap<>();
        for (int i = 0; i < strategySettings.size(); i++) {
            StrategyConfigGroup.StrategySettings setting = strategySettings.get(i);
            if (DistributedPlanStrategyTranslationAndRegistration.SupportedSelectors.keySet().contains(setting.getStrategyName()))
                selectors.put(i, setting.getWeight());
            else {
                mutators.put(i, setting.getWeight());
                maximumIterationForMutationDisabling = Math.max(setting.getDisableAfter(), maximumIterationForMutationDisabling);
            }
        }

        double mutatorSum = CollectionUtils.sumElements(mutators.values());
        double selectorSum = CollectionUtils.sumElements(selectors.values());
        // set to new weight
        for (Entry<Integer, Double> entry : selectors.entrySet()) {
            strategySettings.get(entry.getKey()).setWeight((1 - masterMutationRate - borrowingRate) * entry.getValue() / selectorSum);
        }
        for (Entry<Integer, Double> entry : mutators.entrySet()) {
            strategySettings.get(entry.getKey()).setWeight(masterMutationRate * entry.getValue() / mutatorSum);
        }
        //put it back in the config
        config.strategy().clearStrategySettings();
        for (StrategyConfigGroup.StrategySettings strategySetting : strategySettings) {
            config.strategy().addStrategySettings(strategySetting);
        }
        // add the borrowing rate entry
        StrategyConfigGroup.StrategySettings borrowingSetting = new StrategyConfigGroup.StrategySettings();
        borrowingSetting.setWeight(borrowingRate);
        borrowingSetting.setStrategyName("ReplacePlanFromSlave");
        borrowingSetting.setDisableAfter(maximumIterationForMutationDisabling > 0 ? maximumIterationForMutationDisabling : disableAfterIteration);
        config.strategy().addStrategySettings(borrowingSetting);
        this.innovationEndsAtIter = maximumIterationForMutationDisabling > 0 ? maximumIterationForMutationDisabling : disableAfterIteration;
    }


    private int getTotalNumberOfPlansFromSlaves() {
        int total = 0;
        for (SlaveHandler slaveHandler : slaveHandlerTreeMap.values()) {
            total += slaveHandler.numberOfPlans;
        }
        return total;
    }

    private int getTotalNumberOfPlansOnMaster() {
        int total = 0;
        for (Person person : scenario.getPopulation().getPersons().values()) {
            total += person.getPlans().size();
        }
        return total;
    }

    public static void main(String[] args) throws InterruptedException {
        MasterControler master = null;

        try {
            master = new MasterControler(args);
        } catch (IOException e) {
            e.printStackTrace();
            Runtime.getRuntime().halt(0);
        }
        try {

            master.run();
        } catch (
                RuntimeException re
                ) {
            masterLogger.error(re.getStackTrace());
            master.hydra.killHydra();
            Runtime.getRuntime().halt(-1);
        }
        Runtime.getRuntime().halt(0);
    }

    void run() {
        matsimControler.run();
    }

    public void startSlaveHandlersInMode(CommunicationsMode mode) {
        if (numThreads.get() > 0)
            masterLogger.warn("All slaveHandlers have not finished previous operation but they are being asked to " + mode.toString());
        numThreads = new AtomicInteger(slaveHandlerTreeMap.size());
        for (SlaveHandler slaveHandler : slaveHandlerTreeMap.values()) {
            slaveHandler.communicationsMode = mode;
            Thread slaveThread = new Thread(slaveHandler);
            slaveThread.setName("slave_" + slaveHandler.myNumber + ":" + mode.toString());
            slaveThread.start();

        }
    }

    public void waitForSlaveThreads() {
        masterLogger.warn("Waiting for " + numThreads.get() +
                " slaveHandlers");
        while (numThreads.get() > 0)
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        if (somethingWentWrong) {
            masterLogger.error("Something went wrong. Exiting.");
            throw new RuntimeException();
        }
        masterLogger.warn("All slaveHandlers done.");
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        masterLogger.warn(masterInitialLogString);
        startSlaveHandlersInMode(CommunicationsMode.TRANSMIT_SCENARIO);
        if (initialRoutingOnSlaves) {
            waitForSlaveThreads();
            startSlaveHandlersInMode(CommunicationsMode.TRANSMIT_PLANS_TO_MASTER);
            waitForSlaveThreads();
            mergePlansFromSlaves();
            //this code is a copy of the replanning strategy
            for (Person person : matsimControler.getScenario().getPopulation().getPersons().values()) {
//                person.removePlan(person.getSelectedPlan());
                Plan plan = newPlans.get(person.getId().toString());
                person.addPlan(plan);
                person.setSelectedPlan(plan);
            }
            if (slaveHandlerTreeMap.size() > 1 || slavesHaveRequestedShutdown() || hydra.hydraSlaves.size() > 0)
                loadBalance();
            if (SelectedSimulationMode.equals(SimulationMode.PARALLEL)) {
                waitForSlaveThreads();
                startSlaveHandlersInMode(CommunicationsMode.CONTINUE);
            }
        }
    }


    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        this.currentIteration = event.getIteration();
        if (innovationEndsAtIter > 0 && event.getIteration() > innovationEndsAtIter)
            return;
        //wait for previous transmissions to complete, if necessary
        waitForSlaveThreads();
        //start receiving plans from slaveHandlerTreeMap as the QSim runs
        int firstIteration = config.controler().getFirstIteration();
        if (SelectedSimulationMode.equals(SimulationMode.PARALLEL))
            startSlaveHandlersInMode(CommunicationsMode.TRANSMIT_PLANS_TO_MASTER);
        IterationStopWatch stopwatch = event.getServices().getStopwatch();

    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        if (innovationEndsAtIter > 0 && event.getIteration() > innovationEndsAtIter)
            return;
        if (event.getIteration() == innovationEndsAtIter) {
            startSlaveHandlersInMode(CommunicationsMode.DIE);
            return;
        }

        if (SelectedSimulationMode.equals(SimulationMode.PARALLEL)) {
            waitForSlaveThreads();
            mergePlansFromSlaves();
            waitForSlaveThreads();
            startSlaveHandlersInMode(CommunicationsMode.TRANSMIT_SCORES);
            waitForSlaveThreads();
        }
        boolean isLoadBalanceIteration = event.getIteration() > config.controler().getFirstIteration() &&
                (event.getIteration() % loadBalanceInterval == 0 ||
                        slavesHaveRequestedShutdown() ||
                        hydra.hydraSlaves.size() > 0);
        //do load balancing, if necessary
        if (isLoadBalanceIteration)
            loadBalance();
        isLoadBalanceIteration = false;
        waitForSlaveThreads();
        linkTravelTimes = new SerializableLinkTravelTimes(matsimControler.getLinkTravelTimes(),
                config.travelTimeCalculator().getTraveltimeBinSize(),
                config.qsim().getEndTime(),
                scenario.getNetwork().getLinks().values());
        startSlaveHandlersInMode(CommunicationsMode.TRANSMIT_TRAVEL_TIMES);
        if (SelectedSimulationMode.equals(SimulationMode.SERIAL)) {
            waitForSlaveThreads();
            startSlaveHandlersInMode(CommunicationsMode.TRANSMIT_PLANS_TO_MASTER);
            waitForSlaveThreads();
            mergePlansFromSlaves();
            waitForSlaveThreads();
            startSlaveHandlersInMode(CommunicationsMode.TRANSMIT_SCORES);
            waitForSlaveThreads();
        }
    }

    private boolean slavesHaveRequestedShutdown() {
        for (SlaveHandler slaveHandler : slaveHandlerTreeMap.values()) {
            if (!slaveHandler.isOkForNextIter)
                return true;
        }
        return false;
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        //start receiving plans from slaveHandlerTreeMap as the QSim runs
        hydra.killHydra();
        startSlaveHandlersInMode(CommunicationsMode.DIE);
    }

    private void loadBalance() {
        waitForSlaveThreads();
        //add any newly registered slaveHandlerTreeMap
        slaveHandlerTreeMap.putAll(hydra.getNewSlaves());
        if (slaveHandlerTreeMap.size() < 2)
            return;
        startSlaveHandlersInMode(CommunicationsMode.TRANSMIT_PERFORMANCE);
        waitForSlaveThreads();
        if (getTotalNumberOfPlansFromSlaves() > 0) {
            bytesPerPlan = getTotalSlavePopulationMemoryUse() / getTotalNumberOfPlansFromSlaves();
            bytesPerPerson = getTotalSlavePopulationMemoryUse() / scenario.getPopulation().getPersons().size();
        }
        personPool = new ArrayList<>();
        masterLogger.warn("About to start load balancing.");
        Set<Integer> validSlaves = new TreeSet<>();
        Set<Integer> inValidSlaves = new TreeSet<>();
        validSlaves.addAll(slaveHandlerTreeMap.keySet());
        for (SlaveHandler slaveHandler : slaveHandlerTreeMap.values()) {
            if (!slaveHandler.isOkForNextIter) {
                validSlaves.remove(slaveHandler.myNumber);
                inValidSlaves.add(slaveHandler.myNumber);
                slaveHandler.targetPopulationSize = 0;
            }
        }

        double[] totalIterationTime = new double[validSlaves.size()];
        int[] personsPerSlave = new int[validSlaves.size()];
        long[] usedMemoryPerSlave = new long[validSlaves.size()];
        long[] maxMemoryPerSlave = new long[validSlaves.size()];
        int j = 0;
        for (int i : validSlaves) {
            totalIterationTime[j] = slaveHandlerTreeMap.get(i).totalIterationTime;
            personsPerSlave[j] = slaveHandlerTreeMap.get(i).currentPopulationSize;
            usedMemoryPerSlave[j] = slaveHandlerTreeMap.get(i).usedMemory;
            maxMemoryPerSlave[j] = slaveHandlerTreeMap.get(i).maxMemory;
            j++;
        }

        setSlaveTargetPopulationSizes(validSlaves,
                getSlaveTargetPopulationSizes(totalIterationTime, personsPerSlave, maxMemoryPerSlave, usedMemoryPerSlave,
                        bytesPerPlan, bytesPerPerson, loadBalanceDampeningFactor, scenario.getPopulation().getPersons().size()));
        startSlaveHandlersInMode(CommunicationsMode.POOL_PERSONS);
        waitForSlaveThreads();
        mergePersonsFromSlaves();
        masterLogger.warn("Distributing persons between  slaveHandlerTreeMap");
        //kill slaveHandlerTreeMap that are not ok for another round
        for (int i : inValidSlaves) {
            slaveHandlerTreeMap.get(i).communicationsMode = CommunicationsMode.DIE;
            new Thread(slaveHandlerTreeMap.get(i)).start();
            slaveHandlerTreeMap.remove(i);
        }
        startSlaveHandlersInMode(CommunicationsMode.DISTRIBUTE_PERSONS);
    }

    private long getTotalSlavePopulationMemoryUse() {
        long total = 0;
        for (SlaveHandler slaveHandler : slaveHandlerTreeMap.values()) {
            total += (slaveHandler.usedMemory - scenarioMemoryUse);
        }
        return total;
    }

    private void setSlaveTargetPopulationSizes(Set<Integer> keys, int[] slaveTargetPopulationSizes) {
        int j = 0;
        for (int i : keys) {
            slaveHandlerTreeMap.get(i).targetPopulationSize = slaveTargetPopulationSizes[j];
            j++;
        }
    }

    private void mergePlansFromSlaves() {
        newPlans.clear();
        for (SlaveHandler slaveHandler : slaveHandlerTreeMap.values()) {
            newPlans.putAll(slaveHandler.plans);
        }

    }

    private synchronized List<PersonSerializable> getPersonsFromPool(int diff) throws IndexOutOfBoundsException {
        List<PersonSerializable> outList = new ArrayList<>();
        if (diff < 0) {
            for (int i = 0; i > diff; i--) {
                outList.add(personPool.get(0));
                personPool.remove(0);
            }
        }
        return outList;
    }

    private void mergePersonsFromSlaves() {
        personPool.clear();
        for (SlaveHandler loadBalanceThread : slaveHandlerTreeMap.values()) {
            personPool.addAll(loadBalanceThread.getPersons());
        }
    }

    private static Map<Integer, Integer> getOptimalNumbers(int popSize, double[] timesPerPlan,
                                                           Set<Integer> validSlaveIndices) {
        Map<Integer, Integer> output = new HashMap<>();
        double sumOfReciprocals = 0.0;
        for (int i : validSlaveIndices) {
            sumOfReciprocals += 1 / timesPerPlan[i];
        }
        //        find number of persons that should be allocated to each slave
        int total = 0;
        for (int i : validSlaveIndices) {
            output.put(i, ((int) Math.ceil(popSize / timesPerPlan[i] / sumOfReciprocals)));
            total += output.get(i);
        }
        int j = 0;
        while (total > popSize) {
            for (int i : validSlaveIndices) {
                output.put(i, output.get(i) - 1);
                total--;
                if (total == popSize)
                    break;
            }
        }
        return output;
    }

    public static int[] getSlaveTargetPopulationSizes(double[] totalIterationTime, int[] personsPerSlave,
                                                      long[] maxMemory, long[] usedMemory, long bytesPerPlan,
                                                      long bytesPerPerson,
                                                      double dampeningFactor, int popSize) {
        int numSlaves = totalIterationTime.length;
        StringBuffer sb = new StringBuffer();
        sb.append("\n");
        sb.append(String.format("\t\t\t%20s:\t%20d\n", "bytesPerPlan", bytesPerPlan));
        sb.append(String.format("\t\t\t%20s:\t%20d\n", "bytesPerPerson", bytesPerPerson));
        String[] lines = {"slave", "totalIterationTime", "personsPerSlave", "maxMemory", "usedMemory"};
        sb.append("\n");
        for (int j = 0; j < 5; j++) {

            sb.append("\t\t\t" + String.format("%20s\t", lines[j]));
            for (int i = 0; i < numSlaves; i++) {
                switch (j) {
                    case 0:
                        sb.append(String.format("%20s\t", "slave_" + i));
                        break;
                    case 1:
                        sb.append(String.format("%20.3f\t", totalIterationTime[i]));
                        break;
                    case 2:
                        sb.append(String.format("%20d\t", personsPerSlave[i]));
                        break;
                    case 3:
                        sb.append(String.format("%20d\t", maxMemory[i]));
                        break;
                    case 4:
                        sb.append(String.format("%20d\t", usedMemory[i]));
                        break;
                }

            }
            sb.append("\n");
        }
        masterLogger.warn(sb.toString());


        Map<Integer, Integer> optimalNumberPerSlave = new HashMap<>();
        double[] timesPerPlan = new double[numSlaves];
        int[] allocation = new int[numSlaves];
        boolean[] fullyAllocated = new boolean[numSlaves];
        long[] overheadMemory = new long[numSlaves];

        Set<Integer> validSlaveIndices = new HashSet<>();
        Set<Integer> newSlaves = new HashSet<>();
        double fastestTimePerPlan = Double.POSITIVE_INFINITY;

        for (int i = 0; i < numSlaves; i++) {
            if (personsPerSlave[i] > 0 && totalIterationTime[i] > 0) {
                timesPerPlan[i] = (totalIterationTime[i] / personsPerSlave[i]);
                if (timesPerPlan[i] < fastestTimePerPlan)
                    fastestTimePerPlan = timesPerPlan[i];
            } else
                newSlaves.add(i);
            validSlaveIndices.add(i);
            maxMemory[i] = maxMemory[i] - bytesPerSlaveBuffer;
            overheadMemory[i] = usedMemory[i] - (personsPerSlave[i] * bytesPerPerson);
        }
        fastestTimePerPlan = fastestTimePerPlan > 0 && !new Double(fastestTimePerPlan).equals(Double.POSITIVE_INFINITY) ? fastestTimePerPlan : 1;
        for (int i : newSlaves)
            timesPerPlan[i] = fastestTimePerPlan;
//        adjust numbers taking account of memory avail on slaveHandlerTreeMap
        boolean allGood = false;
        int remainder = popSize;
        while (remainder > 0 && validSlaveIndices.size() > 0) {
            Set<Integer> valid = new HashSet<>();
            valid.addAll(validSlaveIndices);
            optimalNumberPerSlave.putAll(getOptimalNumbers(remainder, timesPerPlan, valid));
            for (int i : valid) {
                fullyAllocated[i] = false;
            }
            while (!isAllTrue(fullyAllocated)) {
                for (int i : valid) {
                    if (fullyAllocated[i]) {
                        continue;
                    }
                    long maxAvailMemory = (long) (maxMemory[i] - (planAllocationLimiter * (optimalNumberPerSlave.get(i) + allocation[i]) * bytesPerPlan));
                    long memoryAvailableForPersons = maxAvailMemory - overheadMemory[i];
                    int maxPersonAllocation = (int) (memoryAvailableForPersons / bytesPerPerson);

                    if (optimalNumberPerSlave.get(i) > maxPersonAllocation) {
                        optimalNumberPerSlave.put(i, optimalNumberPerSlave.get(i) - 1);
                        validSlaveIndices.remove(i);
                        continue;
                    } else {
                        //dampen the difference
                        if (optimalNumberPerSlave.get(i) > personsPerSlave[i] && optimalNumberPerSlave.get(i) > 10) {
                            int dampenedNumber = (int) (((1 - dampeningFactor) * (double) optimalNumberPerSlave.get(i)) + (dampeningFactor * (double) Math.min(personsPerSlave[i], maxPersonAllocation)));
                            optimalNumberPerSlave.put(i, dampenedNumber);
                        }
                    }
                    remainder -= optimalNumberPerSlave.get(i);
                    fullyAllocated[i] = true;
                }
            }
            for (int i : valid) {
                allocation[i] += optimalNumberPerSlave.get(i);
                if (allocation[i] <= 0) {
                    masterLogger.error("Something went wrong during loadBalancing (allocation <=0). Continuing as-is for now...");
                    return personsPerSlave;
                }
            }
            if (validSlaveIndices.size() == 0 && remainder > 0) {
                masterLogger.error("All slaves are nearing their maximum memory capacity!! Probably not a sustainable situation...");
                planAllocationLimiter--;
                //just distribute as if nothing has happened..
                for (int i = 0; i < numSlaves; i++) {
                    validSlaveIndices.add(i);
                }
            }
        }


        sb = new StringBuffer();
        lines = new String[]{"slave", "time per plan", "pax per slave", "allocation", "memUSED_MB", "memAVAIL_MB"};
        sb.append("\n");
        for (int j = 0; j < 6; j++) {

            sb.append("\t\t\t" + String.format("%20s\t", lines[j]));
            for (int i = 0; i < numSlaves; i++) {
                switch (j) {
                    case 0:
                        sb.append(String.format("%20s\t", "slave_" + i));
                        break;
                    case 1:
                        sb.append(String.format("%20.3f\t", timesPerPlan[i]));
                        break;
                    case 2:
                        sb.append(String.format("%20d\t", personsPerSlave[i]));
                        break;
                    case 3:
                        sb.append(String.format("%20d\t", allocation[i]));
                        break;
                    case 4:
                        sb.append(String.format("%20d\t", usedMemory[i]));
                        break;
                    case 5:
                        sb.append(String.format("%20d\t", maxMemory[i]));
                        break;
                }

            }
            sb.append("\n");
        }
        masterLogger.warn(sb.toString());
        return allocation;
    }

    private static boolean isAllTrue(boolean[] fullyAllocated) {
        for (boolean b : fullyAllocated) {
            if (!b) {
                return false;
            }
        }
        return true;

    }

    public double[][] getSlaveScoreHistory() {

        return this.slaveScoreStats.getScoreHistoryAsArray();
    }

    public Config getConfig() {
        return config;
    }

    public MatsimServices getMATSimControler() {
        return matsimControler;
    }


    private class SlaveHandler implements Runnable {
        final Logger slaveLogger = Logger.getLogger(this.getClass());
        final Map<String, Plan> plans = new HashMap<>();
        ObjectInputStream reader;
        ObjectOutputStream writer;
        double totalIterationTime;
        List<PersonSerializable> slavePersonPool;
        int targetPopulationSize = 0;
        CommunicationsMode communicationsMode = CommunicationsMode.TRANSMIT_SCENARIO;
        Collection<String> idStrings;
        private int myNumber;
        private int currentPopulationSize;
        private long usedMemory;
        private long maxMemory;
        private int numberOfPlans;
        private int numThreadsOnSlave;
        private boolean isOkForNextIter = true;

        public SlaveHandler(Socket socket, int i) throws IOException {
            super();
            myNumber = i;
            this.writer = new ObjectOutputStream(socket.getOutputStream());
            this.reader = new ObjectInputStream(socket.getInputStream());
        }

        public void transmitPlans() throws IOException, ClassNotFoundException {
            plans.clear();
            slaveLogger.warn("Waiting to receive plans from slave number " + myNumber);
            int slaveIteration = reader.readInt();
            int timesIteration = reader.readInt();
            slaveLogger.warn(String.format("Plan signature: M%03dP%03dT%03d ", currentIteration + 1, slaveIteration, timesIteration));
            slaveLogger.warn("(M = iteration for execution on master,P = PSim iteration when plan came from on slave, T = travel time iteration from master used to generate plan on slave)");
            Map<String, PlanSerializable> serialPlans = (Map<String, PlanSerializable>) reader.readObject();
            slaveLogger.warn("RECEIVED " + serialPlans.size() + " plans from slave number " + myNumber);
            for (Entry<String, PlanSerializable> entry : serialPlans.entrySet()) {
                plans.put(entry.getKey(), entry.getValue().getPlan(matsimControler.getScenario().getPopulation()));
            }
            this.currentPopulationSize = plans.size();
        }

        public void transmitPerformance() throws IOException {
            totalIterationTime = this.reader.readDouble();
            currentPopulationSize = this.reader.readInt();
            readMemoryStats();
        }

        public void transmitTravelTimes() throws IOException {
            slaveLogger.warn("About to send travel times to slave number " + myNumber);
            writer.writeInt(currentIteration);
            writer.writeObject(linkTravelTimes);
            if (config.transit().isUseTransit()) {
                writer.writeObject(stopStopTimeCalculator.getStopStopTimes());
                writer.writeObject(waitTimeCalculator.getWaitTimes());
                if (FullTransitPerformanceTransmission)
                    writer.writeObject(transitPerformanceRecorder.getTransitPerformance());
            }
            writer.flush();
            slaveLogger.warn("SENT travel times to slave number " + myNumber);
        }

        public void poolPersons() throws IOException, ClassNotFoundException {
            slaveLogger.warn("Trying to receive persons from slave " + myNumber);
            slaveLogger.warn("Currently has " + currentPopulationSize + " persons, target is " + targetPopulationSize);
            slavePersonPool = new ArrayList<>();
            writer.writeInt(currentPopulationSize - targetPopulationSize);
            writer.flush();
            slavePersonPool = (List<PersonSerializable>) reader.readObject();
        }

        public void distributePersons() throws IOException, InterruptedException {
            slaveLogger.warn("Distributing persons to slave" + myNumber);
            writer.writeInt(currentIteration);
            writer.writeObject(getPersonsFromPool(currentPopulationSize - targetPopulationSize));
            writer.flush();
        }

        public void transmitInitialPlans() throws IOException {
            writer.writeInt(currentIteration);
            writer.writeObject(slavePersonPool);
            writer.flush();
            this.currentPopulationSize = slavePersonPool.size();
        }

        @Override
        public void run() {
            try {
                slaveLogger.warn("SlaveHandler " + myNumber + " entering comms mode: " + communicationsMode.toString());
                writer.writeObject(communicationsMode);
                writer.flush();
                switch (communicationsMode) {
                    case TRANSMIT_TRAVEL_TIMES:
                        transmitTravelTimes();
                        reader.readBoolean();
                        communicationsMode = CommunicationsMode.CONTINUE;
                        writer.writeObject(communicationsMode);
                        writer.flush();
                        break;
                    case POOL_PERSONS:
                        poolPersons();
                        break;
                    case DISTRIBUTE_PERSONS:
                        distributePersons();
                        break;
                    case TRANSMIT_PLANS_TO_MASTER:
                        writer.reset();
                        transmitPlans();
                        slaveIsOKForNextIter();
                        break;
                    case TRANSMIT_SCORES:
                        transmitScores();
                        break;
                    case TRANSMIT_PERFORMANCE:
                        transmitPerformance();
                        break;
                    case TRANSMIT_SCENARIO:
                        transmitInitialPlans();
                        reader.readBoolean();
                        communicationsMode = CommunicationsMode.CONTINUE;
                        writer.writeObject(communicationsMode);
                        writer.flush();
                        break;
                    case DIE:
                        return;
                }
                reader.readBoolean();
            } catch (IOException | InterruptedException | IndexOutOfBoundsException | ClassNotFoundException e) {
                e.printStackTrace();
                somethingWentWrong = true;
                numThreads.decrementAndGet();
            }
            //end of a successful Thread.run()
            numThreads.decrementAndGet();
            slaveLogger.warn("SlaveHandler " + myNumber + " leaving comms mode: " + communicationsMode.toString());
        }

        private void transmitScores() throws IOException, ClassNotFoundException {
            slaveScoreStats.insertEntry(currentIteration, currentPopulationSize, scenario.getPopulation().getPersons().size(), (double[]) reader.readObject());
        }


        private void slaveIsOKForNextIter() throws IOException {
            this.isOkForNextIter = reader.readBoolean();
        }


        public void sendNumber(int i) throws IOException {
            writer.writeInt(i);
            writer.flush();
        }

        public void sendDouble(double i) throws IOException {
            writer.writeDouble(i);
            writer.flush();
        }


        public Collection<? extends PersonSerializable> getPersons() {
            return slavePersonPool;
        }

        public void sendBoolean(boolean initialRouting) throws IOException {
            writer.writeBoolean(initialRouting);
            writer.flush();
        }

        public void readNumberOfThreadsOnSlave() throws IOException {
            this.numThreadsOnSlave = reader.readInt();
        }

        public void readMemoryStats() throws IOException {
            this.usedMemory = reader.readLong();
            this.maxMemory = reader.readLong();
            this.numberOfPlans = reader.readInt();
        }
    }

    private class Hydra implements Runnable {
        TreeMap<Integer, SlaveHandler> hydraSlaves = new TreeMap<>();
        AtomicBoolean accessingMap = new AtomicBoolean(false);

        public void killHydra() {
            this.acceptSlaves = false;
        }

        boolean acceptSlaves = true;

        public void run() {
            ServerSocket writeServer = null;
            try {
                writeServer = new ServerSocket(socketNumber);
                while (acceptSlaves) {
                    Socket socket = writeServer.accept();
                    int i = slaveUniqueNumber++;
                    masterLogger.warn("Slave accepted.");
                    SlaveHandler slaveHandler = new SlaveHandler(socket, i);
                    while (accessingMap.get()) {
//                    wait for the other process to finish modifying the map
                        Thread.sleep(10);
                    }
                    accessingMap.set(true);
                    hydraSlaves.put(i, slaveHandler);
                    //order is important
                    initializeSlave(slaveHandler, i, false);
                    slaveHandler.slavePersonPool = new ArrayList<>();
                    accessingMap.set(false);
                    Thread.sleep(1000);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public TreeMap<Integer, SlaveHandler> getNewSlaves() {
            TreeMap<Integer, SlaveHandler> slaveBatch;
            while (accessingMap.get()) {
                try {
//                    wait for the other process to finish modifying the map
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            accessingMap.set(true);
            List<Integer> slavesToDrop = new ArrayList<>();
            for (SlaveHandler slaveHandler : hydraSlaves.values()) {
                if (!slaveHandler.isOkForNextIter) slavesToDrop.add(slaveHandler.myNumber);
            }
            for (int i : slavesToDrop) hydraSlaves.remove(i);
            slaveBatch = hydraSlaves;
            hydraSlaves = new TreeMap<>();
            accessingMap.set(false);
            return slaveBatch;
        }
    }

    private void initializeSlave(SlaveHandler slaveHandler, int i, boolean initialRoutingOnSlaves) throws IOException {
        slaveHandler.sendNumber(i);
        slaveHandler.sendNumber(numberOfPSimIterations);
        slaveHandler.sendNumber(slaveNumberOfPlans);
        slaveHandler.sendDouble(SlaveMutationRate);
        slaveHandler.sendNumber(config.controler().getLastIteration() * numberOfPSimIterations);
        slaveHandler.sendBoolean(initialRoutingOnSlaves);
        slaveHandler.sendBoolean(QuickReplanning);
        slaveHandler.sendBoolean(FullTransitPerformanceTransmission);
        slaveHandler.sendBoolean(TrackGenome);
        slaveHandler.sendBoolean(IntelligentRouters);
        slaveHandler.sendBoolean(false); //for diversity generation;
        slaveHandler.readMemoryStats();
        slaveHandler.readNumberOfThreadsOnSlave();
    }

}


class MemoryUsageCalculator {

    public long getMemoryUse() {
        putOutTheGarbage();
        long totalMemory = Runtime.getRuntime().totalMemory();
        putOutTheGarbage();
        long freeMemory = Runtime.getRuntime().freeMemory();
        return (totalMemory - freeMemory);
    }

    private void putOutTheGarbage() {
        collectGarbage();
        collectGarbage();
    }

    private void collectGarbage() {
        try {
            System.gc();
            long fSLEEP_INTERVAL = 100;
            Thread.currentThread().sleep(fSLEEP_INTERVAL);
            System.runFinalization();
            Thread.currentThread().sleep(fSLEEP_INTERVAL);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}


