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

	public Tour createRoundTour(Customer depot, Customer n) {
		return VrpUtils.createRoundTour(depot, n);
	}

	public TourAgent createTourAgent(VRP vrp, Tour tour, Vehicle vehicle) {
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

	public Tour createRoundTour(Customer depot, Customer i, Customer j) {
		return VrpUtils.createRoundTour(depot, i, j);
	}

}
