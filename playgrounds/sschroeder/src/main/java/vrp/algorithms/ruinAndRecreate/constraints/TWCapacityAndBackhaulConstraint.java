/**
 * 
 */
package vrp.algorithms.ruinAndRecreate.constraints;

import org.apache.log4j.Logger;

import vrp.api.Constraints;
import vrp.basics.DepotDelivery;
import vrp.basics.DepotPickup;
import vrp.basics.Tour;
import vrp.basics.TourActivity;
import vrp.basics.Vehicle;


/**
 * @author stefan schroeder
 *
 */
public class TWCapacityAndBackhaulConstraint implements Constraints {

	private Logger logger = Logger.getLogger(TWCapacityAndBackhaulConstraint.class);
	
	private int maxCap;
	
	public TWCapacityAndBackhaulConstraint(int maxCap) {
		super();
		this.maxCap = maxCap;
	}

	public boolean judge(Tour tour) {
		int currentCap = 0;
		boolean pickupOccured = false;
		for(TourActivity tourAct : tour.getActivities()){
			if(tourAct.getCurrentLoad() > maxCap || tourAct.getCurrentLoad() < 0){
				logger.debug("capacity-conflict (maxCap=" + maxCap + ";currentCap=" + currentCap + " on tour " + tour);
				return false;
			}
			if(tourAct.hasTimeWindowConflict()){
				logger.debug("timeWindow-conflic on tour " + tour);
				return false;
			}
			if(tourAct instanceof DepotPickup){
				pickupOccured = true;
			}
			if(tourAct instanceof DepotDelivery){
				if(pickupOccured){
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean judge(Tour tour, Vehicle vehicle) {
		int currentCap = 0;
		maxCap = vehicle.getCapacity();
		boolean pickupOccured = false;
		for(TourActivity tourAct : tour.getActivities()){
			if(tourAct.getCurrentLoad() > maxCap || tourAct.getCurrentLoad() < 0){
				logger.debug("capacity-conflict (maxCap=" + maxCap + ";currentCap=" + currentCap + " on tour " + tour);
				return false;
			}
			if(tourAct.hasTimeWindowConflict()){
				logger.debug("timeWindow-conflic on tour " + tour);
				return false;
			}
			if(tourAct instanceof DepotPickup){
				pickupOccured = true;
			}
			if(tourAct instanceof DepotDelivery){
				if(pickupOccured){
					return false;
				}
			}
		}
		return true;
	}

}
