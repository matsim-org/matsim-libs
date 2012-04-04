package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRTourAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRTourAgentFactory;
import org.matsim.contrib.freight.vrp.basics.TourPlan;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VrpTourBuilder;

public class RRUtils {
	
	public static RRSolution createSolution(VehicleRoutingProblem vrp, TourPlan tourPlan, RRTourAgentFactory tourAgentFactory){
		List<RRTourAgent> agents = new ArrayList<RRTourAgent>();
		LinkedList<Vehicle> vehicles = new LinkedList<Vehicle>(vrp.getVehicles());
		for(VehicleRoute r : tourPlan.getVehicleRoutes()){
			agents.add(createTourAgent(r, tourAgentFactory));
			vehicles.remove(r.getVehicle());
		}
		for(Vehicle v : vehicles){
			agents.add(createEmptyTourAgent(v,tourAgentFactory));
		}
		
		RRSolution rrSolution = new RRSolution(agents);
		rrSolution.setScore((-1)*tourPlan.getScore());
		return rrSolution;
	}
	
	private static RRTourAgent createEmptyTourAgent(Vehicle v, RRTourAgentFactory tourAgentFactory) {
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		tourBuilder.scheduleStart(v.getLocationId(), v.getEarliestDeparture(), Double.MAX_VALUE);
		tourBuilder.scheduleEnd(v.getLocationId(), 0.0, v.getLatestArrival());
		return tourAgentFactory.createTourAgent(tourBuilder.build(),v);
	}

	public static RRTourAgent createTourAgent(VehicleRoute vehicleRoute, RRTourAgentFactory tourAgentFactory){
		return tourAgentFactory.createTourAgent(vehicleRoute.getTour(), vehicleRoute.getVehicle());
	}

	public static TourPlan createTourPlan(RRSolution rrSolution){
		List<VehicleRoute> routes = new ArrayList<VehicleRoute>();
		for(RRTourAgent a : rrSolution.getTourAgents()){
			routes.add(new VehicleRoute(a.getTour(),a.getVehicle()));
		}
		return new TourPlan(routes);
	}
}
