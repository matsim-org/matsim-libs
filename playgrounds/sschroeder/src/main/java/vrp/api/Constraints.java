package vrp.api;

import vrp.basics.Tour;

/**
 * 
 * @author stefan schroeder
 *
 */

public interface Constraints {
	
	public boolean judge(Tour tour);

}