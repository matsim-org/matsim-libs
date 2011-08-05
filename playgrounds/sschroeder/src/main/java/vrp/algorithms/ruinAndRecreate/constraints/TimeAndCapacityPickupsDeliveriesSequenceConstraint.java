/**
 * 
 */
package vrp.algorithms.ruinAndRecreate.constraints;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.basics.Delivery;
import vrp.basics.EnRouteDelivery;
import vrp.basics.EnRoutePickup;
import vrp.basics.Pickup;
import vrp.basics.Tour;
import vrp.basics.TourActivity;


/**
 * @author stefan schroeder
 *
 */
public class TimeAndCapacityPickupsDeliveriesSequenceConstraint implements Constraints {

	private Logger logger = Logger.getLogger(TimeAndCapacityPickupsDeliveriesSequenceConstraint.class);
	
	private int maxCap;
	
	private int maxTime;
	
	private Costs costs;
	
	public TimeAndCapacityPickupsDeliveriesSequenceConstraint(int maxCap, int maxTime, Costs costs) {
		super();
		this.maxCap = maxCap;
		this.costs = costs;
		this.maxTime = maxTime;
	}

	
	public boolean judge(Tour tour) {
		int currentCap = 0;
		boolean deliveryStarted = false;
		Set<Id> openCustomers = new HashSet<Id>();
		double time = 0.0;
		TourActivity lastAct = null;
		for(TourActivity tourAct : tour.getActivities()){
			if(lastAct == null){
				lastAct = tourAct;
			}
			else{
				time += costs.getTime(lastAct.getLocation(), tourAct.getLocation());
			}
			if(time > maxTime){
				return false;
			}
			if(tourAct.getCurrentLoad() > maxCap || tourAct.getCurrentLoad() < 0){
				logger.debug("capacity-conflict (maxCap=" + maxCap + ";currentCap=" + currentCap + " on tour " + tour);
				return false;
			}
			if(tourAct.hasTimeWindowConflict()){
				logger.debug("timeWindow-conflic on tour " + tour);
				return false;
			}
			if(tourAct instanceof EnRoutePickup || tourAct instanceof Pickup){
				if(deliveryStarted){
					if(!openCustomers.isEmpty()){
						return false;
					}
					else{
						deliveryStarted = false;
					}
				}
				openCustomers.add(tourAct.getCustomer().getId());
			}
			if(tourAct instanceof EnRouteDelivery || tourAct instanceof Delivery){
				if(deliveryStarted == false){
					deliveryStarted = true;
				}
				Id relatedCustomer = tourAct.getCustomer().getRelation().getCustomer().getId();
				if(openCustomers.contains(relatedCustomer)){
					openCustomers.remove(relatedCustomer);
				}
				else{
					return false;
				}
			}
		}
		return true;
	}

}
