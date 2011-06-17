package vrp.algorithms.clarkeAndWright;

import vrp.api.Constraints;
import vrp.basics.Tour;
import vrp.basics.TourActivity;

/**
 * 
 * @author stefan schroeder
 *
 */

public class ClarkeWrightCapacityConstraint implements Constraints{

	private int maxCap;
	
	public ClarkeWrightCapacityConstraint(int maxCap) {
		super();
		this.maxCap = maxCap;
	}

	public boolean judge(Tour tour) {
		int currentCap = 0;
		for(TourActivity acts : tour.getActivities()){
			currentCap += acts.getCustomer().getDemand();
		}
		if(currentCap<=maxCap && currentCap >= maxCap*-1){
			return true;
		}
		else{
			return false;
		}
	}

}
