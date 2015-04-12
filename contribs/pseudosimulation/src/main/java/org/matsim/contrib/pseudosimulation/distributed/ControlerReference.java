package org.matsim.contrib.pseudosimulation.distributed;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.diversitygeneration.planselectors.DiversityGeneratingPlansRemover;
import org.matsim.contrib.pseudosimulation.distributed.listeners.controler.GenomeAnalysis;
import org.matsim.contrib.pseudosimulation.distributed.replanning.DistributedPlanStrategyTranslationAndRegistration;
import org.matsim.contrib.common.randomizedtransitrouter.RandomizedTransitRouterModule;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculatorSerializable;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculatorSerializable;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;

public class ControlerReference {
    private final Controler delegate;

    private ControlerReference(String[] args) throws ParseException {
        System.setProperty("matsim.preferLocalDtds", "true");
        Options options = new Options();
        options.addOption("c", "config", true, "Config file location");
        options.addOption("s", "singapore", false, "Switch to indicate if this is the Singapore scenario, i.e. special scoring function");
        options.addOption("g", "genomeTracking", false, "Track plan genomes");
        options.addOption("I", "IntelligentRouters", false, "Intelligent routers");
        options.addOption("D", "Diversify", false, "Use the DiversityGeneratingPlansRemover");
        options.addOption(OptionBuilder.withLongOpt("appendString")
                .withDescription("Optional string without spaces to append to end of output directory name.")
                .hasArg(true)
                .withArgName("STRING")
                .create("a"));
//        CommandLineParser parser = new BasicParser();
        CommandLineParser parser = new GnuParser();
        CommandLine commandLine = parser.parse(options, args);

        String appendString=commandLine.hasOption("appendString")||commandLine.hasOption("a")?commandLine.getOptionValue("a"):"";

        Config config = ConfigUtils.loadConfig(commandLine.getOptionValue("c"));

        boolean trackGenome = commandLine.hasOption("genomeTracking") || commandLine.hasOption("g");
        boolean singapore = commandLine.hasOption("singapore") || commandLine.hasOption("s");
        boolean intelligentRouters = commandLine.hasOption("IntelligentRouters") || commandLine.hasOption("I");
        boolean diversityGeneratingPlanSelection = commandLine.hasOption("Diversify") || commandLine.hasOption("D");
        if (trackGenome) {

            DistributedPlanStrategyTranslationAndRegistration.TrackGenome = true;
            DistributedPlanStrategyTranslationAndRegistration.substituteStrategies(config, false, 1);
        }
        Scenario scenario = ScenarioUtils.loadScenario(config);
        this.delegate = new Controler(scenario);
        if (commandLine.hasOption("g")) {
            new DistributedPlanStrategyTranslationAndRegistration(this.delegate, null, false, 1);
        }


        if (intelligentRouters) {
            Logger logger = Logger.getLogger(this.getClass());
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
            TravelTimeAndDistanceBasedTravelDisutilityFactory disutilityFactory =
                    new TravelTimeAndDistanceBasedTravelDisutilityFactory();
            delegate.setTravelDisutilityFactory(disutilityFactory);
            disutilityFactory.setSigma(0.1);
            delegate.addOverridingModule(new RandomizedTransitRouterModule());
        }
        if (trackGenome) {
            delegate.addControlerListener(new GenomeAnalysis(false, false, false));
        }
        delegate.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addPlanSelectorForRemovalBinding("DiversityGeneratingPlansRemover").toProvider(DiversityGeneratingPlansRemover.Builder.class);
            }
        });
        if(diversityGeneratingPlanSelection)
            delegate.getConfig().strategy().setPlanSelectorForRemoval("DiversityGeneratingPlansRemover");

        String outputDirectory = config.controler().getOutputDirectory();
        outputDirectory += "_ref" +
                (trackGenome ? "_g" : "") +
                (singapore ? "_s" : "") +
                (intelligentRouters ? "_I" : "") +
                (diversityGeneratingPlanSelection ? "_D" : "") +
                appendString;
        config.controler().setOutputDirectory(outputDirectory);
    }

    public static void main(String args[]) throws ParseException {
        new ControlerReference(args).run();
    }

    private void run() {
        delegate.run();

    }

}
