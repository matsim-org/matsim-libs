package org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators;

import org.matsim.contrib.freight.vrp.basics.VehicleFleetManager;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;

public class StandardRouteAgentFactory implements RouteAgentFactory{
	
	private JobInsertionCalculator jobInsertionCalculator;
	
	private TourStateCalculator tourStateCalculator;
	
	private VehicleFleetManager vehicleFleetManager;
	
	public void setVehicleFleetManager(VehicleFleetManager vehicleFleetManager) {
		this.vehicleFleetManager = vehicleFleetManager;
	}

	public StandardRouteAgentFactory(JobInsertionCalculator jobInsertionCalculator, TourStateCalculator tourStateCalculator) {
		super();
		this.jobInsertionCalculator = jobInsertionCalculator;
		this.tourStateCalculator = tourStateCalculator;
	}

	@Override
	public RouteAgent createAgent(VehicleRoute route) {
		RouteAgentImpl a = new RouteAgentImpl(route,jobInsertionCalculator,tourStateCalculator);
		if(vehicleFleetManager != null) a.setVehicleFleetManager(vehicleFleetManager);
		return a;
	}

}
