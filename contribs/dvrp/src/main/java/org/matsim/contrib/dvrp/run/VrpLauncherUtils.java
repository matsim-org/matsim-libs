/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.router.TravelTimeCalculators;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.*;
import org.matsim.core.network.*;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;


public class VrpLauncherUtils
{
    //to avoid congestion; only the free-flow speed should decide on the movement of vehicles
    public static final double VARIANT_NETWORK_FLOW_CAP_FACTOR = 100;


    public static Scenario initScenario(String netFile, String plansFile)
    {
        return initScenario(netFile, plansFile, null, -1, -1);
    }


    public static Scenario initScenario(String netFile, String plansFile, String changeEventsFile, int interval, int intervalCount)
    {
        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        NetworkImpl network = (NetworkImpl)scenario.getNetwork();

        if (changeEventsFile != null) {
            scenario.getConfig().network().setTimeVariantNetwork(true);
            scenario.getConfig().qsim().setFlowCapFactor(VARIANT_NETWORK_FLOW_CAP_FACTOR);
            network.getFactory().setLinkFactory(new FixedIntervalTimeVariantLinkFactory(interval, intervalCount));
        }

        new MatsimNetworkReader(scenario.getNetwork()).readFile(netFile);

        if (changeEventsFile != null) {
            NetworkChangeEventsParser parser = new NetworkChangeEventsParser(network);
            parser.parse(changeEventsFile);
            network.setNetworkChangeEvents(parser.getEvents());
        }

        new MatsimPopulationReader(scenario).readFile(plansFile);
        return scenario;
    }


    public static TravelTimeCalculator initTravelTimeCalculatorFromEvents(Scenario scenario,
            String eventsFile, int timeInterval)
    {
        TravelTimeCalculatorConfigGroup ttCalcConfigGroup = scenario.getConfig()
                .travelTimeCalculator();
        ttCalcConfigGroup.setTraveltimeBinSize(timeInterval);

        TravelTimeCalculator ttCalculator = TravelTimeCalculator.create(scenario.getNetwork(),
                ttCalcConfigGroup);

        return TravelTimeCalculators.initTravelTimeCalculatorFromEvents(eventsFile, ttCalculator);
    }


    public static VrpData initVrpData(MatsimVrpContext context, String vehiclesFile)
    {
        VrpData vrpData = new VrpDataImpl();
        new VehicleReader(context.getScenario(), vrpData).parse(vehiclesFile);
        return vrpData;
    }


    public static PassengerEngine initPassengerEngine(String mode,
            PassengerRequestCreator requestCreator, VrpOptimizer optimizer,
            MatsimVrpContext context, QSim qSim)
    {
        PassengerEngine passengerEngine = new PassengerEngine(mode, qSim.getEventsManager(),
                requestCreator, optimizer, context);
        qSim.addMobsimEngine(passengerEngine);
        qSim.addDepartureHandler(passengerEngine);
        return passengerEngine;
    }


    public static void initAgentSources(QSim qSim, MatsimVrpContext context, VrpOptimizer optimizer,
            DynActionCreator actionCreator)
    {
        qSim.addAgentSource(new VrpAgentSource(actionCreator, context, optimizer, qSim));
        qSim.addAgentSource(new PopulationAgentSource(context.getScenario().getPopulation(),
                new DefaultAgentFactory(qSim), qSim));
    }
}
