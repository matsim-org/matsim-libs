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
			if(tourAct.getLatestOperationStartTime() < tourAct.getEarliestOperationStartTime()){
//				logger.debug("timeWindow-conflic on tour " + tour);
//				counterRejected.incCounter();
//				counterRejected.printCounter();
				return false;
			}
		}
		return true;
	}
}
