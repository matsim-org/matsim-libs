package org.matsim.contrib.pseudosimulation.distributed;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.distributed.instrumentation.scorestats.SlaveScoreStats;
import org.matsim.contrib.pseudosimulation.distributed.listeners.controler.GenomeAnalysis;
import org.matsim.contrib.pseudosimulation.distributed.listeners.controler.SlaveScoreWriter;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformanceRecorder;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;
import org.matsim.contrib.pseudosimulation.replanning.DistributedPlanStrategyTranslationAndRegistration;
import org.matsim.contrib.pseudosimulation.util.CollectionUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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
import org.matsim.core.scenario.ScenarioUtils;

public class MasterControler implements AfterMobsimListener, ShutdownListener, StartupListener, IterationStartsListener {
    public static final Logger masterLogger = LogManager.getLogger(MasterControler.class);
    private static StringBuilder masterInitialLogString = new StringBuilder();
    private static String appendString;
    private  final int masterPortNumber;
    private  final double masterMutationRate;
    private  final double slaveMutationRate;
    private  final int initialNumberOfSlaves;
    private  final double masterBorrowingRate;
    private  final boolean TrackGenome = false; // todo genome tracking
    private  final boolean intelligentRouters;

    private int innovationEndsAtIter = -1;
    private int slaveNumberOfPlans=3;
    private final HashMap<String, Plan> newPlans = new HashMap<>();
    private final DynamicSlaveRegistry<MasterSlaveSession> hydra;
    private long bytesPerPerson;
    private Scenario scenario;
    private long scenarioMemoryUse;
    private long bytesPerPlan;
    private static boolean initialRoutingOnSlaves = true;
    private final int slaveIterationsPerMasterIteration;
    private Config config;
    private Controler matsimControler;
    private TreeMap<Integer, MasterSlaveSession> slaveHandlerTreeMap;
//    private WaitTimeCalculatorSerializable waitTimeCalculator;
//    private StopStopTimeCalculatorSerializable stopStopTimeCalculator;
    private TransitPerformanceRecorder transitPerformanceRecorder;
    private SerializableLinkTravelTimes linkTravelTimes;
    private final SlaveHandlerCoordinator slaveHandlerCoordinator = SlaveHandlerCoordinator.production(masterLogger);
    private List<PersonSerializable> personPool;
    private static int loadBalanceInterval = 5;
    public static double planAllocationLimiter = 10.0;
    public static final long bytesPerSlaveBuffer = (long) 2e8;
    public int slaveUniqueNumber = 0;
    SlaveScoreStats slaveScoreStats;

    public enum SimulationMode {SERIAL, PARALLEL}

    public static SimulationMode SelectedSimulationMode;

    public static boolean QuickReplanning = false;
    private static boolean fullTransitPerformanceTransmission;

    /**
     * value between 0 and 1; increasing it increases the dampening effect of preventing
     * large transfers of persons during load balance iterations
     */
    private static final double loadBalanceDampeningFactor = 0.4;
    private int currentIteration = -1;



    public MasterControler(String configFile) throws NumberFormatException, IOException, InterruptedException {
        config = ConfigUtils.loadConfig(configFile);
        final DistributedSimConfigGroup distributedSimConfigGroup = ConfigUtils.addOrGetModule(this.config,DistributedSimConfigGroup.GROUP_NAME,DistributedSimConfigGroup.class);
        masterBorrowingRate = distributedSimConfigGroup.getMasterBorrowingRate();
        masterMutationRate = distributedSimConfigGroup.getMasterMutationRate();
        masterPortNumber = distributedSimConfigGroup.getMasterPortNumber();
        slaveMutationRate = distributedSimConfigGroup.getSlaveMutationRate();
        initialNumberOfSlaves = distributedSimConfigGroup.getInitialNumberOfSlaves();
        intelligentRouters = distributedSimConfigGroup.isIntelligentRouters();
        SelectedSimulationMode = distributedSimConfigGroup.isSlavesRunInParallelToMaster() ? SimulationMode.PARALLEL : SimulationMode.SERIAL;
        slaveIterationsPerMasterIteration = distributedSimConfigGroup.getSlaveIterationsPerMasterIteration();
        fullTransitPerformanceTransmission = distributedSimConfigGroup.isFullTransitPerformanceTransmission();

        slaveHandlerTreeMap = new TreeMap<>();
        slaveScoreStats = new SlaveScoreStats(this.config);

        innovationEndsAtIter = new ReplanningWeightUpdater().updateMaster(
                this.config, masterMutationRate, masterBorrowingRate);

//        register initial number of slaves
        ServerSocket writeServer = new ServerSocket(masterPortNumber);
        for (int i = 0; i < initialNumberOfSlaves; i++) {
            Socket socket = writeServer.accept();
            System.out.println("Slave " + (i + 1) + " out of an initial " + initialNumberOfSlaves + " accepted.\n");
            MasterSlaveSession slaveHandler = new MasterSlaveSession(socket, slaveUniqueNumber, masterSlaveContext());
            slaveHandlerTreeMap.put(slaveUniqueNumber, slaveHandler);
            //order is important
            initializeSlave(slaveHandler, slaveUniqueNumber++, initialRoutingOnSlaves);
            Thread.sleep(10);
        }
        writeServer.close();
        masterInitialLogString.append("MASTER accepted minimum number of incoming connections. All further slaves will be registered on the Hydra.\n");
        hydra = DynamicSlaveRegistry.production(
                masterPortNumber,
                (socket, id) -> new MasterSlaveSession(socket, id, masterSlaveContext()),
                () -> slaveUniqueNumber++,
                (slaveHandler, id) -> initializeSlave(slaveHandler, id, false),
                slaveHandler -> slaveHandler.setPersons(new ArrayList<>()),
                MasterSlaveSession::readyForNextIteration,
                MasterSlaveSession::slaveNumber,
                masterLogger);
        Thread hydraThread = new Thread(hydra);
        hydraThread.setName("HYDRA");
        hydraThread.start();


        scenario = ScenarioUtils.loadScenario(this.config);
//        determine the memory use of the population for some initial load balancing
        MemoryUsageCalculator memoryUsageCalculator = new MemoryUsageCalculator();
        scenarioMemoryUse = memoryUsageCalculator.getMemoryUse();
        long currentPopulationMemoryUse = memoryUsageCalculator.getMemoryUse() - scenarioMemoryUse;
        bytesPerPlan = Math.max(1000, currentPopulationMemoryUse / getTotalNumberOfPlansOnMaster());
        bytesPerPerson = bytesPerPlan;
        masterInitialLogString.append("Estimated memory use per plan is " + bytesPerPlan + " bytes\n");

        matsimControler = new Controler(scenario);
        matsimControler.addControllerListener(new SlaveScoreWriter(this));

        //split the population to be sent to the slaveHandlers

        double[] totalIterationTime = new double[initialNumberOfSlaves];
        int[] personsPerSlave = new int[initialNumberOfSlaves];
        long[] usedMemoryPerSlave = new long[initialNumberOfSlaves];
        long[] maxMemoryPerSlave = new long[initialNumberOfSlaves];
        int j = 0;
        for (int i : slaveHandlerTreeMap.keySet()) {
            totalIterationTime[j] = 1 / (double) slaveHandlerTreeMap.get(i).numberOfThreads();
            personsPerSlave[j] = scenario.getPopulation().getPersons().size() / initialNumberOfSlaves;
            usedMemoryPerSlave[j] = slaveHandlerTreeMap.get(i).usedMemory();
            maxMemoryPerSlave[j] = slaveHandlerTreeMap.get(i).maxMemory();
            j++;
        }
        int[] initialWeights = getSlaveTargetPopulationSizes(totalIterationTime, personsPerSlave, maxMemoryPerSlave, usedMemoryPerSlave,
                bytesPerPlan, bytesPerPerson, 0.0, scenario.getPopulation().getPersons().size());
        List<? extends Person>[] personSplit = CollectionUtils.split(scenario.getPopulation().getPersons().values(), initialWeights);
        j = 0;
        for (int i : slaveHandlerTreeMap.keySet()) {
            List<PersonSerializable> personsToSend = new ArrayList<>();
            for (Person p : personSplit[j]) {
                personsToSend.add(new PersonSerializable(p));
            }
            slaveHandlerTreeMap.get(i).setPersons(personsToSend);
            j++;
        }


        if (this.config.transit().isUseTransit()) {
//            waitTimeCalculator = new WaitTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(), this.config.travelTimeCalculator().getTraveltimeBinSize(),
//                    (int) (this.config.qsim().getEndTime().seconds() - this.config.qsim().getStartTime().seconds()));
//            matsimControler.getEvents().addHandler(waitTimeCalculator);
//            stopStopTimeCalculator = new StopStopTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(),
//                    this.config.travelTimeCalculator().getTraveltimeBinSize(), (int) (this.config.qsim()
//                    .getEndTime().seconds() - this.config.qsim().getStartTime().seconds()));
//            matsimControler.getEvents().addHandler(stopStopTimeCalculator);
            //tell PlanSerializable to record transit routes
            PlanSerializable.isUseTransit = true;
            if (fullTransitPerformanceTransmission) {
                transitPerformanceRecorder = new TransitPerformanceRecorder(scenario, matsimControler.getEvents());
            }
        }

        matsimControler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addPlanStrategyBinding("ReplacePlanFromSlave").toProvider(new ReplacePlanFromSlaveFactory(newPlans));
            }
        });
        matsimControler.addControllerListener(this);



        matsimControler.getConfig().controller().setOverwriteFileSetting(
                OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
//                true ?
//                        OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
//                        OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
        if (TrackGenome) {
            matsimControler.addControllerListener(new GenomeAnalysis(true, false, true));
        }

    }

    private int getTotalNumberOfPlansFromSlaves() {
        int total = 0;
        for (MasterSlaveSession slaveHandler : slaveHandlerTreeMap.values()) {
            total += slaveHandler.numberOfPlans();
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
            master = new MasterControler(args[0]);
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
            master.hydra.kill();
            Runtime.getRuntime().halt(-1);
        }
        Runtime.getRuntime().halt(0);
    }

    void run() {
        matsimControler.run();
    }

    public void startSlaveHandlersInMode(CommunicationsMode mode) {
        slaveHandlerCoordinator.start(slaveHandlerTreeMap.values(), mode);
    }

    public void waitForSlaveThreads() {
        slaveHandlerCoordinator.waitForCompletion();
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
            if (slaveHandlerTreeMap.size() > 1 || slavesHaveRequestedShutdown() || hydra.pendingCount() > 0)
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
        if (SelectedSimulationMode.equals(SimulationMode.PARALLEL))
            startSlaveHandlersInMode(CommunicationsMode.TRANSMIT_PLANS_TO_MASTER);

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
        boolean isLoadBalanceIteration = event.getIteration() > config.controller().getFirstIteration() &&
                (event.getIteration() % loadBalanceInterval == 0 ||
                        slavesHaveRequestedShutdown() ||
                        hydra.pendingCount() > 0);
        //do load balancing, if necessary
        if (isLoadBalanceIteration)
            loadBalance();
        isLoadBalanceIteration = false;
        waitForSlaveThreads();
        linkTravelTimes = new SerializableLinkTravelTimes(matsimControler.getLinkTravelTimes(),
                config.travelTimeCalculator().getTraveltimeBinSize(),
				(int) config.qsim().getEndTime().seconds(),
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
        for (MasterSlaveSession slaveHandler : slaveHandlerTreeMap.values()) {
            if (!slaveHandler.readyForNextIteration())
                return true;
        }
        return false;
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        //start receiving plans from slaveHandlerTreeMap as the QSim runs
        hydra.kill();
        startSlaveHandlersInMode(CommunicationsMode.DIE);
    }

    private void loadBalance() {
        waitForSlaveThreads();
        //add any newly registered slaveHandlerTreeMap
        slaveHandlerTreeMap.putAll(hydra.drainReadySlaves());
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
        for (MasterSlaveSession slaveHandler : slaveHandlerTreeMap.values()) {
            if (!slaveHandler.readyForNextIteration()) {
                validSlaves.remove(slaveHandler.slaveNumber());
                inValidSlaves.add(slaveHandler.slaveNumber());
                slaveHandler.setTargetPopulationSize(0);
            }
        }

        double[] totalIterationTime = new double[validSlaves.size()];
        int[] personsPerSlave = new int[validSlaves.size()];
        long[] usedMemoryPerSlave = new long[validSlaves.size()];
        long[] maxMemoryPerSlave = new long[validSlaves.size()];
        int j = 0;
        for (int i : validSlaves) {
            totalIterationTime[j] = slaveHandlerTreeMap.get(i).totalIterationTime();
            personsPerSlave[j] = slaveHandlerTreeMap.get(i).currentPopulationSize();
            usedMemoryPerSlave[j] = slaveHandlerTreeMap.get(i).usedMemory();
            maxMemoryPerSlave[j] = slaveHandlerTreeMap.get(i).maxMemory();
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
            slaveHandlerTreeMap.get(i).setCommunicationsMode(CommunicationsMode.DIE);
            new Thread(slaveHandlerTreeMap.get(i)).start();
            slaveHandlerTreeMap.remove(i);
        }
        startSlaveHandlersInMode(CommunicationsMode.DISTRIBUTE_PERSONS);
    }

    private long getTotalSlavePopulationMemoryUse() {
        long total = 0;
        for (MasterSlaveSession slaveHandler : slaveHandlerTreeMap.values()) {
            total += (slaveHandler.usedMemory() - scenarioMemoryUse);
        }
        return total;
    }

    private void setSlaveTargetPopulationSizes(Set<Integer> keys, int[] slaveTargetPopulationSizes) {
        int j = 0;
        for (int i : keys) {
            slaveHandlerTreeMap.get(i).setTargetPopulationSize(slaveTargetPopulationSizes[j]);
            j++;
        }
    }

    private void mergePlansFromSlaves() {
        newPlans.clear();
        for (MasterSlaveSession slaveHandler : slaveHandlerTreeMap.values()) {
            newPlans.putAll(slaveHandler.plans());
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
        for (MasterSlaveSession loadBalanceThread : slaveHandlerTreeMap.values()) {
            personPool.addAll(loadBalanceThread.persons());
        }
    }

    public static int[] getSlaveTargetPopulationSizes(double[] totalIterationTime, int[] personsPerSlave,
                                                      long[] maxMemory, long[] usedMemory, long bytesPerPlan,
                                                      long bytesPerPerson,
                                                      double dampeningFactor, int popSize) {
        SlavePopulationAllocator allocator = new SlavePopulationAllocator(
                masterLogger, planAllocationLimiter, bytesPerSlaveBuffer);
        try {
            return allocator.allocate(totalIterationTime, personsPerSlave,
                    maxMemory, usedMemory, bytesPerPlan, bytesPerPerson, dampeningFactor, popSize);
        } finally {
            planAllocationLimiter = allocator.planAllocationLimiter();
        }
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


    private void initializeSlave(MasterSlaveSession slave, int number, boolean initialRouting) throws IOException {
        slave.initialize(new MasterSlaveSession.Initialization(
                number,
                slaveIterationsPerMasterIteration,
                slaveNumberOfPlans,
                slaveMutationRate,
                config.controller().getLastIteration() * slaveIterationsPerMasterIteration,
                initialRouting,
                QuickReplanning,
                fullTransitPerformanceTransmission,
                TrackGenome,
                intelligentRouters));
    }

    private MasterSlaveSession.Context masterSlaveContext() {
        return new MasterSlaveSession.Context() {
            @Override
            public int currentIteration() {
                return currentIteration;
            }

            @Override
            public org.matsim.api.core.v01.population.Population population() {
                return scenario.getPopulation();
            }

            @Override
            public SerializableLinkTravelTimes linkTravelTimes() {
                return linkTravelTimes;
            }

            @Override
            public boolean transitEnabled() {
                return config.transit().isUseTransit();
            }

            @Override
            public boolean fullTransitPerformanceTransmission() {
                return fullTransitPerformanceTransmission;
            }

            @Override
            public TransitPerformance transitPerformance() {
                return transitPerformanceRecorder.getTransitPerformance();
            }

            @Override
            public List<PersonSerializable> takePersons(int difference) {
                return getPersonsFromPool(difference);
            }

            @Override
            public void recordScores(int iteration, int slavePopulation, int masterPopulation, double[] scores) {
                slaveScoreStats.insertEntry(iteration, slavePopulation, masterPopulation, scores);
            }

            @Override
            public Runnable completion() {
                return slaveHandlerCoordinator.completion();
            }

            @Override
            public void failed() {
                slaveHandlerCoordinator.failed();
            }
        };
    }

}
