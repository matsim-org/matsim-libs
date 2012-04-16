package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.InsertionEngine.InsertionData;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.OfferData.MetaData;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.OfferData.Offer;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.constraints.Constraints;

public class PickupAndDeliveryOfferMaker implements OfferMaker{
	
	
	private static Logger logger = Logger.getLogger(PickupAndDeliveryOfferMaker.class);

	private Costs costs;
	
	private Constraints constraints;
	
	private InsertionEngine insertionEngine;
	
	public PickupAndDeliveryOfferMaker(Costs costs, Constraints constraints) {
		super();
		this.costs = costs;
		this.constraints = constraints;
		insertionEngine = new InsertionEngine(costs);
	}

	@Override
	public OfferData makeOffer(Vehicle vehicle, Tour tour, Job job, double bestKnownPrice) {
		Shipment shipment = (Shipment)job;
		Pickup pickup = createPickup(shipment);
		Delivery delivery = createDelivery(shipment);
		InsertionData insertionData = insertionEngine.findBestInsertion(vehicle, tour, pickup, delivery, bestKnownPrice);
		if(insertionData.pickupInsertionIndex != null){
			OfferData offerData = new OfferData(new Offer(insertionData.mc),new MetaData(insertionData.pickupInsertionIndex,insertionData.deliveryInsertionIndex));
			return offerData;
		}
		else{
			return new OfferData(new Offer(Double.MAX_VALUE),null);
		}
	}
	
//	public InsertionData findBestInsertion(Vehicle vehicle, Tour tour, Pickup pickup, Delivery delivery, double bestKnownPrice){
//		Constraint constraint = new Constraint();
//		
//		Double bestMarginalCost = bestKnownPrice;
//		Integer pickupInsertionIndex = null;
//		Integer deliveryInsertionIndex = null;
//		
//		int loadRecord = 0;
//		
//		
//		for(int i=1;i<tour.getActivities().size();i++){
//			if(getActivity(tour,i) instanceof JobActivity){
//				loadRecord += ((JobActivity)getActivity(tour,i)).getCapacityDemand();
//			}
//			double mc4pickup = getMarginalInsertionCosts(getActivity(tour,i-1), getActivity(tour,i), pickup);
//			int currentLoad = loadRecord + pickup.getCapacityDemand();
//			if(!constraint.check(getActivity(tour,i-1), pickup, getActivity(tour,i))){
//				continue;
//			}
//			if(vehicleCapExceeded(vehicle,currentLoad)){
//				continue;
//			}
//			if(mc4pickup > bestMarginalCost){
//				continue;
//			}
//			for(int j=i;j<tour.getActivities().size();j++){
//				double totalMarginalCost;
//				double mc4delivery;
//				if(i == j){
//					mc4delivery = getMarginalInsertionCosts(pickup, getActivity(tour,i), delivery);
//				}
//				else{
//					mc4delivery = getMarginalInsertionCosts(getActivity(tour,j-1), getActivity(tour,j), delivery);
//				}
//				totalMarginalCost = mc4pickup + mc4delivery;
//				if(totalMarginalCost < bestMarginalCost){
//					bestMarginalCost = totalMarginalCost;
//					pickupInsertionIndex = i;
//					deliveryInsertionIndex = j;
//				}
//				if(getActivity(tour,j) instanceof JobActivity){
//					currentLoad += ((JobActivity)getActivity(tour,j)).getCapacityDemand();
//				}
//				if(vehicleCapExceeded(vehicle, currentLoad)){
//					break;
//				}
//			}
//		}
//		return new InsertionData(bestMarginalCost, pickupInsertionIndex, deliveryInsertionIndex);
//	}
//	
//	private boolean vehicleCapExceeded(Vehicle vehicle, int currentLoad) {
//		return currentLoad > vehicle.getCapacity();
//	}
//
//	private double getMarginalInsertionCosts(TourActivity prevAct, TourActivity nextAct, TourActivity newAct) {
//		double earliestDepTimeFromPrevAct = TourUtils.getEarliestDepTimeFromAct(prevAct);
//		double tt_prevAct2newAct = getTravelTime(prevAct.getLocationId(), newAct.getLocationId(), earliestDepTimeFromPrevAct);
//		
//		double earliestArrTimeAtNewAct = earliestDepTimeFromPrevAct + tt_prevAct2newAct;
//		double earliestOperationStartTimeAtNewAct = TourUtils.getEarliestOperationStartTime(earliestArrTimeAtNewAct, newAct);
//		
//		double earliestDepTimeFromNewAct = earliestOperationStartTimeAtNewAct + newAct.getOperationTime();
//		double tt_newAct2nextAct = getTravelTime(newAct.getLocationId(),nextAct.getLocationId(),earliestDepTimeFromNewAct);
//		
//		double earliestArrTimeAtNextAct = earliestDepTimeFromNewAct + tt_newAct2nextAct;
//		double earliestOperationStartTimeAtNextAct = TourUtils.getEarliestOperationStartTime(earliestArrTimeAtNextAct, nextAct);
//		
//		double marginalCost = 
//			getGeneralizedCosts(prevAct.getLocationId(), newAct.getLocationId(), earliestDepTimeFromPrevAct) +
//			getGeneralizedCosts(newAct.getLocationId(), nextAct.getLocationId(), earliestDepTimeFromNewAct) -
//			getGeneralizedCosts(prevAct.getLocationId(), nextAct.getLocationId(), earliestDepTimeFromPrevAct);
//		
//		//preCheck whether time-constraints are met
//		if(earliestOperationStartTimeAtNewAct > newAct.getLatestOperationStartTime() || earliestOperationStartTimeAtNextAct > nextAct.getLatestOperationStartTime()){
//			return Double.MAX_VALUE;
//		}
//				
//		double latestOperationStartTimeAtNewAct = nextAct.getLatestOperationStartTime() - newAct.getOperationTime() - 
//			getBackwardTravelTime(newAct.getLocationId(),nextAct.getLocationId(),nextAct.getLatestOperationStartTime());
//		
//		double latestOperationStartTimeAtPrevAct = newAct.getLatestOperationStartTime() - prevAct.getOperationTime() - 
//			getBackwardTravelTime(prevAct.getLocationId(),newAct.getLocationId(), newAct.getLatestOperationStartTime());
//		
//		if(latestOperationStartTimeAtNewAct < newAct.getEarliestOperationStartTime() || latestOperationStartTimeAtPrevAct < prevAct.getEarliestOperationStartTime()){
//			return Double.MAX_VALUE;
//		}
//		return marginalCost;
//	}
//	
//	private double getBackwardTravelTime(String fromId, String toId, double arrTime) {
//		return costs.getBackwardTransportTime(fromId, toId, arrTime);
//	}
//
//
	private Delivery createDelivery(Shipment shipment) {
		return new Delivery(shipment);
	}

	private Pickup createPickup(Shipment shipment) {
		return new Pickup(shipment);
	}
//
//	private double getTravelTime(String fromId, String toId, double time) {
//		return costs.getTransportTime(fromId, toId, time);
//	}
//
//	private double getGeneralizedCosts(String fromId, String toId, double time) {
//		return costs.getTransportCost(fromId, toId, time);
//	}
//
//	private TourActivity getActivity(Tour tour, int i) {
//		return tour.getActivities().get(i);
//	}


}
