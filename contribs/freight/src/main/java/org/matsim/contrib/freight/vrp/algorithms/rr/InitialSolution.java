/* *********************************************************************** *
 * project: org.matsim.*
 * IniSolution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.contrib.freight.vrp.algorithms.rr.recreation.BestInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.PickupAndDeliveryTourFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRTourAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourCostAndTWProcessor;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourStatusProcessor;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.SingleDepotInitialSolutionFactory;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VrpTourBuilder;

public class InitialSolution implements SingleDepotInitialSolutionFactory {

	@Override
	public RRSolution createInitialSolution(VehicleRoutingProblem vrp) {
		RRSolution solution = createEmptySolution(vrp);
		BestInsertion bestInsertion = new BestInsertion();
		bestInsertion.run(solution, getUnassignedJobs(vrp));
		return solution;
	}

	private List<Job> getUnassignedJobs(VehicleRoutingProblem vrp) {
		List<Job> jobs = new ArrayList<Job>(vrp.getJobs().values());
		return jobs;
	}

	private RRSolution createEmptySolution(VehicleRoutingProblem vrp) {
		Collection<TourAgent> emptyTours = new ArrayList<TourAgent>();
		TourStatusProcessor statusProcessor = new TourCostAndTWProcessor(vrp.getCosts());
		TourAgentFactory tourAgentFactory = new RRTourAgentFactory(statusProcessor, new PickupAndDeliveryTourFactory(vrp.getCosts(), 
				vrp.getConstraints(), statusProcessor));
		for (Vehicle vehicle : vrp.getVehicles()) { 
			TourAgent tourAgent = createTourAgent(vehicle, vehicle.getLocationId(), tourAgentFactory);
			emptyTours.add(tourAgent);
		}
		return new RRSolution(emptyTours);
	}


	private TourAgent createTourAgent(Vehicle vehicle, String vehicleLocationId, TourAgentFactory tourAgentFactory) {
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		tourBuilder.scheduleStart(vehicleLocationId, vehicle.getEarliestDeparture(), Double.MAX_VALUE);
		tourBuilder.scheduleEnd(vehicleLocationId, 0.0, vehicle.getLatestArrival());
		Tour tour = tourBuilder.build();
		return tourAgentFactory.createTourAgent(tour, vehicle);
	}
	
}
