package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.agentFactories;

import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.DistribJIFFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.JobDistribOfferMaker;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.LocalMCCalculatorFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRDriverAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.ServiceProviderAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.ServiceProviderFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourCostAndTWProcessor;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public class SingleDepotDistribTWSPFactory implements ServiceProviderFactory{

	@Override
	public ServiceProviderAgent createAgent(Tour tour, Vehicle vehicle, Costs costs) {
		RRDriverAgent a = new RRDriverAgent(vehicle, tour, new TourCostAndTWProcessor(costs), costs.getCostParams());
		a.setOfferMaker(new JobDistribOfferMaker(costs, null, new DistribJIFFactory(new LocalMCCalculatorFactory())));
		return a;
	}

}
