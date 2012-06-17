/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
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
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.recreation.BestInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.DistribJIFFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.JobDistribOfferMaker;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.LocalMCCalculatorFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.ServiceProviderAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.ServiceProviderFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourCostAndTWProcessor;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourStatusProcessor;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.agentFactories.RRTourAgentFactory;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.InitialSolutionFactory;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VrpTourBuilder;

public class InitialSolution implements InitialSolutionFactory {

	private static Logger logger = Logger.getLogger(InitialSolution.class);
	
	@Override
	public RRSolution createInitialSolution(VehicleRoutingProblem vrp) {
		logger.info("create initial solution.");
		List<ServiceProviderAgent> serviceProviders = createEmptyServiceProviders(vrp);
		BestInsertion bestInsertion = new BestInsertion();
		bestInsertion.recreate(serviceProviders, getUnassignedJobs(vrp));
		return new RRSolution(serviceProviders);
	}

	private List<Job> getUnassignedJobs(VehicleRoutingProblem vrp) {
		List<Job> jobs = new ArrayList<Job>(vrp.getJobs().values());
		return jobs;
	}

	private List<ServiceProviderAgent> createEmptyServiceProviders(VehicleRoutingProblem vrp) {
		List<ServiceProviderAgent> emptyTours = new ArrayList<ServiceProviderAgent>();
		TourStatusProcessor statusProcessor = new TourCostAndTWProcessor(vrp.getCosts());
		RRTourAgentFactory tourAgentFactory = new RRTourAgentFactory(statusProcessor, vrp.getCosts().getCostParams(), 
				new JobDistribOfferMaker(vrp.getCosts(), vrp.getGlobalConstraints(), new DistribJIFFactory(new LocalMCCalculatorFactory())));
		for (Vehicle vehicle : vrp.getVehicles()) { 
			ServiceProviderAgent tourAgent = createTourAgent(vehicle, vehicle.getLocationId(), tourAgentFactory, vrp.getCosts());
			emptyTours.add(tourAgent);
		}
		return emptyTours;
	}


	private ServiceProviderAgent createTourAgent(Vehicle vehicle, String vehicleLocationId, ServiceProviderFactory tourAgentFactory, Costs costs) {
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		tourBuilder.scheduleStart(vehicleLocationId, vehicle.getEarliestDeparture(), Double.MAX_VALUE);
		tourBuilder.scheduleEnd(vehicleLocationId, 0.0, vehicle.getLatestArrival());
		Tour tour = tourBuilder.build();
		return tourAgentFactory.createAgent(tour, vehicle, costs);
	}
	
}
