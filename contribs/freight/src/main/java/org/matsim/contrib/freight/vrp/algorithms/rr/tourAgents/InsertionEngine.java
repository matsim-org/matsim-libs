package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public class InsertionEngine {
	
	class InsertionData {
		double mc;
		Integer pickupInsertionIndex;
		Integer deliveryInsertionIndex;
		public InsertionData(double mc, Integer pickupInsertionIndex, Integer deliveryInsertionIndex) {
			super();
			this.mc = mc;
			this.pickupInsertionIndex = pickupInsertionIndex;
			this.deliveryInsertionIndex = deliveryInsertionIndex;
		}
	}
	
	private int startOuter;
	
	private int endOuter;
	
	private int startInner;
	
	private int endInner;
	
	private Costs costs;
	
	public InsertionEngine(Costs costs) {
		super();
		this.costs = costs;
	}
	
	public InsertionData findBestInsertion(Vehicle vehicle, Tour tour, Pickup pickup, Delivery delivery, double bestKnownPrice){
		ConstraintManager constraintManager = new ConstraintManager(vehicle,tour,pickup,delivery);
	
		Double bestMarginalCost = bestKnownPrice;
		Integer bestPickupInsertionIndex = null;
		Integer bestDeliveryInsertionIndex = null;
		
		int firstDel = tour.getActivities().size()/2;
		int pick = firstDel-1;
		
		if(pick==0){
			pick=1;
			firstDel=2;
		}
		
		constraintManager.informInsertionStarts();
//		for(int pickupIndex=1;pickupIndex<tour.getActivities().size();pickupIndex++){
		for(int pickupIndex=pick;pickupIndex<firstDel;pickupIndex++){
			constraintManager.informPickupIterationStarts(pickupIndex);
			if(constraintManager.isContinue()){
				continue;
			}
			if(constraintManager.isBreak()){
				break;
			}
			double mc4pickup = getMarginalInsertionCosts(getActivity(tour,pickupIndex-1), getActivity(tour,pickupIndex), pickup);
			if(mc4pickup > bestMarginalCost){
				continue;
			}
//			for(int deliveryIndex=pickupIndex;deliveryIndex<tour.getActivities().size();deliveryIndex++){
			for(int deliveryIndex=pickupIndex;deliveryIndex<tour.getActivities().size();deliveryIndex++){
				constraintManager.informDeliveryIterationStarts(deliveryIndex);
				if(constraintManager.isContinue()){
					continue;
				}
				if(constraintManager.isBreak()){
					break;
				}
				double totalMarginalCost;
				double mc4delivery;
				if(pickupIndex == deliveryIndex){
					mc4delivery = getMarginalInsertionCosts(pickup, getActivity(tour,pickupIndex), delivery);
				}
				else{
					mc4delivery = getMarginalInsertionCosts(getActivity(tour,deliveryIndex-1), getActivity(tour,deliveryIndex), delivery);
				}
				totalMarginalCost = mc4pickup + mc4delivery;
				if(totalMarginalCost < bestMarginalCost){
					bestMarginalCost = totalMarginalCost;
					bestPickupInsertionIndex = pickupIndex;
					bestDeliveryInsertionIndex = deliveryIndex;
				}
				constraintManager.informDeliveryIterationEnds(deliveryIndex);
			}
			constraintManager.informPickupIterationEnds(pickupIndex);
		}
		constraintManager.informInsertionEnds();
		return new InsertionData(bestMarginalCost, bestPickupInsertionIndex, bestDeliveryInsertionIndex);
	}
	
	private double getMarginalInsertionCosts(TourActivity prevAct, TourActivity nextAct, TourActivity newAct) {
		double earliestDepTimeFromPrevAct = TourUtils.getEarliestDepTimeFromAct(prevAct);
		double tt_prevAct2newAct = getTravelTime(prevAct.getLocationId(), newAct.getLocationId(), earliestDepTimeFromPrevAct);
		
		double earliestArrTimeAtNewAct = earliestDepTimeFromPrevAct + tt_prevAct2newAct;
		double earliestOperationStartTimeAtNewAct = TourUtils.getEarliestOperationStartTime(earliestArrTimeAtNewAct, newAct);
		
		double earliestDepTimeFromNewAct = earliestOperationStartTimeAtNewAct + newAct.getOperationTime();
		double tt_newAct2nextAct = getTravelTime(newAct.getLocationId(),nextAct.getLocationId(),earliestDepTimeFromNewAct);
		
		double earliestArrTimeAtNextAct = earliestDepTimeFromNewAct + tt_newAct2nextAct;
		double earliestOperationStartTimeAtNextAct = TourUtils.getEarliestOperationStartTime(earliestArrTimeAtNextAct, nextAct);
		
		double marginalCost = 
			getGeneralizedCosts(prevAct.getLocationId(), newAct.getLocationId(), earliestDepTimeFromPrevAct) +
			getGeneralizedCosts(newAct.getLocationId(), nextAct.getLocationId(), earliestDepTimeFromNewAct) -
			getGeneralizedCosts(prevAct.getLocationId(), nextAct.getLocationId(), earliestDepTimeFromPrevAct);
		
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
		return marginalCost;
	}
	
	private double getBackwardTravelTime(String fromId, String toId, double arrTime) {
		return costs.getBackwardTransportTime(fromId, toId, arrTime);
	}


	private double getTravelTime(String fromId, String toId, double time) {
		return costs.getTransportTime(fromId, toId, time);
	}

	private double getGeneralizedCosts(String fromId, String toId, double time) {
		return costs.getTransportCost(fromId, toId, time);
	}

	private TourActivity getActivity(Tour tour, int i) {
		return tour.getActivities().get(i);
	}



}
