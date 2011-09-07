/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
/**
 * 
 */
package vrp.algorithms.ruinAndRecreate.constraints;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
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
public class TimeAndCapacityPickupsDeliveriesSequenceBreakConstraint implements Constraints {
	
	public static class Break {
		static double start = 4*3600;
		static double end = 14*3600;
		
		public static boolean isWithin(double time){
			double normalisedTime = time%(24*3600);
			if(normalisedTime >= start && normalisedTime<end){
				return true;
			}
			else{
				return false;
			}
		}
	}

	private Logger logger = Logger.getLogger(TimeAndCapacityPickupsDeliveriesSequenceBreakConstraint.class);
	
	private int maxCap;
	
	private int maxTime;
	
	private Costs costs;
	
	private double breakTimeStart;
	
	private double breakTimeEnd;
	
	public TimeAndCapacityPickupsDeliveriesSequenceBreakConstraint(int maxCap, int maxTime, Costs costs) {
		super();
		this.maxCap = maxCap;
		this.costs = costs;
		this.maxTime = maxTime;
	}

	
	@Override
	public boolean judge(Tour tour) {
		int currentCap = 0;
		boolean deliveryStarted = false;
		Set<String> openCustomers = new HashSet<String>();
		double time = 0.0;
		
		TourActivity lastAct = null;
		TourActivity firstAct = null;
		for(TourActivity tourAct : tour.getActivities()){
			if(lastAct == null){
				firstAct = tourAct;
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
				String relatedCustomer = tourAct.getCustomer().getRelation().getCustomer().getId();
				if(openCustomers.contains(relatedCustomer)){
					openCustomers.remove(relatedCustomer);
				}
				else{
					return false;
				}
			}
			if(isWithinBreak(tourAct.getEarliestArrTime()) || isWithinBreak(tourAct.getLatestArrTime())){
				return false;
			}
		}
		return true;
	}


	private boolean isWithinBreak(double time) {
		if(time >= breakTimeStart && time < breakTimeStart){
			return true;
		}
		return false;
	}


	@Override
	public boolean judge(Tour tour, Vehicle vehicle) {
		maxCap = vehicle.getCapacity();
		int currentCap = 0;
		boolean deliveryStarted = false;
		Set<String> openCustomers = new HashSet<String>();
		double time = 0.0;
		TourActivity lastAct = null;
		boolean firstPickup = true;
		for(TourActivity tourAct : tour.getActivities()){
			if(firstPickup && tourAct instanceof EnRoutePickup){
				firstPickup = false;
			}
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
				String relatedCustomer = tourAct.getCustomer().getRelation().getCustomer().getId();
				if(openCustomers.contains(relatedCustomer)){
					openCustomers.remove(relatedCustomer);
				}
				else{
					return false;
				}
			}
			if(Break.isWithin(tourAct.getEarliestArrTime())){
				return false;
			}
		}
		return true;
	}

}
