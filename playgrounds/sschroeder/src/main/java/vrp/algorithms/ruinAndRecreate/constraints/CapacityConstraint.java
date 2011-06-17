/**
 * 
 */
package vrp.algorithms.ruinAndRecreate.constraints;

import org.apache.log4j.Logger;

import vrp.api.Constraints;
import vrp.basics.Tour;
import vrp.basics.TourActivity;


/**
 * @author stefan schroeder
 *
 */
public class CapacityConstraint implements Constraints {

	private Logger logger = Logger.getLogger(CapacityConstraint.class);
	
	private int maxCap;
	
	public CapacityConstraint(int maxCap) {
		super();
		this.maxCap = maxCap;
	}

	
	public boolean judge(Tour tour) {
		int currentCap = 0;
		for(TourActivity tourAct : tour.getActivities()){
			if(tourAct.getCurrentLoad() > maxCap || tourAct.getCurrentLoad() < 0){
				logger.debug("capacity-conflict (maxCap=" + maxCap + ";currentCap=" + currentCap + " on tour " + tour);
				return false;
			}
			if(tourAct.hasTimeWindowConflict()){
				logger.debug("timeWindow-conflic on tour " + tour);
				return false;
			}
		}
		return true;
	}

}
