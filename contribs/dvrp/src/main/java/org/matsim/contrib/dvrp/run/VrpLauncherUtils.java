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

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.data.file.DepotReader;
import org.matsim.contrib.dvrp.data.network.*;
import org.matsim.contrib.dvrp.data.network.router.*;
import org.matsim.contrib.dvrp.data.network.shortestpath.MatsimArcFactories;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.agents.*;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.*;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.network.ArcFactory;
import pl.poznan.put.vrp.dynamic.util.TimeDiscretizer;


public class VrpLauncherUtils
{
    public enum TravelTimeSource
    {
        FREE_FLOW_SPEED("FF", 24 * 60 * 60), // no eventsFileName
        EVENTS_24_H("24H", 24 * 60 * 60), // based on eventsFileName, with 24-hour time interval
        EVENTS_15_MIN("15M", 15 * 60); // based on eventsFileName, with 15-minute time interval

        public final String shortcut;
        public final int travelTimeBinSize;
        public final int numSlots;


        private TravelTimeSource(String shortcut, int travelTimeBinSize)
        {
            this.shortcut = shortcut;
            this.travelTimeBinSize = travelTimeBinSize;
            this.numSlots = 24 * 60 * 60 / travelTimeBinSize;// to cover 24 hours
        }
    }


    public enum TravelDisutilitySource
    {
        TIME, // travel time
        DISTANCE; // travel distance
    }


    public static Scenario initScenario(String netFileName, String plansFileName)
    {
        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(netFileName);
        new MatsimPopulationReader(scenario).readFile(plansFileName);
        return scenario;
    }


    public static void convertLegModes(List<String> passengerIds, String mode, Scenario scenario)
    {
        Map<Id, ? extends Person> persons = scenario.getPopulation().getPersons();

        for (String id : passengerIds) {
            Person person = persons.get(scenario.createId(id));

            for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
                if (pe instanceof Leg) {
                    ((Leg)pe).setMode(mode);
                }
            }
        }
    }


    public static void removeNonPassengers(String mode, Scenario scenario)
    {
        Set<Id> nonPassengerIds = new HashSet<Id>();
        Map<Id, ? extends Person> persons = scenario.getPopulation().getPersons();

        for (Entry<Id, ? extends Person> e : persons.entrySet()) {
            Person person = e.getValue();

            boolean isPassenger = false;
            for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
                if (pe instanceof Leg) {
                    if ( ((Leg)pe).getMode().equals(mode)) {
                        isPassenger = true;
                        break;
                    }
                }
            }

            if (!isPassenger) {
                nonPassengerIds.add(e.getKey());
            }
        }

        for (Id id : nonPassengerIds) {
            persons.remove(id);
        }
    }


    public static TravelTime initTravelTime(Scenario scenario,
            TravelTimeCalculator travelTimeCalculator, TravelTimeSource ttimeSource,
            String eventsFileName)
    {
        if (travelTimeCalculator == null) {
            switch (ttimeSource) {
                case FREE_FLOW_SPEED:
                    return new FreeSpeedTravelTime();

                case EVENTS_15_MIN:
                case EVENTS_24_H:
                    scenario.getConfig().travelTimeCalculator()
                            .setTraveltimeBinSize(ttimeSource.travelTimeBinSize);
                    return TravelTimeCalculators.createTravelTimeFromEvents(eventsFileName,
                            scenario);

                default:
                    throw new IllegalArgumentException();
            }
        }
        else {
            return travelTimeCalculator.getLinkTravelTimes();
        }
    }


    public static TravelDisutility initTravelDisutility(TravelDisutilitySource tdisSource,
            TravelTime travelTime)
    {
        switch (tdisSource) {
            case DISTANCE:
                return new DistanceAsTravelDisutility();

            case TIME:
                return new TimeAsTravelDisutility(travelTime);

            default:
                throw new IllegalArgumentException();
        }
    }


    public static MatsimVrpGraph initMatsimVrpGraph(Scenario scenario,
            TravelTimeSource ttimeSource, TravelTime travelTime, TravelDisutility travelDisutility)
    {
        Network network = scenario.getNetwork();
        TimeDiscretizer timeDiscretizer = new TimeDiscretizer(ttimeSource.travelTimeBinSize,
                ttimeSource.numSlots);
        ArcFactory arcFactory = MatsimArcFactories.createArcFactory(network, travelTime,
                travelDisutility, timeDiscretizer, false);

        MatsimVrpGraph graph;
        try {
            graph = MatsimVrpGraphCreator.create(network, arcFactory, false);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        return graph;
    }


    public static VrpData initVrpData(Scenario scenario, MatsimVrpGraph graph, String depotsFileName)
    {
        VrpData vrpData = new VrpData();
        vrpData.setVrpGraph(graph);
        new DepotReader(scenario, vrpData).readFile(depotsFileName);
        return vrpData;
    }


    public static QSim initQSim(Scenario scenario)
    {
        EventsManager events = EventsUtils.createEventsManager();
        QSim qSim = new QSim(scenario, events);

        ActivityEngine activityEngine = new ActivityEngine();
        qSim.addMobsimEngine(activityEngine);
        qSim.addActivityHandler(activityEngine);

        QNetsimEngine netsimEngine = new DefaultQSimEngineFactory().createQSimEngine(qSim);
        qSim.addMobsimEngine(netsimEngine);
        qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

        qSim.addMobsimEngine(new TeleportationEngine());

        return qSim;
    }


    public static VrpSimEngine initVrpSimEngine(QSim qSim, MatsimVrpData data,
            VrpOptimizer optimizer)
    {
        VrpSimEngine vrpSimEngine = new VrpSimEngine(qSim, data, optimizer);
        qSim.addMobsimEngine(vrpSimEngine);
        return vrpSimEngine;
    }


    public static void initAgentSources(QSim qSim, MatsimVrpData data, VrpSimEngine vrpSimEngine,
            DynActionCreator actionCreator, boolean onlineVehicleTracker)
    {
        qSim.addAgentSource(new VrpAgentSource(actionCreator, data, vrpSimEngine,
                onlineVehicleTracker));
        qSim.addAgentSource(new PopulationAgentSource(data.getScenario().getPopulation(),
                new DefaultAgentFactory(qSim), qSim));
    }


    public static void initDepartureHandler(QSim qSim, MatsimVrpData data,
            VrpSimEngine vrpSimEngine, PassengerRequestCreator requestCreator, String mode)
    {
        qSim.addDepartureHandler(new PassengerDepartureHandler(mode, requestCreator, vrpSimEngine,
                data));
    }


    public static void writeHistograms(LegHistogram legHistogram, String histogramOutDirName)
    {
        new File(histogramOutDirName).mkdir();
        legHistogram.write(histogramOutDirName + "legHistogram_all.txt");
        legHistogram.writeGraphic(histogramOutDirName + "legHistogram_all.png");
        for (String legMode : legHistogram.getLegModes()) {
            legHistogram.writeGraphic(histogramOutDirName + "legHistogram_" + legMode + ".png",
                    legMode);
        }
    }
}
