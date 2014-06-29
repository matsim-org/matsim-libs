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

import java.io.File;
import java.util.*;

import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.util.time.TimeDiscretizer;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.*;
import org.matsim.core.network.*;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.*;


public class VrpLauncherUtils
{
    public static final int MAX_TIME = 36 * 60 * 60;


    public enum TravelTimeSource
    {
        FREE_FLOW_SPEED("FF", TimeDiscretizer.CYCLIC_24_HOURS), // no eventsFileName
        EVENTS_24_H("24H", TimeDiscretizer.CYCLIC_24_HOURS), // based on eventsFileName, averaged over a whole day
        EVENTS_15_MIN("15M", TimeDiscretizer.CYCLIC_15_MIN); // based on eventsFileName, 15-minute time interval

        public final String shortcut;
        public final TimeDiscretizer timeDiscretizer;


        private TravelTimeSource(String shortcut, TimeDiscretizer timeDiscretizer)
        {
            this.shortcut = shortcut;
            this.timeDiscretizer = timeDiscretizer;
        }
    }


    public enum TravelDisutilitySource
    {
        STRAIGHT_LINE, // however, Dijkstra's algo will use DISTANCE cost
        DISTANCE, // travel distance
        TIME; // travel time
    }


    //TODO: Make Scenario Loading Matsim Standard (e.g. use a config file)
    public static Scenario initScenario(String netFileName, String plansFileName)
    {
        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(netFileName);
        new MatsimPopulationReader(scenario).readFile(plansFileName);
        return scenario;
    }


    public static Scenario initTimeVariantScenario(String netFileName, String plansFileName,
            String changeEventsFilename)
    {
        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        scenario.getConfig().network().setTimeVariantNetwork(true);
        NetworkImpl network = (NetworkImpl)scenario.getNetwork();
        network.getFactory().setLinkFactory(new TimeVariantLinkFactory());
        new MatsimNetworkReader(scenario).readFile(netFileName);
        System.out.println("use TimeVariantLinks in NetworkFactory.");
        scenario.getConfig().network().setChangeEventInputFile(changeEventsFilename);
        System.out.println("loading network change events from "
                + scenario.getConfig().network().getChangeEventsInputFile());
        NetworkChangeEventsParser parser = new NetworkChangeEventsParser(network);
        parser.parse(scenario.getConfig().network().getChangeEventsInputFile());

        network.setNetworkChangeEvents(parser.getEvents());

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
        Map<Id, ? extends Person> persons = scenario.getPopulation().getPersons();
        Iterator<? extends Person> personIter = persons.values().iterator();

        while (personIter.hasNext()) {
            Plan selectedPlan = personIter.next().getSelectedPlan();

            if (!hasLegOfMode(selectedPlan, mode)) {
                personIter.remove();
            }
        }
    }


    private static boolean hasLegOfMode(Plan plan, String mode)
    {
        for (PlanElement pe : plan.getPlanElements()) {
            if (pe instanceof Leg) {
                if ( ((Leg)pe).getMode().equals(mode)) {
                    return true;
                }
            }
        }

        return false;
    }


    public static TravelTime initTravelTime(Scenario scenario, TravelTimeSource ttimeSource,
            String eventsFileName)
    {
        switch (ttimeSource) {
            case FREE_FLOW_SPEED:
                return new FreeSpeedTravelTime();

            case EVENTS_15_MIN:
            case EVENTS_24_H:
                scenario.getConfig().travelTimeCalculator()
                        .setTraveltimeBinSize(ttimeSource.timeDiscretizer.getTimeInterval());
                TravelTimeCalculator ttCalculator = TravelTimeCalculators
                        .createTravelTimeCalculator(scenario);
                return TravelTimeCalculators.createTravelTimeFromEvents(eventsFileName,
                        ttCalculator);

            default:
                throw new IllegalArgumentException();
        }
    }


    public static TravelDisutility initTravelDisutility(TravelDisutilitySource tdisSource,
            TravelTime travelTime)
    {
        switch (tdisSource) {
            case STRAIGHT_LINE:
            case DISTANCE:
                return new DistanceAsTravelDisutility();

            case TIME:
                return new TimeAsTravelDisutility(travelTime);

            default:
                throw new IllegalArgumentException();
        }
    }


    public static VrpData initVrpData(MatsimVrpContext context, String vehiclesFileName)
    {
        VrpData vrpData = new VrpDataImpl();
        new VehicleReader(context.getScenario(), vrpData).parse(vehiclesFileName);
        return vrpData;
    }


    public static PassengerEngine initPassengerEngine(String mode,
            PassengerRequestCreator requestCreator, VrpOptimizer optimizer,
            MatsimVrpContext context, QSim qSim)
    {
        PassengerEngine passengerEngine = new PassengerEngine(mode, requestCreator, optimizer,
                context);
        qSim.addMobsimEngine(passengerEngine);
        qSim.addDepartureHandler(passengerEngine);
        return passengerEngine;
    }


    public static void initAgentSources(QSim qSim, MatsimVrpContext context,
            VrpOptimizer optimizer, DynActionCreator actionCreator)
    {
        qSim.addAgentSource(new VrpAgentSource(actionCreator, context, optimizer, qSim));
        qSim.addAgentSource(new PopulationAgentSource(context.getScenario().getPopulation(),
                new DefaultAgentFactory(qSim), qSim));
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
