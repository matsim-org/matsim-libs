package vrp.api;

import vrp.basics.Tour;
import vrp.basics.Vehicle;

/**
 * 
 * @author stefan schroeder
 *
 */

public interface Constraints {
	
	public boolean judge(Tour tour);
	
	public boolean judge(Tour tour, Vehicle vehicle);

}