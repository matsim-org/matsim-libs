package vrp.algorithms.ruinAndRecreate.basics;

import vrp.algorithms.ruinAndRecreate.api.TourActivityStatusUpdater;
import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.api.TourAgentFactory;
import vrp.api.Customer;
import vrp.api.VRP;
import vrp.basics.Tour;
import vrp.basics.Vehicle;
import vrp.basics.VrpUtils;


/**
 * 
 * @author stefan schroeder
 *
 */

public class RRTourAgentFactory implements TourAgentFactory{

	private VRP vrp;
	
	public RRTourAgentFactory(VRP vrp) {
		super();
		this.vrp = vrp;
	}

	public Tour createRoundTour(Customer n) {
		Customer depot = getClosestDepot(n);
		return VrpUtils.createRoundTour(depot, n);
	}

	private Customer getClosestDepot(Customer n) {
		Customer bestDepot = null;
		Double minCost2Depot = Double.MAX_VALUE; 
		for(Customer depot : vrp.getDepots().values()){
			if(bestDepot == null){
				bestDepot = depot;
			}
			else{
				double costs = vrp.getCosts().getCost(depot.getLocation(), n.getLocation());
				if(costs < minCost2Depot){
					minCost2Depot = costs;
					bestDepot = depot;
				}
			}
		}
		return bestDepot;
	}

	public TourAgent createTourAgent(Tour tour, Vehicle vehicle) {
		TourActivityStatusUpdater updater = new TourActivityStatusUpdaterImpl(vrp.getCosts());
		BestTourBuilder tourBuilder = new BestTourBuilder();
		tourBuilder.setConstraints(vrp.getConstraints());
		tourBuilder.setCosts(vrp.getCosts());
		tourBuilder.setTourActivityStatusUpdater(updater);
		RRTourAgent tourAgent = new RRTourAgent(vrp.getCosts(), tour, vehicle, updater);
		tourAgent.setConstraint(vrp.getConstraints());
		tourAgent.setTourBuilder(tourBuilder);
		return tourAgent;
	}

	public Tour createRoundTour(Customer i, Customer j) {
		Tour tour = null;
		Customer depot = null;
		if(vrp.getDepots().containsKey(i.getId())){
			tour = createRoundTour(j);
		}
		else if(vrp.getDepots().containsKey(j.getId())){
			tour = createRoundTour(i);
		}
		else{
			depot = getClosestDepot(i);
			tour = VrpUtils.createRoundTour(depot, i, j);
		}
		return tour;
	}
}
