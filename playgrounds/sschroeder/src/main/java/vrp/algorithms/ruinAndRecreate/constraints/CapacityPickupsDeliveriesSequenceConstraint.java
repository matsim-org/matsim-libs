/**
 * 
 */
package vrp.algorithms.ruinAndRecreate.constraints;

import java.util.HashSet;
import java.util.Set;

import javax.management.MXBean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import vrp.api.Constraints;
import vrp.basics.Delivery;
import vrp.basics.EnRouteDelivery;
import vrp.basics.EnRoutePickup;
import vrp.basics.Pickup;
import vrp.basics.TimeWindow;
import vrp.basics.Tour;
import vrp.basics.TourActivity;


/**
 * @author stefan schroeder
 *
 */
public class CapacityPickupsDeliveriesSequenceConstraint implements Constraints {

	private Logger logger = Logger.getLogger(CapacityPickupsDeliveriesSequenceConstraint.class);
	
	private int maxCap;
	
	public CapacityPickupsDeliveriesSequenceConstraint(int maxCap) {
		super();
		this.maxCap = maxCap;
	}

	
	public boolean judge(Tour tour) {
		int currentCap = 0;
		boolean deliveryStarted = false;
		Set<Id> openCustomers = new HashSet<Id>();
		for(TourActivity tourAct : tour.getActivities()){
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
//			TimeWindow theoreticalTimeWindow = tourAct.getCustomer().getTheoreticalTimeWindow();
//			if(maxTimeWindow == null){
//				maxTimeWindow = theoreticalTimeWindow;
//			}
//			if(tourAct instanceof EnRoutePickup){
//				if(theoreticalTimeWindow.getStart() < maxTimeWindow.getStart()){
//					return false;
//				}
//				else{
//					if(theoreticalTimeWindow.getStart() > maxTimeWindow.getStart()){
//						maxTimeWindow = theoreticalTimeWindow;
//					}
//				}
//			}
			
		}
		return true;
	}

}
