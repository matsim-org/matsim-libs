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
package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.TourActivity;

public class LocalMCCalculator implements MarginalInsertionCostsCalculator {

	private Costs costs;

	public LocalMCCalculator(Costs costs) {
		super();
		this.costs = costs;
	}

	@Override
	public double calculateMarginalCosts(TourActivity prevAct, TourActivity nextAct, TourActivity newAct) {
		double earliestDepTimeFromPrevAct = TourUtils.getEarliestDepTimeFromAct(prevAct);
		double tt_prevAct2newAct = getTravelTime(prevAct.getLocationId(), newAct.getLocationId(), earliestDepTimeFromPrevAct);
		
		double earliestArrTimeAtNewAct = earliestDepTimeFromPrevAct + tt_prevAct2newAct;
		double earliestOperationStartTimeAtNewAct = TourUtils.getEarliestOperationStartTime(earliestArrTimeAtNewAct, newAct);
		
		double earliestDepTimeFromNewAct = earliestOperationStartTimeAtNewAct + newAct.getOperationTime();
		double tt_newAct2nextAct = getTravelTime(newAct.getLocationId(),nextAct.getLocationId(),earliestDepTimeFromNewAct);
		
		double earliestArrTimeAtNextAct = earliestDepTimeFromNewAct + tt_newAct2nextAct;
		double earliestOperationStartTimeAtNextAct = TourUtils.getEarliestOperationStartTime(earliestArrTimeAtNextAct, nextAct);
		
		//preCheck whether time-constraints are met
		if(earliestOperationStartTimeAtNewAct > newAct.getLatestOperationStartTime() || earliestOperationStartTimeAtNextAct > nextAct.getLatestOperationStartTime()){
			return Double.MAX_VALUE;
		}
				
		double latestOperationStartTimeAtNewAct = nextAct.getLatestOperationStartTime() - newAct.getOperationTime() - 
			getBackwardTravelTime(newAct.getLocationId(),nextAct.getLocationId(),nextAct.getLatestOperationStartTime());
		
		double latestOperationStartTimeAtPrevAct = newAct.getLatestOperationStartTime() - prevAct.getOperationTime() - 
			getBackwardTravelTime(prevAct.getLocationId(),newAct.getLocationId(), newAct.getLatestOperationStartTime());
		
		if(latestOperationStartTimeAtNewAct < newAct.getEarliestOperationStartTime() || latestOperationStartTimeAtPrevAct < prevAct.getEarliestOperationStartTime()){
			return Double.MAX_VALUE;
		}
		
		double marginalCost = 
			getGeneralizedCosts(prevAct.getLocationId(), newAct.getLocationId(), earliestDepTimeFromPrevAct) +
			getGeneralizedCosts(newAct.getLocationId(), nextAct.getLocationId(), earliestDepTimeFromNewAct) -
			getGeneralizedCosts(prevAct.getLocationId(), nextAct.getLocationId(), earliestDepTimeFromPrevAct);
		
		
		return marginalCost;

	}
	
	private double getTravelTime(String fromId, String toId, double time) {
		return costs.getTransportTime(fromId, toId, time);
	}

	private double getGeneralizedCosts(String fromId, String toId, double time) {
		return costs.getTransportCost(fromId, toId, time);
	}

	private double getBackwardTravelTime(String fromId, String toId, double arrTime) {
		return costs.getBackwardTransportTime(fromId, toId, arrTime);
	}

}
