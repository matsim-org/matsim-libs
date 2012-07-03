package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

import org.matsim.contrib.freight.vrp.basics.VRPSchema;
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
		if(VRPSchema.SINGLEDEPOT_DISTRIBUTION_TIMEWINDOWS.equals(agentType)){
			return new SingleDepotDistribTWSPFactory(tourCost,vehicleRoutingCost);
		}
		else if(VRPSchema.SINGLEDEPOT_DISTRIBUTION.equals(agentType)){
			return new SingleDepotDistribSPFactory(tourCost,vehicleRoutingCost);
		}
		else{
			throw new IllegalStateException("does not support agentType " + agentType);
		}
	}

}
