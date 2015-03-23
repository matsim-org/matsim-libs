package playground.pieter.distributed;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import playground.pieter.distributed.listeners.controler.GenomeAnalysis;
import playground.pieter.distributed.plans.router.DefaultTripRouterFactoryForPlanGenomesModule;
import playground.pieter.distributed.randomizedcarrouter.RandomizedCarRouterTravelTimeAndDisutilityModule;
import playground.pieter.distributed.replanning.DistributedPlanStrategyTranslationAndRegistration;
import playground.pieter.distributed.scoring.CharyparNagelOpenTimesScoringFunctionFactoryForPlanGenomes;
import playground.singapore.scoring.CharyparNagelOpenTimesScoringFunctionFactory;
import playground.singapore.transitRouterEventsBased.TransitRouterEventsWSFactory;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTimeCalculatorSerializable;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTimeCalculatorSerializable;
import playground.vsp.planselectors.DiversityGeneratingPlansRemover;
import playground.vsp.randomizedtransitrouter.RandomizedTransitRouterModule;

public class ControlerReference {
    private final Controler delegate;
    private Logger logger = Logger.getLogger(this.getClass());

    private ControlerReference(String[] args) throws ParseException {
        System.setProperty("matsim.preferLocalDtds", "true");
        Options options = new Options();
        options.addOption("c", "config", true, "Config file location");
        options.addOption("s", "singapore", false, "Switch to indicate if this is the Singapore scenario, i.e. special scoring function");
        options.addOption("g", "genomeTracking", false, "Track plan genomes");
        options.addOption("I", "IntelligentRouters", false, "Intelligent routers");
        options.addOption("D", "Diversify", false, "Use the DiversityGeneratingPlansRemover");
//        CommandLineParser parser = new BasicParser();
        CommandLineParser parser = new GnuParser();
        CommandLine commandLine = parser.parse(options, args);


        Config config = ConfigUtils.loadConfig(commandLine.getOptionValue("c"), new DestinationChoiceConfigGroup());

        boolean trackGenome = commandLine.hasOption("genomeTracking") || commandLine.hasOption("g");
        boolean singapore = commandLine.hasOption("singapore") || commandLine.hasOption("s");
        boolean intelligentRouters = commandLine.hasOption("IntelligentRouters") || commandLine.hasOption("I");
        boolean diversityGeneratingPlanSelection = commandLine.hasOption("Diversify") || commandLine.hasOption("D");
        if (trackGenome) {

            DistributedPlanStrategyTranslationAndRegistration.TrackGenome = true;
            DistributedPlanStrategyTranslationAndRegistration.substituteStrategies(config, false, 1);
        }
        Scenario scenario = ScenarioUtilsForPlanGenomes.buildAndLoadScenario(config, trackGenome, false);
        this.delegate = new Controler(scenario);
        if (commandLine.hasOption("g")) {
            new DistributedPlanStrategyTranslationAndRegistration(this.delegate, null, false, 1);
        }
        delegate.setOverwriteFiles(true);

        if (singapore) {
            logger.warn("Singapore scoring function");
            delegate.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(delegate.getConfig().planCalcScore(), delegate.getScenario()));

//        for (Link link : scenario.getNetwork().getLinks().values()) {
//            Set<String> modes = new HashSet<>(link.getAllowedModes());
//            modes.add("pt");
//            link.setAllowedModes(modes);
//        }
            //this is some more magic hacking to get location choice by car to work, by sergioo
            //sergioo creates a car-only network, then associates each activity and facility with a car link.
//        Set<String> carMode = new HashSet<>();
//        carMode.add("car");
//        NetworkImpl justCarNetwork = NetworkImpl.createNetwork();
//        new TransportModeNetworkFilter(scenario.getNetwork()).filter(justCarNetwork, carMode);
//        for (Person person : scenario.getPopulation().getPersons().values())
//            for (PlanElement planElement : person.getSelectedPlan().getPlanElements())
//                if (planElement instanceof Activity)
//                    ((ActivityImpl) planElement).setLinkId(justCarNetwork.getNearestLinkExactly(((ActivityImpl) planElement).getCoord()).getId());
//        for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values())
//            ((ActivityFacilityImpl) facility).setLinkId(justCarNetwork.getNearestLinkExactly(facility.getCoord()).getId());

//        delegate.addPlanStrategyFactory("TransitLocationChoice", new PlanStrategyFactory() {
//            @Override
//            public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager) {
//                return new TransitLocationChoiceStrategy(scenario);
//            }
//        });
//        delegate.setMobsimFactory(new PTQSimFactory());
        }
        if (intelligentRouters) {
            logger.warn("Smart routing");
            StopStopTimeCalculatorSerializable stopStopTimes = new StopStopTimeCalculatorSerializable(scenario.getTransitSchedule(),
                    config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config
                    .qsim().getEndTime() - config.qsim().getStartTime()));

            WaitTimeCalculatorSerializable waitTimes = new WaitTimeCalculatorSerializable(scenario.getTransitSchedule(),
                    config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config
                    .qsim().getEndTime() - config.qsim().getStartTime()));
            delegate.getEvents().addHandler(waitTimes);
            delegate.getEvents().addHandler(stopStopTimes);
            delegate.setTransitRouterFactory(new TransitRouterEventsWSFactory(delegate.getScenario(), waitTimes.getWaitTimes(), stopStopTimes.getStopStopTimes()));
        } else {
            delegate.addOverridingModule(new RandomizedCarRouterTravelTimeAndDisutilityModule());
            delegate.addOverridingModule(new RandomizedTransitRouterModule());
        }
        if (trackGenome) {
            delegate.addOverridingModule(new DefaultTripRouterFactoryForPlanGenomesModule());
//            delegate.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactoryForPlanGenomes(config.planCalcScore(), scenario, singapore));
            delegate.addControlerListener(new GenomeAnalysis(false, false, false));
        }
        delegate.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addPlanSelectorForRemovalBinding("DiversityGeneratingPlansRemover").toProvider(playground.pieter.distributed.replanning.selectors.DiversityGeneratingPlansRemover.Builder.class);
            }
        });
        if(diversityGeneratingPlanSelection)
            delegate.getConfig().strategy().setPlanSelectorForRemoval("DiversityGeneratingPlansRemover");

        String outputDirectory = config.controler().getOutputDirectory();
        outputDirectory += "_ref" +
                (trackGenome ? "_g" : "") +
                (singapore ? "_s" : "") +
                (intelligentRouters ? "_I" : "") +
                (diversityGeneratingPlanSelection ? "_D" : "")
        ;
        config.controler().setOutputDirectory(outputDirectory);
    }

    public static void main(String args[]) throws ParseException {
        new ControlerReference(args).run();
    }

    private void run() {
        delegate.run();

    }

}
