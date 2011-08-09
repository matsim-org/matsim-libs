package vrp.algorithms.ruinAndRecreate.basics;

import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.api.TourAgentFactory;
import vrp.api.Customer;
import vrp.api.VRP;
import vrp.basics.Tour;
import vrp.basics.Vehicle;

public class NewRRTourAgentFactory implements TourAgentFactory{

	private VRP vrp;
	
	public NewRRTourAgentFactory(VRP vrp) {
		super();
		this.vrp = vrp;
	}

	@Override
	public Tour createRoundTour(Customer n) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TourAgent createTourAgent(Tour tour, Vehicle vehicle) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tour createRoundTour(Customer from, Customer to) {
		// TODO Auto-generated method stub
		return null;
	}

	

}
