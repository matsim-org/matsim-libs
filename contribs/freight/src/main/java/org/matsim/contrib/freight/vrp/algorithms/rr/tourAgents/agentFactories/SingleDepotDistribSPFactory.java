package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.agentFactories;

import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.DistribJIFFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.JobDistribOfferMaker;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.LocalMCCalculatorFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRDriverAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.ServiceProviderAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.ServiceProviderFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourCostProcessor;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.DriverCostParams;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public class SingleDepotDistribSPFactory implements ServiceProviderFactory{
	

	@Override
	public ServiceProviderAgent createAgent(Tour tour, Vehicle vehicle, Costs costs) {
		RRDriverAgent a = new RRDriverAgent(vehicle, tour, new TourCostProcessor(costs), new DriverCostParams());
		a.setOfferMaker(new JobDistribOfferMaker(costs, null, new DistribJIFFactory(new LocalMCCalculatorFactory())));
		return a;
	}

}
