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
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRTourAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.ServiceProviderAgent;
import org.matsim.contrib.freight.vrp.basics.InitialSolutionFactory;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;

public class InitialSolutionFromCarrierPlan implements InitialSolutionFactory{

	private CarrierPlan plan;
	
	private RRTourAgentFactory tourAgentFactory;
	
	public InitialSolutionFromCarrierPlan(CarrierPlan plan, RRTourAgentFactory tourAgentFactory) {
		super();
		this.plan = plan;
		this.tourAgentFactory = tourAgentFactory;
	}

	@Override
	public RRSolution createInitialSolution(VehicleRoutingProblem vrp) {
		List<ServiceProviderAgent> agents = new ArrayList<ServiceProviderAgent>();
		
		for(ScheduledTour sTour : plan.getScheduledTours()){
			tourAgentFactory.createTourAgent(getTour(sTour), getVehicle(sTour.getVehicle().getVehicleId(),vrp));
		}
		return new RRSolution(agents);
	}

	private Tour getTour(ScheduledTour sTour) {
		// TODO Auto-generated method stub
		return null;
	}

	private Vehicle getVehicle(Id vehicleId, VehicleRoutingProblem vrp) {
		for(Vehicle v : vrp.getVehicles()){
			if(vehicleId.toString().equals(v.getId())){
				return v;
			}
		}
		return null;
	}

}
