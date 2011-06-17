package vrp.algorithms.ruinAndRecreate.api;

import vrp.api.Customer;
import vrp.api.VRP;
import vrp.basics.Tour;
import vrp.basics.Vehicle;

/**
 * 
 * @author stefan schroeder
 *
 */

public interface TourAgentFactory {

	public Tour createRoundTour(Customer depot, Customer n);

	public TourAgent createTourAgent(VRP vrp, Tour tour, Vehicle createVehicle);

	public Tour createRoundTour(Customer depot, Customer from, Customer to);

}
