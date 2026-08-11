package org.matsim.contrib.pseudosimulation.distributed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.common.diversitygeneration.planselectors.DiversityGeneratingPlansRemover;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;
import org.matsim.contrib.pseudosimulation.mobsim.PSimProvider;
import org.matsim.contrib.pseudosimulation.replanning.DistributedPlanStrategyTranslationAndRegistration;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import com.google.inject.Inject;

//IMPORTANT: PSim produces events that are not in chronological order. This controler
// will require serious overhaul if chronological order is enforced in all event manager implementations
public class SlaveControler implements IterationStartsListener, StartupListener, BeforeMobsimListener, IterationEndsListener, ShutdownListener, Runnable {
    public static int numberOfPSimIterationsPerCycle;
    private final Scenario scenario;
    private final MemoryUsageCalculator memoryUsageCalculator;
    private final ReplaceableTravelTime travelTime;
    private final boolean fullTransitPerformanceTransmission;
    private final boolean IntelligentRouters;
    private final Logger slaveLogger;
    @Inject private  PlanCatcher plancatcher;
    private static double slaveMutationRate;
    private final int numberOfPlansOnSlave;
    private boolean initialRouting;
    private int masterCurrentIteration = -1;
    private Config config;
    private Controler matsimControler;
    private TravelTime linkTravelTimes;
    private SlaveMasterSession masterSession;
    private PSimProvider pSimProvider;
    private boolean isOkForNextIter = true;
    private TransitPerformance transitPerformance;
    private final SlaveIterationOrchestrator iterationOrchestrator;
    private final SelectedPlanScorePreserver scorePreserver = new SelectedPlanScorePreserver();

    public SlaveControler(String[] args) throws IOException, ClassNotFoundException, ParseException, InterruptedException {
        iterationOrchestrator = new SlaveIterationOrchestrator(iterationOperations());
        System.setProperty("matsim.preferLocalDtds", "true");
        SlaveCommandLineParser commandLineParser = new SlaveCommandLineParser();
        SlaveLaunchArguments launchArguments = commandLineParser.parse(args);
        if (launchArguments.configFile() != null) {
            try {
                config = ConfigUtils.loadConfig(launchArguments.configFile());
            } catch (UncheckedIOException e) {
                System.err.println("Config file not found");
                commandLineParser.printHelp();
                System.exit(1);
            }
        } else {
            System.err.println("Config file not specified");
            System.out.println(commandLineParser.optionsDescription());
            System.exit(1);
        }


        final DistributedSimConfigGroup distributedSimConfigGroup = ConfigUtils.addOrGetModule(this.config,DistributedSimConfigGroup.GROUP_NAME,DistributedSimConfigGroup.class);
        SlaveConfigPreparer configPreparer = new SlaveConfigPreparer();
        SlaveConfigPreparer.SlaveConnectionSettings connectionSettings = configPreparer.prepareForConnection(
                config, distributedSimConfigGroup, launchArguments, System.out, System.err,
                commandLineParser.optionsDescription());
        memoryUsageCalculator = new MemoryUsageCalculator();

        /*
        * INITIALIZING COMMS
        * */
        SlaveMasterSession.Connected connection = SlaveMasterSession.connect(connectionSettings, masterContext());
        masterSession = connection.session();
        SlaveMasterSession.Initialization initialization = connection.initialization();
        int myNumber = initialization.number();
        slaveLogger = masterSession.logger();

        numberOfPSimIterationsPerCycle = initialization.iterationsPerCycle();
        numberOfPlansOnSlave = initialization.numberOfPlans();
        slaveMutationRate = initialization.mutationRate();
        slaveLogger.warn("Running " + numberOfPSimIterationsPerCycle + " PSim iterations for every QSim iter");
        config.controller().setLastIteration(initialization.lastIteration());
        initialRouting = initialization.initialRouting();
        iterationOrchestrator.configure(initialRouting, numberOfPSimIterationsPerCycle);
        boolean quickReplannning = initialization.quickReplanning();
        fullTransitPerformanceTransmission = initialization.fullTransitPerformance();
        boolean trackGenome = initialization.trackGenome();
        IntelligentRouters = initialization.intelligentRouters();
        boolean diversityGeneratingPlanSelection = initialization.diversityGeneratingPlanSelection();

        if (initialRouting) slaveLogger.warn("Performing initial routing.");

        configPreparer.prepareForScenario(config, myNumber);
        if (slaveMutationRate > 0)
            new ReplanningWeightUpdater().updateSlave(config, slaveMutationRate);

        scenario = ScenarioUtils.loadScenario(config);

        //experimental, not to be used
        DistributedPlanStrategyTranslationAndRegistration.TrackGenome = trackGenome;
//        strategy substitution: mainly to register whether the option for quick replanning is to be used,
//        as its original function is to mark plans for execution by PSim. But here, all plans are executed by PSim
//        should rather be that PSim marks activities for execution in some other way
        DistributedPlanStrategyTranslationAndRegistration.substituteStrategies(config, quickReplannning, numberOfPSimIterationsPerCycle);
        matsimControler = new Controler(scenario);
        DistributedPlanStrategyTranslationAndRegistration.registerStrategiesWithControler(this.matsimControler, plancatcher, quickReplannning, numberOfPSimIterationsPerCycle);
        matsimControler.getConfig().controller().setOverwriteFileSetting(
                true ?
                        OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
                        OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
        matsimControler.addControllerListener(this);

//        init traveltime for when non yet has been received from master
        travelTime = new ReplaceableTravelTime();
        linkTravelTimes = new FreeSpeedTravelTime();
        travelTime.setTravelTime(linkTravelTimes);

        matsimControler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bindMobsim().toProvider(pSimProvider);
            }
        });

        if (config.transit().isUseTransit()) {
            // tell PlanSerializable to record transit routes
            PlanSerializable.isUseTransit = true;

        }
        if (IntelligentRouters)
            matsimControler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
//                    System.out.println("init routers");
//                    transitRouterEventsWSFactory = new TransitRouterEventsWSFactory(scenario,
//                            waitTimes,
//                            stopStopTimes);
//                    bind(TransitRouter.class).toProvider(transitRouterEventsWSFactory);
                }
            });

        else {
            final RandomizingTimeDistanceTravelDisutilityFactory disutilityFactory =
                    new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config);
            matsimControler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addTravelDisutilityFactoryBinding(TransportMode.car).toInstance(disutilityFactory);
                }
            });

        }
        matsimControler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(TravelTime.class).toInstance(travelTime);
                if (scenario.getConfig().replanning().getPlanSelectorForRemoval().equals("DiversityGeneratingPlansRemover")) {
                    bindPlanSelectorForRemoval().toProvider(DiversityGeneratingPlansRemover.Builder.class);
                }
            }
        });


        if (trackGenome) {

        }
        if (diversityGeneratingPlanSelection)
            matsimControler.getConfig().replanning().setPlanSelectorForRemoval("DiversityGeneratingPlansRemover");
        //no use for this, if you don't exactly know the communicationsMode of population when something goes wrong.
        // better to have plans written out every n successful iterations, specified in the config
        configPreparer.prepareController(matsimControler.getConfig(), numberOfPlansOnSlave);
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, ParseException, InterruptedException {
        SlaveControler slave = new SlaveControler(args);
        new Thread(slave).start();
//        System.out.printf("Enter KILL to kill the slave: ");
//        Scanner in = new Scanner(System.in);
//        String s;
//        boolean running = true;
//        do {
//            s = in.nextLine();
//            if (s.equals("KILL"))
//                running = false;
//        } while (running);
//        slave.requestShutDown();
    }

    public Config getConfig() {
        return config;
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
        iterationOrchestrator.iterationStarts(event.getIteration());
    }

    public double getTotalIterationTime() {
        return iterationOrchestrator.sumIterationTimes();
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

    public void communications() {
        iterationOrchestrator.communicateNow();
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        iterationOrchestrator.startup();
    }


    public void addPlansForPsim(Plan plan) {


        plancatcher.addPlansForPsim(plan);
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        scorePreserver.beforeMobsim(event.getIteration(), scenario.getPopulation(), plancatcher);
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
//        StopStopTimeCalculatorSerializable.printCallStatisticsAndReset();
//        WaitTimeCalculatorSerializable.printCallStatisticsAndReset();
        scorePreserver.afterMobsim(scenario.getPopulation());
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        iterationOrchestrator.shutdown();
    }

    private SlaveMasterSession.Context masterContext() {
        return new SlaveMasterSession.Context() {
            public Iterable<? extends Person> persons() { return scenario.getPopulation().getPersons().values(); }
            public org.matsim.api.core.v01.population.Population population() { return scenario.getPopulation(); }
            public int currentIteration() { return iterationOrchestrator.currentIteration(); }
            public int masterIteration() { return masterCurrentIteration; }
            public boolean transitEnabled() { return config.transit().isUseTransit(); }
            public boolean fullTransitPerformance() { return fullTransitPerformanceTransmission; }
            public void updateTravelTimes(int iteration, SerializableLinkTravelTimes times,
                    TransitPerformance performance) {
                masterCurrentIteration = iteration;
                linkTravelTimes = times;
                transitPerformance = performance;
            }
            public void addPersons(List<PersonSerializable> persons, int ignoredMasterIteration) {
                SlaveControler.this.addPersons(persons);
                iterationOrchestrator.resetIterationTimes();
                scorePreserver.resetExecutedPlanCount();
            }
            public List<PersonSerializable> takePersons(int difference) { return getPersonsToSend(difference); }
            public boolean ready() { return isOkForNextIter; }
            public double totalIterationTime() { return iterationOrchestrator.totalIterationTime(); }
            public int executedPlanCount() { return scorePreserver.executedPlanCount(); }
            public int iterationsPerCycle() { return numberOfPSimIterationsPerCycle; }
            public int populationSize() { return scenario.getPopulation().getPersons().size(); }
            public long memoryUse() { return memoryUsageCalculator.getMemoryUse(); }
            public long maximumMemory() { return Runtime.getRuntime().maxMemory(); }
            public int totalNumberOfPlans() { return getTotalNumberOfPlans(); }
        };
    }

    private SlaveIterationOrchestrator.Operations iterationOperations() {
        return new SlaveIterationOrchestrator.Operations() {
            public boolean communicate() { return masterSession.communicate(); }
            public void halt() { Runtime.getRuntime().halt(0); }
            public void activateTravelTime() {
                travelTime.setTravelTime(linkTravelTimes);
                pSimProvider.setTravelTime(linkTravelTimes);
            }
            public void initializePlanCatcher() { plancatcher.init(); }
        };
    }
}
