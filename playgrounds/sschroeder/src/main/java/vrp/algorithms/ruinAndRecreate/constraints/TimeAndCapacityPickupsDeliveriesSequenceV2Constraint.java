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
import vrp.basics.DepotDelivery;
import vrp.basics.EnRouteDelivery;
import vrp.basics.EnRoutePickup;
import vrp.basics.DepotPickup;
import vrp.basics.Tour;
import vrp.basics.TourActivity;
import vrp.basics.Vehicle;


/**
 * @author stefan schroeder
 *
 */
public class TimeAndCapacityPickupsDeliveriesSequenceV2Constraint implements Constraints {

	private Logger logger = Logger.getLogger(TimeAndCapacityPickupsDeliveriesSequenceV2Constraint.class);
	
	private int maxCap;
	
	private double maxTimeOutOfDepot;
	
	private double maxTimeOnTheRoad;
	
	private int maxNuOfDifferentLocations;
	
	private Costs costs;
	
	public TimeAndCapacityPickupsDeliveriesSequenceV2Constraint(int maxCap, double maxTimeOutOfDepot, double maxTimeOnTheRoad, int maxNuOfDifferentLocations, Costs costs) {
		super();
		this.maxCap = maxCap;
		this.costs = costs;
		this.maxTimeOutOfDepot = maxTimeOutOfDepot;
		this.maxTimeOnTheRoad = maxTimeOnTheRoad;
		this.maxNuOfDifferentLocations = maxNuOfDifferentLocations;
	}

	
	public boolean judge(Tour tour) {
		int currentCap = 0;
		boolean deliveryStarted = false;
		Set<Id> openCustomers = new HashSet<Id>();
		double time = 0.0;
		double timeOutOfDepot = tour.getActivities().get(0).getLatestArrTime() - tour.getActivities().getLast().getEarliestArrTime();
		Set<Id> differentLocations = new HashSet<Id>();
		if(timeOutOfDepot > maxTimeOutOfDepot){
			return false;
		}
		TourActivity lastAct = null;
		for(TourActivity tourAct : tour.getActivities()){
			if(lastAct == null){
				lastAct = tourAct;
			}
			else{
				time += costs.getTime(lastAct.getLocation(), tourAct.getLocation());
			}	
			differentLocations.add(tourAct.getCustomer().getLocation().getId());
			if(differentLocations.size() > maxNuOfDifferentLocations){
				return false;
			}
			if(time > maxTimeOutOfDepot){
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
			if(tourAct instanceof EnRoutePickup || tourAct instanceof DepotPickup){
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
			if(tourAct instanceof EnRouteDelivery || tourAct instanceof DepotDelivery){
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


	@Override
	public boolean judge(Tour tour, Vehicle vehicle) {
		maxCap = vehicle.getCapacity();
		int currentCap = 0;
		boolean deliveryStarted = false;
		Set<Id> openCustomers = new HashSet<Id>();
		Set<Id> differentLocations = new HashSet<Id>();
		double timeOutOfDepot = tour.getActivities().get(0).getLatestArrTime() - tour.getActivities().getLast().getEarliestArrTime();
		double timeOnTheRoad = 0.0;
		
		if(timeOutOfDepot > maxTimeOutOfDepot){
			return false;
		}
		TourActivity lastAct = null;
		for(TourActivity tourAct : tour.getActivities()){
			if(lastAct == null){
				lastAct = tourAct;
			}
			else{
				timeOnTheRoad += costs.getTime(lastAct.getLocation(), tourAct.getLocation());
			}
			differentLocations.add(tourAct.getCustomer().getLocation().getId());
			if(differentLocations.size() > maxNuOfDifferentLocations){
				return false;
			}
			if(timeOnTheRoad > maxTimeOnTheRoad){
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
			if(tourAct instanceof EnRoutePickup || tourAct instanceof DepotPickup){
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
			if(tourAct instanceof EnRouteDelivery || tourAct instanceof DepotDelivery){
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
