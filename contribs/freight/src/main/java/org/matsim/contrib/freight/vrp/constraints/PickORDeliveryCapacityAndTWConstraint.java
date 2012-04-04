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
package org.matsim.contrib.freight.vrp.constraints;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.core.utils.misc.Counter;

/**
 * @author stefan schroeder
 *
 */
public class PickORDeliveryCapacityAndTWConstraint implements Constraints {

	private Logger logger = Logger.getLogger(PickORDeliveryCapacityAndTWConstraint.class);
	
	public double maxTimeInOperation = Double.MAX_VALUE;
	
	public Counter counter;
	
	public Counter counterRejected;
	
	public PickORDeliveryCapacityAndTWConstraint() {
		super();
		counter = new Counter("#constraints calls ");
		counterRejected = new Counter("#constraints rejected: ");
	}

	@Override
	public boolean judge(Tour tour, Vehicle vehicle) {
//		counter.incCounter();
//		c
		int currentLoad = 0;
		boolean deliveryOccured = false;
		for(TourActivity tourAct : tour.getActivities()){
			if(tourAct instanceof JobActivity){
				if(deliveryOccured){
					if(tourAct instanceof Pickup){
//						counterRejected.incCounter();
//						counterRejected.printCounter();
						return false;
					}
				}
				currentLoad += ((JobActivity) tourAct).getCapacityDemand();
				if(tourAct instanceof Delivery){
					deliveryOccured = true;
				}
			}
			if(currentLoad > vehicle.getCapacity()){
//				logger.debug("capacity-conflict (maxCap=" + maxCap + ";currentLoad=" + currentLoad + " on tour " + tour);
//				counterRejected.incCounter();
//				counterRejected.printCounter();
				return false;
			}
			if(tourAct.getLatestArrTime() < tourAct.getEarliestArrTime()){
//				logger.debug("timeWindow-conflic on tour " + tour);
//				counterRejected.incCounter();
//				counterRejected.printCounter();
				return false;
			}
		}
		return true;
	}
}
