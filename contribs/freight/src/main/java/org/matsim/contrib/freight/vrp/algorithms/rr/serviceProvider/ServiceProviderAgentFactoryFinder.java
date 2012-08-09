package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemTypes;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;

public class ServiceProviderAgentFactoryFinder {
	
	private TourCost tourCost;
	
	private VehicleRoutingCosts vehicleRoutingCost;


	public ServiceProviderAgentFactoryFinder(TourCost tourCost,
			VehicleRoutingCosts vehicleRoutingCost) {
		super();
		this.tourCost = tourCost;
		this.vehicleRoutingCost = vehicleRoutingCost;
	}

	public ServiceProviderAgentFactory getFactory(String agentType){
		if(VehicleRoutingProblemTypes.CVRPTW.equals(agentType)){
			return new SingleDepotDistribTWSPFactory(tourCost,vehicleRoutingCost);
		}
		else if(VehicleRoutingProblemTypes.CVRP.equals(agentType)){
			return new SingleDepotDistribSPFactory(tourCost,vehicleRoutingCost);
		}
		else{
			throw new IllegalStateException("does not support agentType " + agentType);
		}
	}

}
