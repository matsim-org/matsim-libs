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
package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;

class LocalMCCalculator extends LeastCostInsertionCalculator {

	private VehicleRoutingCosts costs;

	public LocalMCCalculator(VehicleRoutingCosts costs) {
		super();
		this.costs = costs;
	}

	@Override
	double calculateLeastCost(Tour tour, TourActivity prevAct, TourActivity nextAct, TourActivity newAct, Driver driver, Vehicle vehicle) {
		double earliestDepTimeFromPrevAct = prevAct.getEarliestOperationStartTime() + prevAct.getOperationTime();
		
		double tt_prevAct2newAct = costs.getTransportTime(prevAct.getLocationId(), newAct.getLocationId(), earliestDepTimeFromPrevAct, driver, vehicle);
		
		double earliestArrTimeAtNewAct = earliestDepTimeFromPrevAct + tt_prevAct2newAct;
		double earliestOperationStartTimeAtNewAct = Math.max(earliestArrTimeAtNewAct, newAct.getEarliestOperationStartTime());
		
		double depTimeFromNewAct = earliestOperationStartTimeAtNewAct + newAct.getOperationTime();
		double tt_newAct2nextAct = costs.getTransportTime(newAct.getLocationId(),nextAct.getLocationId(), depTimeFromNewAct, driver, vehicle);
		
		double arrTimeAtNextAct = depTimeFromNewAct + tt_newAct2nextAct;
		double earliestOperationStartTimeAtNextAct = Math.max(arrTimeAtNextAct, nextAct.getEarliestOperationStartTime());
		
		double backwardArrTimeAtNewAct = nextAct.getLatestOperationStartTime() - costs.getBackwardTransportTime(newAct.getLocationId(), nextAct.getLocationId(), nextAct.getLatestOperationStartTime(), driver, vehicle);
		double potentialLatestOperationStartTimeAtNewAct = backwardArrTimeAtNewAct - newAct.getOperationTime();
		
		double latestOperationStartTimeAtNewAct = Math.min(newAct.getLatestOperationStartTime(), potentialLatestOperationStartTimeAtNewAct);
		
		double backwardArrTimeAtPrevAct = backwardArrTimeAtNewAct - costs.getBackwardTransportTime(prevAct.getLocationId(),newAct.getLocationId(), backwardArrTimeAtNewAct, driver, vehicle);
		double potentialLatestOperationStartTimeAtPrevAct = backwardArrTimeAtPrevAct - prevAct.getOperationTime();
		
		double latestOperationStartTimeAtPrevAct = Math.min(prevAct.getLatestOperationStartTime(), potentialLatestOperationStartTimeAtPrevAct);
		
		//check feasibility of inserting newAct at this position
		if(earliestOperationStartTimeAtNewAct > latestOperationStartTimeAtNewAct){
			return Double.MAX_VALUE;
		}
		else if(earliestOperationStartTimeAtNextAct > nextAct.getLatestOperationStartTime()){
			return Double.MAX_VALUE;
		}
		else if(latestOperationStartTimeAtPrevAct < prevAct.getEarliestOperationStartTime()){
			return Double.MAX_VALUE;
		}
		else{
			double marginalCost = 
				costs.getTransportCost(prevAct.getLocationId(), newAct.getLocationId(), earliestDepTimeFromPrevAct, driver, vehicle) +
				costs.getTransportCost(newAct.getLocationId(), nextAct.getLocationId(), depTimeFromNewAct, driver, vehicle) -
				costs.getTransportCost(prevAct.getLocationId(), nextAct.getLocationId(), earliestDepTimeFromPrevAct, driver, vehicle);
			return marginalCost;
		}
	}

}
