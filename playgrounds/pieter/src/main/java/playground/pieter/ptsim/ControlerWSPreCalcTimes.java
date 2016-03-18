/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerWW.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,  *
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

package playground.pieter.ptsim;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimePreCalcSerializable;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;
import org.matsim.contrib.travelsummary.events2traveldiaries.EventsToTravelDiaries;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import com.google.inject.Provider;

import playground.pieter.singapore.utils.events.RidershipTracking;
import playground.sergioo.eventAnalysisTools2013.excessWaitingTime.ExcessWaitingTimeCalculator;
import playground.sergioo.ptsim2013.TransitSheduleToNetwork;
import playground.singapore.ptsim.pt.BoardAlightVehicleTransitStopHandlerFactory;
import playground.singapore.ptsim.qnetsimengine.PTLinkSpeedCalculatorWithPreCalcTimes;


/**
 * A run Controler for a randomized transit router, that works with pre-calculated
 * travel times on the links
 *
 * @author sergioo, pieterfourie
 */

public class ControlerWSPreCalcTimes {


    public static void main(String[] args) throws IOException, ClassNotFoundException {

        final Config config = ConfigUtils.createConfig();
        ConfigUtils.loadConfig(config, args[0]);

        final Controler controler = new Controler(ScenarioUtils.loadScenario(config));
        final StopStopTime preloadedStopStopTimes = new StopStopTimePreCalcSerializable(args[1], controler.getScenario(), true);

//        final TravelTimeAndDistanceBasedTravelDisutilityFactory disutilityFactory =
//                new TravelTimeAndDistanceBasedTravelDisutilityFactory();
//        controler.addOverridingModule(new AbstractModule() {
//            @Override
//            public void install() {
//                bindCarTravelDisutilityFactory().toInstance(disutilityFactory);
//            }
//        });
//        disutilityFactory.setSigma(0.3);
//        controler.addOverridingModule(new RandomizedTransitRouterModule());

        final WaitTimeStuckCalculator waitTimeStuckCalculator = new WaitTimeStuckCalculator(controler.getScenario().getPopulation(),
                controler.getScenario().getTransitSchedule(),
                controler.getScenario().getConfig());

        final StopStopTimeCalculator stopStopTimeCalculatorSerializable = new StopStopTimeCalculator(
                controler.getScenario().getTransitSchedule(),
                controler.getScenario().getConfig());


        final MyAfterMobsimAnalyses analyses = new MyAfterMobsimAnalyses(controler);

        controler.addControlerListener(analyses);


        controler.getEvents().addHandler(
                waitTimeStuckCalculator
        );

        controler.getEvents().addHandler(
                stopStopTimeCalculatorSerializable
        );

        //need to make MRT slower, so identify the links with this mode with a hotfix
        for (Link l : controler.getScenario().getNetwork().getLinks().values()) {
            LinkImpl l1 = (LinkImpl) l;
            String[] parts = l.getId().toString().split(TransitSheduleToNetwork.SEPARATOR);
            if (parts[0].matches("[A-Z]+[0-9]*[_a-z]*")) {
                l1.setType("rail");
            } else {
                l1.setType("road");
            }
        }
        final PTLinkSpeedCalculatorWithPreCalcTimes linkSpeedCalculatorWithPreCalcTimes = new PTLinkSpeedCalculatorWithPreCalcTimes(preloadedStopStopTimes, true);
        controler.addControlerListener(linkSpeedCalculatorWithPreCalcTimes);
        //
        controler.addOverridingModule(new AbstractModule() {

            @Override

            public void install() {
                bind(TransitRouter.class).toProvider(new TransitRouterEventsWSFactory(controler.getScenario(),
                        waitTimeStuckCalculator.getWaitTimes(), stopStopTimeCalculatorSerializable.getStopStopTimes()));

                bindMobsim().toProvider(new Provider<Mobsim>() {

                    @Override

                    public Mobsim get() {
                        QSimConfigGroup conf = config.qsim();
                        if (conf == null) {
                            throw new NullPointerException("There is no configuration set for the QSim. " +
                                    "Please add the module 'qsim' to your config file.");
                        }

                        QSim qSim = new QSim(controler.getScenario(), controler.getEvents());

                        ActivityEngine activityEngine = new ActivityEngine(controler.getEvents(), qSim.getAgentCounter());

                        qSim.addMobsimEngine(activityEngine);
                        qSim.addActivityHandler(activityEngine);
                        //
                        ConfigurableQNetworkFactory netsimEngineFactory = new ConfigurableQNetworkFactory(controler.getEvents(), controler.getScenario() ) ;
                        netsimEngineFactory.setLinkSpeedCalculator(linkSpeedCalculatorWithPreCalcTimes);
				//
                        QNetsimEngine netsimEngine = new QNetsimEngine(qSim, netsimEngineFactory );
                        qSim.addMobsimEngine(netsimEngine);
                        qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
                        //
                        TeleportationEngine teleportationEngine = new TeleportationEngine(controler.getScenario(), controler.getEvents());
                        qSim.addMobsimEngine(teleportationEngine);
                        AgentFactory agentFactory;

                        if (config.transit().isUseTransit()) {
                            agentFactory = new TransitAgentFactory(qSim);
                            TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
                            transitEngine.setTransitStopHandlerFactory(new BoardAlightVehicleTransitStopHandlerFactory());
                            qSim.addDepartureHandler(transitEngine);
                            qSim.addAgentSource(transitEngine);
                            qSim.addMobsimEngine(transitEngine);
                        } else {
                            agentFactory = new DefaultAgentFactory(qSim);
                        }

                        if (config.network().isTimeVariantNetwork()) {
                            qSim.addMobsimEngine(new NetworkChangeEventsEngine());
                        }

                        PopulationAgentSource agentSource = new PopulationAgentSource(controler.getScenario().getPopulation(), agentFactory, qSim);
                        qSim.addAgentSource(agentSource);

                        return qSim;

                    }

                });

            }

        });

        controler.run();

    }
}

class MyAfterMobsimAnalyses implements AfterMobsimListener, Runnable {

    private MatsimServices controler;
    private String eventsFileName;
    private int currentIteration;
    private ExcessWaitingTimeCalculator eWTCalculator;

    public MyAfterMobsimAnalyses(MatsimServices controler) {
        this.controler = controler;
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        if (event.getIteration() % controler.getConfig().controler().getWriteEventsInterval() == 0) {
            eventsFileName = controler.getControlerIO().getIterationFilename(event.getIteration(), "events") + ".xml.gz";
            currentIteration = event.getIteration();
            new Thread(this).start();
        }
    }

    @Override
    public void run() {
        EventsManager events = EventsUtils.createEventsManager();
        eWTCalculator = new ExcessWaitingTimeCalculator();
        RidershipTracking ridershipTracking = new RidershipTracking(controler.getScenario(), controler.getControlerIO().getTempPath(), "_" + currentIteration);
        EventsToTravelDiaries eventsToTravelDiaries =
                new EventsToTravelDiaries(controler.getScenario());

        events.addHandler(eWTCalculator);
        events.addHandler(ridershipTracking.getRidershipHandler());
        events.addHandler(eventsToTravelDiaries);

        new MatsimEventsReader(events).readFile(eventsFileName);

        try {
            eventsToTravelDiaries.writeSimulationResultsToTabSeparated(controler.getControlerIO().getTempPath(), "_" + currentIteration);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ridershipTracking.finish();
        writeExcessWaitingTimes();
    }

    public void writeExcessWaitingTimes() {
        String fileName = controler.getControlerIO().getTempPath() + "/EWT_" + currentIteration +
                ".txt.gz";
        BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
        try {
            writer.write("iter\tline\troute\tstop_id\tEWT_headway\tEWT_numPeople\tEWT_byPerson");
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Logger logger = Logger.getLogger(this.getClass());
        logger.info("Writing excess waiting time calculations...");
        for (TransitLine line : controler.getScenario().getTransitSchedule().getTransitLines().values()) {
            for (TransitRoute route : line.getRoutes().values()) {
                for (TransitRouteStop stop : route.getStops()) {
                    try {
                        try {

                            writer.write(
                                    currentIteration +
                                            "\t" + line.getId().toString() +
                                            "\t" + route.getId().toString() +
                                            "\t" + stop.getStopFacility().getId().toString() +
                                            "\t" + eWTCalculator.getExcessWaitTime(line.getId(), route, stop.getStopFacility().getId(), ExcessWaitingTimeCalculator.Mode.TIME_WEIGHT) +
                                            "\t" + eWTCalculator.getExcessWaitTime(line.getId(), route, stop.getStopFacility().getId(), ExcessWaitingTimeCalculator.Mode.NUM_PEOPLE_WEIGHT) +
                                            "\t" + eWTCalculator.getExcessWaitTime(line.getId(), route, stop.getStopFacility().getId(), ExcessWaitingTimeCalculator.Mode.FULL_SAMPLE));
                            writer.newLine();
                        } catch (NoSuchElementException | NullPointerException ne) {
                            writer.newLine();

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

