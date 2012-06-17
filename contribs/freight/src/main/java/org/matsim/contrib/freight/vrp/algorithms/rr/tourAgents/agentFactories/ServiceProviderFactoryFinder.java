package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.agentFactories;

import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.ServiceProviderFactory;
import org.matsim.contrib.freight.vrp.basics.VRPSchema;

public class ServiceProviderFactoryFinder {
	
//	private static String SINGLEDEPOT_DISTRIBUTION_TIMEWINDOWS = "SingleDepotDistributionT";

	public ServiceProviderFactory getFactory(String agentType){
		if(VRPSchema.SINGLEDEPOT_DISTRIBUTION_TIMEWINDOWS.equals(agentType)){
			return new SingleDepotDistribTWSPFactory();
		}
		else if(VRPSchema.SINGLEDEPOT_DISTRIBUTION.equals(agentType)){
			return new SingleDepotDistribSPFactory();
		}
		else{
			throw new IllegalStateException("does not support agentType " + agentType);
		}
	}

}
