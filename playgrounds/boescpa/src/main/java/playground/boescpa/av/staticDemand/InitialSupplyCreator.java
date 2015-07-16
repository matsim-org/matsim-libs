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
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Counter;
import playground.boescpa.lib.tools.tripReader.Trip;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class InitialSupplyCreator {
	private static Logger log = Logger.getLogger(InitialSupplyCreator.class);

	private static Random random = MatsimRandom.getRandom();

	public static List<AutonomousVehicle> createInitialAVSupply(double share, StaticDemand staticDemand) {
		if (staticDemand == null) {
			throw new IllegalArgumentException("demand was null");
		}

		List<AutonomousVehicle> autonomousVehicles = new ArrayList<>();
		List<Trip> demand = staticDemand.getFilteredDemand();
		List<Integer> agents = filterDemandForAgents(demand);

		log.info("Create initial supply of AVs...");
		Counter counter = new Counter(" AV ");
		for (int i = 0; i < Math.round(share*agents.size()); i++) {
			Trip referenceTrip = demand.get(agents.get(random.nextInt(agents.size())));
			Coord initialPosition = new CoordImpl(referenceTrip.startXCoord, referenceTrip.startYCoord);
			autonomousVehicles.add(new AutonomousVehicle(initialPosition));
			counter.incCounter();
		}
		counter.printCounter();
		log.info("Create initial supply of AVs... done.");

		return autonomousVehicles;
	}

	private static List<Integer> filterDemandForAgents(List<Trip> demand) {
		log.info("Filter demand for agents...");
		List<Integer> firstAgentPositions = new ArrayList<>();
		Id currentAgent = null;
		for (int i = 0; i < demand.size(); i++) {
			Id runningAgent = demand.get(i).agentId;
			if (currentAgent == null || !runningAgent.toString().equals(currentAgent.toString())) {
				currentAgent = runningAgent;
				firstAgentPositions.add(i);
			}
		}
		log.info("Filter demand for agents... done.");
		log.info(firstAgentPositions.size() + " driving agents found.");
		return firstAgentPositions;
	}

}
