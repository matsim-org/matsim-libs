/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 *
 */
package playground.johannes.gsv.sim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import playground.johannes.coopsim.analysis.*;
import playground.johannes.coopsim.pysical.TrajectoryEventsBuilder;
import playground.johannes.gsv.analysis.CountsCompareAnalyzer;
import playground.johannes.gsv.analysis.PkmGeoTask;
import playground.johannes.gsv.sim.cadyts.CadytsContext;
import playground.johannes.gsv.sim.cadyts.CadytsScoring;
import playground.johannes.gsv.sim.cadyts.ODAdjustorListener;
import playground.johannes.gsv.synPop.Proxy2Matsim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 */
public class Simulator {

    private static final Logger logger = Logger.getLogger(Simulator.class);

    public static void main(String[] args) throws IOException {
        final GsvConfigGroup gsvConfigGroup = new GsvConfigGroup();
//		gsvConfigGroup.setCountsfile("examples/equil/counts100.xml");
//		gsvConfigGroup.setAttributesFile("examples/tutorial/programming/multipleSubpopulations/personAtrributes.xml");
        final Config config = ConfigUtils.loadConfig(args[0], gsvConfigGroup);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controler().setDumpDataAtEnd(false);
//        StrategySettings settings = new StrategySettings(Id.create(1, StrategySettings.class));
//        settings.setStrategyName("activityLocations");
//        config.strategy().addStrategySettings(settings);
//        settings = new StrategySettings(Id.create(2, StrategySettings.class));
//        settings.setStrategyName("doNothing");
//        config.strategy().addStrategySettings(settings);

        final Scenario scenario = ScenarioUtils.loadScenario(config);
        final Controler controler = new Controler(scenario);

        logger.info("Setting up services modules...");
        controler.setModules(new AbstractModule() {
            @Override
            public void install() {
//                install(new CharyparNagelScoringFunctionModule());
                install(new EventsManagerModule());
                // include(new TravelTimeCalculatorModule());
                install(new TravelDisutilityModule());
                install(new TripRouterModule());
                install(new StrategyManagerModule());
                // include(new LinkStatsModule());
                // include(new VolumesAnalyzerModule());
                // include(new LegHistogramModule());
                // include(new LegTimesModule());
                // include(new ScoreStatsModule());
                // include(new CountsModule());
                // include(new PtCountsModule());
                // include(new VspPlansCleanerModule());
                // include(new SignalsModule());

				/*
                 * setup scoring and cadyts integration
		 		*/
                logger.info("Setting up cadyts...");
                LinkOccupancyCalculator calculator = new LinkOccupancyCalculator(scenario.getPopulation());
                addEventHandlerBinding().toInstance(calculator);
                if (!gsvConfigGroup.isDisableCadyts()) {
                    // Consider using the "official" CadytsCarModule..
                    final CadytsContext context = new CadytsContext(config, null, calculator);
                    bindScoringFunctionFactory().toInstance(new ScoringFactory(context, config, scenario.getNetwork()));
                    addControlerListenerBinding().toInstance(context);
                    addControlerListenerBinding().toInstance(new CadytsRegistration(context));
                    // Must be bound because MobsimConnectorFactory wants it
                    bind(CadytsContext.class).toInstance(context);
                }

                addControlerListenerBinding().toInstance(new ControllerSetup());
                addControlerListenerBinding().toInstance(new DTVAnalyzer(gsvConfigGroup.getCountsfile(), calculator, config.counts().getCountsScaleFactor()));
                addControlerListenerBinding().toInstance(new CountsCompareAnalyzer(calculator, gsvConfigGroup.getCountsfile(), config.counts().getCountsScaleFactor()));

                addTravelTimeBinding(TransportMode.car).toInstance(MobsimConnectorFactory.getTravelTimeCalculator(1.5));
                bindMobsim().toProvider(MobsimConnectorFactory.class);
                addPlanStrategyBinding("doNothing").toInstance(new PlanStrategy() {

                    @Override
                    public void run(HasPlansAndId<Plan, Person> person) {
                    }

                    @Override
                    public void init(ReplanningContext replanningContext) {
                    }

                    @Override
                    public void finish() {
                    }
                });
                logger.info("Setting up activity location strategy...");
                addPlanStrategyBinding("activityLocations").to(WrappedActivityLocationStrategy.class);
            }
        });

        // services.addOverridingModule(abstractModule);
		/*
		 * load person attributes
		 */
        logger.info("Loading person attributes...");
        ObjectAttributesXmlReader oaReader = new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes());
        oaReader.putAttributeConverter(ArrayList.class, new Proxy2Matsim.Converter());
        oaReader.parse(gsvConfigGroup.getAttributesFile());

        controler.run();

    }

    private static class ControllerSetup implements StartupListener {

        @Override
        public void notifyStartup(StartupEvent event) {
			/*
			 * connect facilities to links
			 */
            logger.info("Connecting facilities to links...");
            NetworkImpl network = (NetworkImpl) event.getServices().getScenario().getNetwork();
            for (ActivityFacility facility : event.getServices().getScenario().getActivityFacilities().getFacilities().values()) {
                Coord coord = facility.getCoord();
                Link link = NetworkUtils.getNearestLink(network, coord);
                ((ActivityFacilityImpl) facility).setLinkId(link.getId());
            }
			
			/*
			 * setup analysis modules
			 */
            logger.info("Setting up analysis modules...");
            TrajectoryAnalyzerTaskComposite task = new TrajectoryAnalyzerTaskComposite();
            task.addTask(new TripGeoDistanceTask(event.getServices().getScenario().getActivityFacilities()));
//			task.addTask(new SpeedFactorTask(services.getScenario().getActivityFacilities()));
//			task.addTask(new ScoreTask());
            task.addTask(new PkmGeoTask(event.getServices().getScenario().getActivityFacilities()));
            task.addTask(new PkmRouteTask(event.getServices().getScenario().getNetwork(), 0));
            task.addTask(new PkmRouteTask(event.getServices().getScenario().getNetwork(), 0.5));
            task.addTask(new PkmRouteTask(event.getServices().getScenario().getNetwork(), 1));
            // task.addTask(new ModeShareTask());
            // task.addTask(new ActivityDurationTask());
            // task.addTask(new ActivityLoadTask());
            // task.addTask(new LegLoadTask());
//			task.addTask(new TripDurationTask());
//			task.addTask(new TripPurposeShareTask());
            // task.addTask(new LegFrequencyTask());
            task.addTask(new TripCountTask());

            AnalyzerListiner listener = new AnalyzerListiner();
            listener.task = task;
            listener.interval = 5;
            listener.notifyStartup(event);

            event.getServices().addControlerListener(listener);
			/*
			 * Setup ODAdjustor
			 */
            logger.info("Setting up ODAdjustor...");
            ODAdjustorListener odAdjustor = new ODAdjustorListener(event.getServices());
            event.getServices().addControlerListener(odAdjustor);
        }

    }

    private static class CadytsRegistration implements AfterMobsimListener {

        private CadytsContext context;

        public CadytsRegistration(CadytsContext context) {
            this.context = context;
        }

        @Override
        public void notifyAfterMobsim(AfterMobsimEvent event) {
            Population population = event.getServices().getScenario().getPopulation();
            for (Person person : population.getPersons().values()) {
                context.getCalibrator().addToDemand(context.getPlansTranslator().getCadytsPlan(person.getSelectedPlan()));
            }

        }

    }

    private static class ScoringFactory implements ScoringFunctionFactory {

        // private ScoringFunction function;

        private CadytsContext context;

        private Config config;

        public ScoringFactory(CadytsContext context, Config config, Network network) {
            this.context = context;
            this.config = config;
        }

        @Override
        public ScoringFunction createNewScoringFunction(Person person) {
            SumScoringFunction sum = new SumScoringFunction();
            CadytsScoring scoringFunction = new CadytsScoring(person.getSelectedPlan(), config, context);
            sum.addScoringFunction(scoringFunction);
            return sum;
        }

    }

    private static class AnalyzerListiner implements IterationEndsListener, IterationStartsListener, StartupListener {

        private TrajectoryAnalyzerTask task;

        private TrajectoryEventsBuilder builder;

        private int interval;

        @Override
        public void notifyIterationEnds(IterationEndsEvent event) {
            try {
                if (event.getIteration() % interval == 0) {
                    TrajectoryAnalyzer.analyze(builder.trajectories(), task, event.getServices().getControlerIO().getIterationPath(event.getIteration()));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void notifyIterationStarts(IterationStartsEvent event) {
            builder.reset(event.getIteration());
        }

        @Override
        public void notifyStartup(StartupEvent event) {

            Set<Person> person = new HashSet<Person>(event.getServices().getScenario().getPopulation().getPersons().values());
            builder = new TrajectoryEventsBuilder(person);
            event.getServices().getEvents().addHandler(builder);
        }
    }
}
