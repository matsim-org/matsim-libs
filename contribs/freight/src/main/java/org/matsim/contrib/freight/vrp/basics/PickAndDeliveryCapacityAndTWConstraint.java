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
package org.matsim.contrib.freight.vrp.basics;

import org.apache.log4j.Logger;

/**
 * @author stefan schroeder
 *
 */
public class PickAndDeliveryCapacityAndTWConstraint implements Constraints {

	private Logger logger = Logger.getLogger(PickAndDeliveryCapacityAndTWConstraint.class);
	
	@Override
	public boolean judge(Tour tour, Vehicle vehicle) {
		int currentLoad = 0;
		int maxCap = vehicle.getCapacity();
		for(TourActivity tourAct : tour.getActivities()){
			if(tourAct instanceof JobActivity){
				currentLoad += ((JobActivity) tourAct).getCapacityDemand();
			}
			if(currentLoad > vehicle.getCapacity()){
				logger.debug("capacity-conflict (maxCap=" + maxCap + ";currentLoad=" + currentLoad + " on tour " + tour);
				return false;
			}
			if(tourAct.getLatestArrTime() < tourAct.getEarliestArrTime()){
				logger.debug("timeWindow-conflic on tour " + tour);
				return false;
			}
		}
		return true;
	}
}
