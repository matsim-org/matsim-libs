package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemType;

public class ServiceProviderAgentFactoryFinder {
	
	private TourCost tourCost;
	
	private VehicleRoutingCosts vehicleRoutingCost;

	public ServiceProviderAgentFactoryFinder(TourCost tourCost,VehicleRoutingCosts vehicleRoutingCost) {
		super();
		this.tourCost = tourCost;
		this.vehicleRoutingCost = vehicleRoutingCost;
	}

	public ServiceProviderAgentFactory getFactory(VehicleRoutingProblemType problemType){
		if(VehicleRoutingProblemType.CVRPTW.equals(problemType)){
			return new SingleDepotDistribTWSPFactory(tourCost,vehicleRoutingCost);
		}
		else if(VehicleRoutingProblemType.CVRP.equals(problemType)){
			return new SingleDepotDistribSPFactory(tourCost,vehicleRoutingCost);
		}
		else{
			throw new IllegalStateException("does not support agentType " + problemType);
		}
	}

}
