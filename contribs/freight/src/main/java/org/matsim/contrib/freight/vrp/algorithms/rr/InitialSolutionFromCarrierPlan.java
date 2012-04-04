package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRTourAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRTourAgentFactory;
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
		List<RRTourAgent> agents = new ArrayList<RRTourAgent>();
		
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
