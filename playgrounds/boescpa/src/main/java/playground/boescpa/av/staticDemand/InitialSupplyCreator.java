/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.av.staticDemand;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.boescpa.analysis.trips.tripReader.Trip;

import java.util.*;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class InitialSupplyCreator {
    private static Logger log = Logger.getLogger(InitialSupplyCreator.class);

    private static Random random = MatsimRandom.getRandom();

    public static List<AutonomousVehicle> createInitialAVSupply(double shareOfOriginalFleetReplacedByAV, StaticDemand staticDemand) {
        if (staticDemand == null) {
            throw new IllegalArgumentException("demand was null");
        }

        List<AutonomousVehicle> autonomousVehicles = new ArrayList<>();
        List<Trip> demand = staticDemand.getFilteredDemand();
        List<Integer> agents = filterDemandForAgents(demand);

        log.info("Create initial supply of AVs...");
        long shareToServe = Math.round(shareOfOriginalFleetReplacedByAV*agents.size());
        for (int i = 0; i < shareToServe; i++) {
            int agentToServe =  random.nextInt(agents.size());
            Trip referenceTrip = demand.get(agents.get(agentToServe));
            Coord initialPosition = CoordUtils.createCoord(referenceTrip.startXCoord, referenceTrip.startYCoord);
            autonomousVehicles.add(new AutonomousVehicle(initialPosition));
            agents.remove(agentToServe);
        }
        log.info("Create initial supply of AVs... done.");
        log.info(autonomousVehicles.size() + " AVs supplied.");

        return autonomousVehicles;
    }

    private static List<Integer> filterDemandForAgents(List<Trip> demand) {
        log.info("Filter demand for agents...");
        List<Integer> firstAgentPositions = new ArrayList<>();
        Set<Id> agentsSet = new HashSet<>();
        for (int i = 0; i < demand.size(); i++) {
            Id runningAgent = demand.get(i).agentId;
            if (!agentsSet.contains(runningAgent)) {
                agentsSet.add(runningAgent);
                firstAgentPositions.add(i);
            }
        }
        log.info("Filter demand for agents... done.");
        log.info(firstAgentPositions.size() + " driving agents in sample found.");
        return firstAgentPositions;
    }

}
