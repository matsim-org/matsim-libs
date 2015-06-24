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

package playground.singapore.ptsim;

import com.google.inject.Provider;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculatorSerializable;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimePreCalcSerializable;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculatorSerializable;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
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
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import playground.singapore.ptsim.pt.BoardAlightVehicleTransitStopHandlerFactory;
import playground.singapore.ptsim.qnetsimengine.PTLinkSpeedCalculatorWithPreCalcTimes;

import java.io.IOException;


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
        final StopStopTime preloadedStopStopTimes = new StopStopTimePreCalcSerializable(args[1], controler.getScenario(),true);

//        final TravelTimeAndDistanceBasedTravelDisutilityFactory disutilityFactory =
//                new TravelTimeAndDistanceBasedTravelDisutilityFactory();
//        controler.addOverridingModule(new AbstractModule() {
//            @Override
//            public void install() {
//                bindTravelDisutilityFactory().toInstance(disutilityFactory);
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

        controler.getEvents().addHandler(
                waitTimeStuckCalculator
        );
        controler.getEvents().addHandler(
                stopStopTimeCalculatorSerializable
        );
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
                        QNetsimEngine netsimEngine = new QNetsimEngine(qSim);
                        netsimEngine.setLinkSpeedCalculator(linkSpeedCalculatorWithPreCalcTimes);
                        qSim.addMobsimEngine(netsimEngine);
                        qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
                        //
                        TeleportationEngine teleportationEngine = new TeleportationEngine(controler.getScenario(), controler.getEvents());
                        qSim.addMobsimEngine(teleportationEngine);
                        AgentFactory agentFactory;

                        if (config.scenario().isUseTransit()) {
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
