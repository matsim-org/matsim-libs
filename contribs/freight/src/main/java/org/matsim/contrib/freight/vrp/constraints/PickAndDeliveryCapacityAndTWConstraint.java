/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.matsim.contrib.freight.vrp.constraints;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

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
			if(tourAct.getLatestOperationStartTime() < tourAct.getEarliestOperationStartTime()){
				logger.debug("timeWindow-conflic on tour " + tour);
				return false;
			}
		}
		return true;
	}
}
