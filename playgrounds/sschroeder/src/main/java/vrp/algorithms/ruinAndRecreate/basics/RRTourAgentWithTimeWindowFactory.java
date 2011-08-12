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

public class RRTourAgentWithTimeWindowFactory implements TourAgentFactory{

	private VRP vrp;
	
	public RRTourAgentWithTimeWindowFactory(VRP vrp) {
		super();
		this.vrp = vrp;
	}

	public TourAgent createTourAgent(Tour tour, Vehicle vehicle) {
		TourActivityStatusUpdater updater = new TourActivityStatusUpdaterWithTWImpl(vrp.getCosts());
		BestTourBuilder tourBuilder = new BestTourBuilder();
		tourBuilder.setConstraints(vrp.getConstraints());
		tourBuilder.setCosts(vrp.getCosts());
		tourBuilder.setTourActivityStatusUpdater(updater);
		tourBuilder.setVehicle(vehicle);
		RRTourAgent tourAgent = new RRTourAgent(vrp.getCosts(), tour, vehicle, updater);
		tourAgent.setConstraint(vrp.getConstraints());
		tourAgent.setTourBuilder(tourBuilder);
		return tourAgent;
	}

}
