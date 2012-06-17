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
package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.ServiceProviderAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.ServiceProviderFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourAgent;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.TourPlan;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VrpTourBuilder;

public class RRUtils {
	
	public static RRSolution createSolution(VehicleRoutingProblem vrp, TourPlan tourPlan, ServiceProviderFactory tourAgentFactory){
		List<ServiceProviderAgent> agents = new ArrayList<ServiceProviderAgent>();
		LinkedList<Vehicle> vehicles = new LinkedList<Vehicle>(vrp.getVehicles());
		for(VehicleRoute r : tourPlan.getVehicleRoutes()){
			agents.add(createTourAgent(r, tourAgentFactory, vrp.getCosts()));
			vehicles.remove(r.getVehicle());
		}
		for(Vehicle v : vehicles){
			agents.add(createEmptyTourAgent(v,tourAgentFactory,vrp.getCosts()));
		}
		
		RRSolution rrSolution = new RRSolution(agents);
		rrSolution.setScore((-1)*tourPlan.getScore());
		return rrSolution;
	}
	
	private static ServiceProviderAgent createEmptyTourAgent(Vehicle v, ServiceProviderFactory tourAgentFactory, Costs costs) {
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		tourBuilder.scheduleStart(v.getLocationId(), v.getEarliestDeparture(), Double.MAX_VALUE);
		tourBuilder.scheduleEnd(v.getLocationId(), 0.0, v.getLatestArrival());
		return tourAgentFactory.createAgent(tourBuilder.build(),v, costs);
	}

	public static ServiceProviderAgent createTourAgent(VehicleRoute vehicleRoute, ServiceProviderFactory tourAgentFactory, Costs costs){
		return tourAgentFactory.createAgent(vehicleRoute.getTour(), vehicleRoute.getVehicle(), costs);
	}

	public static TourPlan createTourPlan(RRSolution rrSolution){
		List<VehicleRoute> routes = new ArrayList<VehicleRoute>();
		for(TourAgent a : rrSolution.getTourAgents()){
			routes.add(new VehicleRoute(a.getTour(),a.getVehicle()));
		}
		return new TourPlan(routes);
	}
}
