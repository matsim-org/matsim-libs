package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public class TourUtils {
	
	public static double getEarliestDepTimeFromAct(TourActivity tourAct){
		return tourAct.getEarliestOperationStartTime() + tourAct.getOperationTime();
	}
	
	public static double getEarliestOperationStartTime(double arrivalTimeAtTourAct, TourActivity tourAct){
		return Math.max(arrivalTimeAtTourAct, tourAct.getEarliestOperationStartTime());
	}
	
	public static double getMarginalInsertionCosts(TourActivity prevAct, TourActivity nextAct, TourActivity newAct, Vehicle vehicle, Costs costs){
		double earliestDepTimeFromPrevAct = TourUtils.getEarliestDepTimeFromAct(prevAct);
		double tt_prevAct2newAct = costs.getTransportTime(prevAct.getLocationId(), newAct.getLocationId(), earliestDepTimeFromPrevAct);
		
		double earliestArrTimeAtNewAct = earliestDepTimeFromPrevAct + tt_prevAct2newAct;
		double earliestOperationStartTimeAtNewAct = TourUtils.getEarliestOperationStartTime(earliestArrTimeAtNewAct, newAct);
		
		double earliestDepTimeFromNewAct = earliestOperationStartTimeAtNewAct + newAct.getOperationTime();
		double tt_newAct2nextAct = costs.getTransportTime(newAct.getLocationId(),nextAct.getLocationId(),earliestDepTimeFromNewAct);
		
		double earliestArrTimeAtNextAct = earliestDepTimeFromNewAct + tt_newAct2nextAct;
		double earliestOperationStartTimeAtNextAct = TourUtils.getEarliestOperationStartTime(earliestArrTimeAtNextAct, nextAct);
		
		double marginalCost = 
			costs.getTransportCost(prevAct.getLocationId(), newAct.getLocationId(), earliestDepTimeFromPrevAct) +
			costs.getTransportCost(newAct.getLocationId(), nextAct.getLocationId(), earliestDepTimeFromNewAct) -
			costs.getTransportCost(prevAct.getLocationId(), nextAct.getLocationId(), earliestDepTimeFromPrevAct);
		
		//preCheck whether time-constraints are met
		if(earliestOperationStartTimeAtNewAct > newAct.getLatestOperationStartTime() || earliestOperationStartTimeAtNextAct > nextAct.getLatestOperationStartTime()){
			return Double.MAX_VALUE;
		}
				
		double latestOperationStartTimeAtNewAct = nextAct.getLatestOperationStartTime() - newAct.getOperationTime() - 
			costs.getBackwardTransportCost(newAct.getLocationId(),nextAct.getLocationId(),nextAct.getLatestOperationStartTime());
		
		double latestOperationStartTimeAtPrevAct = newAct.getLatestOperationStartTime() - prevAct.getOperationTime() - 
			costs.getBackwardTransportCost(prevAct.getLocationId(),newAct.getLocationId(), newAct.getLatestOperationStartTime());
		
		if(latestOperationStartTimeAtNewAct < newAct.getEarliestOperationStartTime() || latestOperationStartTimeAtPrevAct < prevAct.getEarliestOperationStartTime()){
			return Double.MAX_VALUE;
		}
		return marginalCost;
	}

}
