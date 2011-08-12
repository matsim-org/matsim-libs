package vrp.algorithms.ruinAndRecreate.api;

import vrp.basics.Tour;
import vrp.basics.Vehicle;

/**
 * 
 * @author stefan schroeder
 *
 */

public interface TourAgentFactory {


	public TourAgent createTourAgent(Tour tour, Vehicle vehicle);

}
