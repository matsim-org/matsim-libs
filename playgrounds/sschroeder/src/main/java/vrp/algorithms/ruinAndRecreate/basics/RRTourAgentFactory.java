package vrp.algorithms.ruinAndRecreate.basics;

import vrp.algorithms.ruinAndRecreate.api.TourActivityStatusUpdater;
import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.api.TourAgentFactory;
import vrp.api.VRP;
import vrp.basics.Tour;
import vrp.basics.Vehicle;


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

	public TourAgent createTourAgent(Tour tour, Vehicle vehicle) {
		TourActivityStatusUpdater updater = new TourActivityStatusUpdaterImpl(vrp.getCosts());
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
