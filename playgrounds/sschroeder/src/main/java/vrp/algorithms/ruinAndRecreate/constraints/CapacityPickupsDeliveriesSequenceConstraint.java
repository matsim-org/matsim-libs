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
import vrp.basics.DeliveryFromDepot;
import vrp.basics.EnRouteDelivery;
import vrp.basics.EnRoutePickup;
import vrp.basics.PickupToDepot;
import vrp.basics.Tour;
import vrp.basics.TourActivity;
import vrp.basics.Vehicle;


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


	@Override
	public boolean judge(Tour tour, Vehicle vehicle) {
		int currentCap = 0;
		maxCap = vehicle.getCapacity();
		boolean deliveryStarted = false;
		Set<String> openCustomers = new HashSet<String>();
		for(TourActivity tourAct : tour.getActivities()){
			
			if(tourAct.getCurrentLoad() > maxCap || tourAct.getCurrentLoad() < 0){
				logger.debug("capacity-conflict (maxCap=" + maxCap + ";currentCap=" + currentCap + " on tour " + tour);
				return false;
			}
			if(tourAct.hasTimeWindowConflict()){
				logger.debug("timeWindow-conflic on tour " + tour);
				return false;
			}
			if(tourAct instanceof EnRoutePickup || tourAct instanceof PickupToDepot){
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
			if(tourAct instanceof EnRouteDelivery || tourAct instanceof DeliveryFromDepot){
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

			
		}
		return true;
	}

}
