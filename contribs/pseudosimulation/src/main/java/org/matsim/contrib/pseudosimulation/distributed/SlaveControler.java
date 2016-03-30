package org.matsim.contrib.pseudosimulation.distributed;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.common.diversitygeneration.planselectors.DiversityGeneratingPlansRemover;
import org.matsim.contrib.common.randomizedtransitrouter.RandomizedTransitRouterModule;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculatorSerializable;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculatorSerializable;
import org.matsim.contrib.pseudosimulation.distributed.instrumentation.scorestats.SlaveScoreStatsCalculator;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;
import org.matsim.contrib.pseudosimulation.mobsim.PSimFactory;
import org.matsim.contrib.pseudosimulation.replanning.DistributedPlanStrategyTranslationAndRegistration;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.DefaultMobsimModule;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionModule;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.router.TransitRouter;
import org.matsim.vehicles.Vehicle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.*;

//IMPORTANT: PSim produces events that are not in chronological order. This controler
// will require serious overhaul if chronological order is enforced in all event manager implementations
public class SlaveControler implements IterationStartsListener, StartupListener, BeforeMobsimListener, IterationEndsListener, Runnable {
    public static int numberOfPSimIterationsPerCycle;
    private final Scenario scenario;
    private final MemoryUsageCalculator memoryUsageCalculator;
    private final ReplaceableTravelTime travelTime;
    private final boolean fullTransitPerformanceTransmission;
    private final boolean IntelligentRouters;
    private final Logger slaveLogger;
    private final PlanCatcher plancatcher;
    private boolean initialRouting;
    private int numberOfIterations = -1;
    private int executedPlanCount;
    private int currentIteration = 0;
    private int masterCurrentIteration = -1;
    private Config config;
    private double totalIterationTime;
    private Controler matsimControler;
    private TravelTime linkTravelTimes;
    private WaitTime waitTimes;
    private StopStopTime stopStopTimes;
    private ObjectInputStream reader;
    private ObjectOutputStream writer;
    private PSimFactory pSimFactory;
    private List<Long> iterationTimes = new ArrayList<>();
    private long lastIterationStartTime;
    private boolean somethingWentWrong = false;
    private boolean isOkForNextIter = true;
    private Map<Id<Person>, Double> selectedPlanScoreMemory;
    private TransitPerformance transitPerformance;
    private void printHelp(Options options){
        String header = "The MasterControler takes the following options:\n\n";
        String footer = "";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("MasterControler", header, options, footer, true);
    }
    public SlaveControler(String[] args) throws IOException, ClassNotFoundException, ParseException, InterruptedException {
        lastIterationStartTime = System.currentTimeMillis();
        System.setProperty("matsim.preferLocalDtds", "true");
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("config")
                .withDescription("Config file location")
                .hasArg(true)
                .withArgName("CONFIG.XML")
                .isRequired(true)
                .create("c"));
        options.addOption("h","host", true, "Host name or IP");
        options.addOption("p", "port", true, "Port number of MasterControler");
        options.addOption("t","threads", true, "Number of threads for parallel events handling.");
        CommandLineParser parser = new BasicParser();
        CommandLine commandLine = parser.parse(options, args);


        if (commandLine.hasOption("c")) {
            try {
                config = ConfigUtils.loadConfig(commandLine.getOptionValue("c"));

            } catch (UncheckedIOException e) {
                System.err.println("Config file not found");
                printHelp(options);
                System.exit(1);
            }
        } else {
            System.err.println("Config file not specified");
            System.out.println(options.toString());
            System.exit(1);
        }

        //The following line will make the controler use the events manager that doesn't check for event order
        config.parallelEventHandling().setSynchronizeOnSimSteps(false);
        //if you don't set the number of threads, org.matsim.core.events.EventsUtils will just use the simstepmanager
        int numThreads = 1;
        if (commandLine.hasOption("t"))
            try {
                numThreads = Integer.parseInt(commandLine.getOptionValue("t"));
            } catch (NumberFormatException e) {
                System.err.println("Number of threads should be int.");
                System.out.println(options.toString());
                System.exit(1);
            }
        else {
            System.err.println("Will use the number of threads in config for simulation.");
        }
        config.global().setNumberOfThreads(numThreads);
        config.parallelEventHandling().setNumberOfThreads(1);

        String hostname = "localhost";
        if (commandLine.hasOption("h")) {
            hostname = commandLine.getOptionValue("h");
        } else
            System.err.println("No host specified, using default (localhost)");

        /*
        * INITIALIZING COMMS
        * */
        Socket socket = null;
        int socketNumber = 12345;
        if (commandLine.hasOption("p")) {
            try {
                socketNumber = Integer.parseInt(commandLine.getOptionValue("p"));
            } catch (NumberFormatException e) {
                System.err.println("Port number should be integer");
                System.out.println(options.toString());
                System.exit(1);
            }
        } else {
            System.err.println("Will accept connections on default port number 12345");
        }
        boolean connected = false;
        while (!connected) {
            try {
                socket = new Socket(hostname, socketNumber);
                connected = true;
            } catch (ConnectException e) {
                Thread.sleep(1000);
            }
        }
        this.reader = new ObjectInputStream(socket.getInputStream());
        this.writer = new ObjectOutputStream(socket.getOutputStream());

        int myNumber = reader.readInt();
        slaveLogger = Logger.getLogger(("SLAVE_" + myNumber));

        numberOfPSimIterationsPerCycle = reader.readInt();
        slaveLogger.warn("Running " + numberOfPSimIterationsPerCycle + " PSim iterations for every QSim iter");
        config.controler().setLastIteration(reader.readInt());
        initialRouting = reader.readBoolean();
        boolean quickReplannning = reader.readBoolean();
        fullTransitPerformanceTransmission = reader.readBoolean();
        boolean singaporeScenario = reader.readBoolean();
        boolean trackGenome = reader.readBoolean();
        IntelligentRouters = reader.readBoolean();
        boolean diversityGeneratingPlanSelection = reader.readBoolean();

        if (initialRouting) slaveLogger.warn("Performing initial routing.");

        memoryUsageCalculator = new MemoryUsageCalculator();
        writeMemoryStats();
        writer.writeInt(config.global().getNumberOfThreads());
        writer.flush();


        config.controler().setOutputDirectory(config.controler().getOutputDirectory() + "_" + myNumber);
        //limit IO
        config.linkStats().setWriteLinkStatsInterval(0);
        config.controler().setCreateGraphs(false);
        config.controler().setWriteEventsInterval(1);
        config.controler().setWritePlansInterval(0);
        config.controler().setWriteSnapshotsInterval(0);
        scenario = ScenarioUtils.loadScenario(config);
        DistributedPlanStrategyTranslationAndRegistration.TrackGenome = trackGenome;
        DistributedPlanStrategyTranslationAndRegistration.substituteStrategies(config, quickReplannning, numberOfPSimIterationsPerCycle);
        matsimControler = new Controler(scenario);
        plancatcher = new PlanCatcher();
        DistributedPlanStrategyTranslationAndRegistration.registerStrategiesWithControler(this.matsimControler, plancatcher, quickReplannning, numberOfPSimIterationsPerCycle);
        matsimControler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
        matsimControler.addControlerListener(this);
        linkTravelTimes = new FreeSpeedTravelTime();
        travelTime = new ReplaceableTravelTime();
        travelTime.setTravelTime(linkTravelTimes);
        pSimFactory = new PSimFactory();
        matsimControler.setModules(new AbstractModule() {
            @Override
            public void install() {
//                include(new ScoreStatsModule());
                install(new DefaultMobsimModule());
                install(new TripRouterModule());
                install(new CharyparNagelScoringFunctionModule());
                install(new StrategyManagerModule());
                if (IntelligentRouters)
                    install(new TravelDisutilityModule());
                else{
                    final Builder disutilityFactory =
                            new Builder( TransportMode.car, config.planCalcScore() );
                    matsimControler.addOverridingModule(new AbstractModule() {
                        @Override
                        public void install() {
                            bindCarTravelDisutilityFactory().toInstance(disutilityFactory);
                        }
                    });
                    disutilityFactory.setSigma(0.1);

                }
                bind(TravelTime.class).toInstance(travelTime);
                if (getConfig().strategy().getPlanSelectorForRemoval().equals("DiversityGeneratingPlansRemover")) {
                    bindPlanSelectorForRemoval().toProvider(DiversityGeneratingPlansRemover.Builder.class);
                }
            }
        });
//        new Thread(new TimesReceiver()).start();
        if (config.transit().isUseTransit()) {

            stopStopTimes = new StopStopTimeCalculatorSerializable(scenario.getTransitSchedule(),
                    config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config
                    .qsim().getEndTime() - config.qsim().getStartTime())).getStopStopTimes();

            waitTimes = new WaitTimeCalculatorSerializable(scenario.getTransitSchedule(),
                    config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config
                    .qsim().getEndTime() - config.qsim().getStartTime())).getWaitTimes();

            //tell PlanSerializable to record transit routes
            PlanSerializable.isUseTransit = true;

        }


        if (IntelligentRouters) {
            matsimControler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    bind(TransitRouter.class).toProvider(new TransitRouterEventsWSFactory(scenario, waitTimes, stopStopTimes));
                }
            });
        } else {
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
        if (trackGenome) {

        }
        if(diversityGeneratingPlanSelection)
            matsimControler.getConfig().strategy().setPlanSelectorForRemoval("DiversityGeneratingPlansRemover");
        //no use for this, if you don't exactly know the communicationsMode of population when something goes wrong.
        // better to have plans written out every n successful iterations, specified in the config
        matsimControler.getConfig().controler().setDumpDataAtEnd(false);
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, ParseException, InterruptedException {
        SlaveControler slave = new SlaveControler(args);
        new Thread(slave).start();
        System.out.printf("Enter KILL to kill the slave: ");
        Scanner in = new Scanner(System.in);
        String s;
        boolean running = true;
        do {
            s = in.nextLine();
            if (s.equals("KILL"))
                running = false;
        } while (running);
        slave.requestShutDown();
    }

    public Config getConfig() {
        return config;
    }

    private void writeMemoryStats() throws IOException {
        writer.writeLong(memoryUsageCalculator.getMemoryUse());
        writer.writeLong(Runtime.getRuntime().maxMemory());
        writer.writeInt(getTotalNumberOfPlans());
    }

    private int getTotalNumberOfPlans() {
        int total = 0;
        try {
            for (Person person : scenario.getPopulation().getPersons().values()) {
                total += person.getPlans().size();
            }
        } catch (NullPointerException e) {
        }
        return total;
    }

    @Override
    public void run() {
        matsimControler.run();
    }

    public void requestShutDown() {
        isOkForNextIter = false;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (numberOfIterations >= 0 || initialRouting)
            iterationTimes.add(System.currentTimeMillis() - lastIterationStartTime);

        if (initialRouting || (numberOfIterations > 0 && numberOfIterations % numberOfPSimIterationsPerCycle == 0)) {
            this.totalIterationTime = getTotalIterationTime();
            communications();
            if (somethingWentWrong) Runtime.getRuntime().halt(0);
            initialRouting = false;
        }
        this.currentIteration = event.getIteration();
        lastIterationStartTime = System.currentTimeMillis();
        travelTime.setTravelTime(linkTravelTimes);
        pSimFactory.setTravelTime(linkTravelTimes);
        if (config.transit().isUseTransit()) {
            pSimFactory.setStopStopTime(stopStopTimes);
            pSimFactory.setWaitTime(waitTimes);
            pSimFactory.setTransitPerformance(transitPerformance);
//            if (matsimControler.getTransitRouterFactory() instanceof TransitRouterEventsWSFactory) {
//                ((TransitRouterEventsWSFactory) matsimControler.getTransitRouterFactory()).setStopStopTime(stopStopTimes);
//                ((TransitRouterEventsWSFactory) matsimControler.getTransitRouterFactory()).setWaitTime(waitTimes);
//            }
            throw new RuntimeException();
        }
        plancatcher.init();
        numberOfIterations++;
    }

    public double getTotalIterationTime() {
        double sumTimes = 0;
        for (long t : iterationTimes) {
            sumTimes += t;
        }
        return sumTimes;
    }

    private void addPersons(List<PersonSerializable> persons) {
        for (PersonSerializable person : persons) {
            matsimControler.getScenario().getPopulation().addPerson(person.getPerson());
        }
        slaveLogger.warn("Added " + persons.size() + " pax to my population.");
    }

    private List<PersonSerializable> getPersonsToSend(int diff) {
        int i = 0;
        List<PersonSerializable> personsToSend = new ArrayList<>();
        Set<Id<Person>> personIdsToRemove = new HashSet<>();
        for (Id<Person> personId : matsimControler.getScenario().getPopulation().getPersons().keySet()) {
            if (i++ >= diff) break;
            personsToSend.add(new PersonSerializable(matsimControler.getScenario().getPopulation().getPersons().get(personId)));
            personIdsToRemove.add(personId);
        }
        for (Id<Person> personId : personIdsToRemove)
            matsimControler.getScenario().getPopulation().getPersons().remove(personId);
        return personsToSend;
    }

    public void transmitPlans() throws IOException, ClassNotFoundException {
        Map<String, PlanSerializable> tempPlansCopyForSending = new HashMap<>();
        for (Person person : matsimControler.getScenario().getPopulation().getPersons().values()) {
            PlanSerializable planSerializable = new PlanSerializable(person.getSelectedPlan());
            planSerializable.pSimScore = planSerializable.getScore() == null ? 0 : planSerializable.getScore();
            tempPlansCopyForSending.put(person.getId().toString(), planSerializable);
        }
        Map<String, PlanSerializable> plansCopyForSending = tempPlansCopyForSending;
        slaveLogger.warn("Sending " + plansCopyForSending.size() + " plans...");
        writer.writeInt(currentIteration);
        writer.writeInt(masterCurrentIteration);
        writer.writeObject(plansCopyForSending);
        slaveLogger.warn("Sending completed.");

    }

    public void transmitTravelTimes() throws IOException, ClassNotFoundException {
        slaveLogger.warn("RECEIVING travel times...");
        masterCurrentIteration = reader.readInt();
        linkTravelTimes = (SerializableLinkTravelTimes) reader.readObject();
        if (config.transit().isUseTransit()) {
            stopStopTimes = (StopStopTime) reader.readObject();
            waitTimes = (WaitTime) reader.readObject();
            if (fullTransitPerformanceTransmission) {
                Object o = reader.readObject();
                transitPerformance = (TransitPerformance) o;
            }
        }
        slaveLogger.warn("RECEIVING travel times completed. Master at iteration number " + masterCurrentIteration);
    }

    public void transmitPerformance() throws IOException {
        if (totalIterationTime > 0) {
            slaveLogger.warn("Spent a total of " + totalIterationTime +
                    " running " + executedPlanCount +
                    " person plans for " + numberOfPSimIterationsPerCycle +
                    " PSim iterations.");
        }
        writer.writeDouble(totalIterationTime);
        writer.writeInt(matsimControler.getScenario().getPopulation().getPersons().size());
        //send memory usage fraction of max to prevent being assigned more persons
        writeMemoryStats();

    }

    private double getMemoryUse() {
        putOutTheGarbage();
        double totalMemory = Runtime.getRuntime().totalMemory();
        putOutTheGarbage();
        double freeMemory = Runtime.getRuntime().freeMemory();
        double usedMemoryEst = totalMemory - freeMemory;
        double maxMemory = Runtime.getRuntime().maxMemory();
        return usedMemoryEst / maxMemory;
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
            somethingWentWrong = true;
        }
    }

    public void distributePersons() throws IOException, ClassNotFoundException {
        int masterCurrentIteration = reader.readInt();
        List<PersonSerializable> personSerializables = (List<PersonSerializable>) reader.readObject();
        addPersons(personSerializables);
        iterationTimes = new ArrayList<>();
        executedPlanCount = 0;
        slaveLogger.warn("Received " + personSerializables.size() + " persons. Master.currentIteration = " + masterCurrentIteration);
    }

    public void poolPersons() throws IOException {
        slaveLogger.warn("Load balancing...");
        int diff = reader.readInt();
        slaveLogger.warn("Received " + diff + " as lb instr from master");
        List<PersonSerializable> personsToSend = new ArrayList<>();
        if (diff > 0) {
            personsToSend = getPersonsToSend(diff);
        }
        writer.writeObject(personsToSend);
        slaveLogger.warn("Sent " + personsToSend.size() + " pax to master");
    }

    public void communications() {
        CommunicationsMode communicationsMode = CommunicationsMode.WAIT;
        slaveLogger.warn("Initializing communications...");
        try {
            while (!communicationsMode.equals(CommunicationsMode.CONTINUE)) {
                communicationsMode = (CommunicationsMode) reader.readObject();
                switch (communicationsMode) {
                    case TRANSMIT_SCENARIO:
                        distributePersons();
                        break;
                    case TRANSMIT_TRAVEL_TIMES:
                        transmitTravelTimes();
                        break;
                    case POOL_PERSONS:
                        poolPersons();
                        break;
                    case DISTRIBUTE_PERSONS:
                        distributePersons();
                        break;
                    case TRANSMIT_PLANS_TO_MASTER:
                        transmitPlans();
                        slaveIsOKForNextIter();
                        break;
                    case TRANSMIT_SCORES:
                        transmitScores();
                        break;
                    case TRANSMIT_PERFORMANCE:
                        transmitPerformance();
                        break;
                    case CONTINUE:
                        break;
                    case WAIT:
                        Thread.sleep(10);
                        break;
                    case DIE:
                        slaveLogger.error("Got the kill signal from MASTER. Bye.");
                        Runtime.getRuntime().halt(0);
                        break;
                    case DUMP_PLANS:
                        dumpPlans();
                        break;
                }
                // sending a boolean forces the thread on the master to wait
                writer.writeBoolean(true);
                writer.flush();
            }
            writer.reset();
        } catch (ClassNotFoundException | IOException | InterruptedException e) {
            e.printStackTrace();
            slaveLogger.error("Something went wrong. Exiting.");
            somethingWentWrong = true;
            return;
        }
        slaveLogger.warn("Communications completed.");
    }

    private void transmitScores() throws IOException {
        writer.writeObject(new SlaveScoreStatsCalculator().calculateScoreStats(scenario.getPopulation()));
    }

    private void dumpPlans() throws IOException {
        List<PersonSerializable> temp = new ArrayList<>();
        for (Person p : scenario.getPopulation().getPersons().values())
            temp.add(new PersonSerializable(p));
        writer.writeObject(temp);
    }

    private void slaveIsOKForNextIter() throws IOException {
        writer.writeBoolean(isOkForNextIter);
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        communications();
    }

    public MatsimServices getMATSimControler() {
        return matsimControler;
    }

    public void addPlansForPsim(Plan plan) {


        plancatcher.addPlansForPsim(plan);
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        selectedPlanScoreMemory = new HashMap<>(scenario.getPopulation().getPersons().size());
        if (event.getIteration() == 0) {
            plancatcher.init();
            for (Person person : scenario.getPopulation().getPersons().values()) {
                plancatcher.addPlansForPsim(person.getSelectedPlan());
            }
        } else {
            for (Person person : scenario.getPopulation().getPersons().values()) {
                selectedPlanScoreMemory.put(person.getId(), person.getSelectedPlan().getScore());
            }
            for (Plan plan : plancatcher.getPlansForPSim()) {
                selectedPlanScoreMemory.remove(plan.getPerson().getId());
            }
        }

        executedPlanCount += plancatcher.getPlansForPSim().size();
        pSimFactory.setPlans(plancatcher.getPlansForPSim());
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        Iterator<Map.Entry<Id<Person>, Double>> iterator = selectedPlanScoreMemory.entrySet().iterator();
        StopStopTimeCalculatorSerializable.printCallStatisticsAndReset();
        WaitTimeCalculatorSerializable.printCallStatisticsAndReset();
        while (iterator.hasNext()) {
            Map.Entry<Id<Person>, Double> entry = iterator.next();
            scenario.getPopulation().getPersons().get(entry.getKey()).getSelectedPlan().setScore(entry.getValue());
        }
    }
}

class ReplaceableTravelTime implements TravelTime {
    private TravelTime delegate;

    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return this.delegate.getLinkTravelTime(link, time, person, vehicle);
    }

    public void setTravelTime(TravelTime linkTravelTimes) {
        this.delegate = linkTravelTimes;
    }
}