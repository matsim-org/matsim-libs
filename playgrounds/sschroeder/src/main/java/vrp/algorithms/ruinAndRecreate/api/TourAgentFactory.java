package vrp.algorithms.ruinAndRecreate.api;

import vrp.api.Customer;
import vrp.basics.Tour;
import vrp.basics.Vehicle;

/**
 * 
 * @author stefan schroeder
 *
 */

public interface TourAgentFactory {

	public Tour createRoundTour(Customer n);

	public TourAgent createTourAgent(Tour tour, Vehicle vehicle);

	public Tour createRoundTour(Customer from, Customer to);
	

}
