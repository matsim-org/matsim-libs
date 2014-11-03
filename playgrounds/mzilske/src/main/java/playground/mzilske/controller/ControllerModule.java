/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ControllerModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.controller;


import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.matsim.analysis.*;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.SnapshotWriterFactoryRegister;
import org.matsim.core.controler.SnapshotWriterRegistrar;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutilityFactory;
import org.matsim.core.router.util.*;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.counts.CountControlerListener;
import org.matsim.counts.Counts;
import org.matsim.population.VspPlansCleaner;
import org.matsim.pt.counts.PtCountControlerListener;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImplFactory;
import org.matsim.signalsystems.controler.DefaultSignalsControllerListenerFactory;
import org.matsim.signalsystems.controler.SignalsControllerListenerFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriterFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ControllerModule extends AbstractModule {

    @Override
    protected void configure() {
        requireBinding(Config.class);
        requireBinding(Scenario.class);
        requireBinding(ScoringFunctionFactory.class);
        Multibinder.newSetBinder(binder(), ControlerListener.class);
        MapBinder.newMapBinder(binder(), String.class, PlanStrategyFactory.class);
        MapBinder.newMapBinder(binder(), String.class, MobsimFactory.class);

        bind(Controller.class).to(InjectableController.class).in(Singleton.class);
        bind(ScoreStats.class).to(ScoreStatsControlerListener.class).in(Singleton.class);
        bind(Controler.TerminationCriterion.class).to(IsLastIteration.class);
        bind(Runnable.class).annotatedWith(Names.named("prepareForSim")).to(PrepareForSim.class);
        bind(TripRouter.class).toProvider(TripRouterProvider.class);
        bind(TripRouterFactory.class).toProvider(TripRouterFactoryProvider.class).in(Singleton.class);
        bind(SignalsControllerListenerFactory.class).to(DefaultSignalsControllerListenerFactory.class);
        bind(TravelTimeCalculatorFactory.class).to(TravelTimeCalculatorFactoryImpl.class);
        bind(TravelDisutilityFactory.class).to(TravelTimeAndDistanceBasedTravelDisutilityFactory.class);
        bind(StrategyManager.class).toProvider(StrategyManagerProvider.class).in(Singleton.class);
        bind(ReplanningContextFactory.class).to(ReplanningContextFactoryImpl.class);
    }

    @Provides Population providePopulation(Scenario scenario) {
        return scenario.getPopulation();
    }

    @Provides Network provideNetwork(Scenario scenario) {
        return scenario.getNetwork();
    }

    @Provides @Singleton EventsManager provideEventsManager(Config config) {
        return EventsUtils.createEventsManager(config);
    }

    @Provides CharyparNagelScoringFunctionFactory provideChariparNagelScoringFunctionFactory(Config config, Network network) {
        return new CharyparNagelScoringFunctionFactory(config.planCalcScore(), network);
    }

    @Provides
    OutputDirectoryHierarchy provideOutputDirectoryHierarchy(InjectableController injectableController) {
        return injectableController.getControlerIO();
    }

    @Provides
    List<MobsimListener> provideMobsimListeners() {
        return Collections.emptyList();
    }

    @Provides @Named("coreControlerListeners")
    List<ControlerListener> provideCoreControlerListeners(
            Config config,
            Scenario scenarioData,
            OutputDirectoryHierarchy controlerIO,
            ScoringFunctionFactory scoringFunctionFactory,
            EventsManager events,
            IterationStopWatch stopwatch,
            CalcLegTimes calcLegTimes,
            ReplanningControlerListener replanningControlerListener) {
        List<ControlerListener> result = new ArrayList<ControlerListener>();
        result.add(new DumpDataAtEnd(scenarioData, controlerIO));
        result.add(new PlansScoring(scenarioData, events, controlerIO, scoringFunctionFactory));
        result.add(replanningControlerListener);
        result.add(new PlansDumping(scenarioData, config.controler().getFirstIteration(), config.controler().getWritePlansInterval(), stopwatch, controlerIO));
        result.add(new LegTimesControlerListener(calcLegTimes, controlerIO));
        result.add(new EventsHandling(events, config.controler().getWriteEventsInterval(), config.controler().getEventsFileFormats(), controlerIO));
        return result;
    }

    @Provides
    ScoreStatsControlerListener provideScoreStatsControlerListener(Population population, OutputDirectoryHierarchy controlerIO, Config config) {
        return new ScoreStatsControlerListener(config, population,
                controlerIO.getOutputFilename(Controler.FILENAME_SCORESTATS), config.controler().isCreateGraphs());
    }

    @Provides @Named("defaultControlerListeners")
    List<ControlerListener> provideDefaultControlerListeners(
            Config config,
            Scenario scenarioData,
            EventsManager events,
            ScoreStatsControlerListener scoreStats,
            SignalsControllerListenerFactory signalsFactory,
            TravelTimeCalculator travelTimeCalculator,
            VolumesAnalyzer volumes,
            CalcLinkStats linkStats,
            OutputDirectoryHierarchy controlerIO) {
        List<ControlerListener> result = new ArrayList<ControlerListener>();
        result.add(scoreStats);
        if (config.counts().getCountsFileName() != null || scenarioData.getScenarioElement(Counts.ELEMENT_NAME) != null) {
            CountControlerListener ccl = new CountControlerListener(config.counts());
            result.add(ccl);
        }
        if (config.scenario().isUseTransit()) {
            if (config.ptCounts().getAlightCountsFileName() != null) {
                result.add(new PtCountControlerListener(config));
            }
        }
        if (config.scenario().isUseSignalSystems()) {
            result.add(signalsFactory.createSignalsControllerListener());
        }
        VspExperimentalConfigGroup.ActivityDurationInterpretation actDurInterpr = config.plans().getActivityDurationInterpretation() ;
        if ( actDurInterpr != VspExperimentalConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime || config.vspExperimental().isRemovingUnneccessaryPlanAttributes() ) {
            result.add(new VspPlansCleaner(scenarioData));
        }
        return result;
    }

    @Provides @Singleton @Named("fullyLoadedEventsManager")
    EventsManager provideFullyLoadedEventsManager(EventsManager eventsManager, List<EventHandler> eventHandlers) {
        for (EventHandler eventHandler : eventHandlers) {
            eventsManager.addHandler(eventHandler);
        }
        return eventsManager;
    }

    @Provides
    List<EventHandler> provideEventHandlers(VolumesAnalyzer volumesAnalyzer, CalcLegTimes calcLegTimes, TravelTimeCalculator travelTimeCalculator) {
        List<EventHandler> result = new ArrayList<EventHandler>();
        result.add(volumesAnalyzer);
        result.add(calcLegTimes);
        result.add(travelTimeCalculator);
        return result;
    }

    @Provides
    IterationStopWatch provideIterationStopWatch(InjectableController controller) {
        return controller.stopwatch;
    }

    @Provides @Singleton
    CalcLinkStats provideLinkStats(Network network) {
        return new CalcLinkStats(network);
    }

    @Provides @Singleton
    VolumesAnalyzer provideVolumesAnalyzer(Network network) {
        VolumesAnalyzer volumesAnalyzer = new VolumesAnalyzer(3600, 24 * 3600 - 1, network);
        return volumesAnalyzer;
    }

    @Provides @Singleton
    CalcLegTimes provideCalcLegTimes() {
        CalcLegTimes calcLegTimes = new CalcLegTimes();
        return calcLegTimes;
    }

    @Provides @Singleton
    TravelTimeCalculator provideTravelTimeCalculator(
            Config config,
            Network network,
            TravelTimeCalculatorFactory travelTimeCalculatorFactory) {
        TravelTimeCalculator travelTimeCalculator = travelTimeCalculatorFactory.createTravelTimeCalculator(network, config.travelTimeCalculator());
        return travelTimeCalculator;
    }

    @Provides @Singleton
    TransitRouterFactory provideTransitRouterFactory(Scenario scenario) {
        Config config = scenario.getConfig();
        if(config.scenario().isUseTransit()) {
            return new TransitRouterImplFactory(
                    scenario.getTransitSchedule(),
                    new TransitRouterConfig(
                            config.planCalcScore(),
                            config.plansCalcRoute(),
                            config.transitRouter(),
                            config.vspExperimental())
            );
        } else {
            return null;
        }
    }

    @Provides @Singleton
    LeastCostPathCalculatorFactory provideLeastCostPathCalculatorFactory(Scenario scenario) {
        Config config = scenario.getConfig();
        if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.Dijkstra)) {
            return new DijkstraFactory();
        } else if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.AStarLandmarks)) {
            return new AStarLandmarksFactory(
                    scenario.getNetwork(), new FreespeedTravelTimeAndDisutility(config.planCalcScore()), config.global().getNumberOfThreads());
        } else if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.FastDijkstra)) {
            return new FastDijkstraFactory();
        } else if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks)) {
            return new FastAStarLandmarksFactory(
                    scenario.getNetwork(), new FreespeedTravelTimeAndDisutility(config.planCalcScore()));
        } else {
            throw new IllegalStateException("Enumeration Type RoutingAlgorithmType was extended without adaptation of Controler!");
        }
    }



    @Provides
    List<SnapshotWriterFactory> provideSnapshotWriterFactories(Config config) {
        SnapshotWriterRegistrar snapshotWriterRegistrar = new SnapshotWriterRegistrar();
        SnapshotWriterFactoryRegister snapshotWriterRegister = snapshotWriterRegistrar.getFactoryRegister();
        List<SnapshotWriterFactory> result = new ArrayList<SnapshotWriterFactory>();
        for (String snapshotFormat : config.controler().getSnapshotFormat()) {
            result.add(snapshotWriterRegister.getInstance(snapshotFormat));
        }
        return result;
    }

    @Provides
    TravelTime provideTravelTime(TravelTimeCalculator travelTimeCalculator) {
        return travelTimeCalculator.getLinkTravelTimes();
    }

    @Provides
    LinkToLinkTravelTime provideLinkToLinkTravelTime(TravelTimeCalculator travelTimeCalculator) {
        return travelTimeCalculator.getLinkToLinkTravelTimes();
    }

    @Provides
    TravelDisutility provideTravelDisutility(Config config, TravelDisutilityFactory travelDisutilityFactory, TravelTime travelTime) {
        return travelDisutilityFactory.createTravelDisutility(travelTime, config.planCalcScore());
    }

}
