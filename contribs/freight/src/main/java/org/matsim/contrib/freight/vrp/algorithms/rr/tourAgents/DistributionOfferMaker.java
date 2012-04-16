package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;


import java.util.Iterator;

import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.OfferData.MetaData;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.OfferData.Offer;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.constraints.Constraints;
import org.matsim.core.utils.misc.Counter;

/**
 * calculates best marginal insertion cost for the single depot distribution problem and returns an 
 * an data-object 'OfferData' with these costs and the corresponding insertion indices.
 */
public class DistributionOfferMaker implements OfferMaker{
	
	class InsertionData {
		double mc;
		Integer insertionIndex;
		public InsertionData(double mc, Integer insertionIndex) {
			super();
			this.mc = mc;
			this.insertionIndex = insertionIndex;
		}
	}
	
	private Costs costs;
	
	private Constraints constraints;

	private Counter buildTourCounter;

	public DistributionOfferMaker(Costs costs, Constraints constraints) {
		super();
		this.costs = costs;
		this.constraints = constraints;
		buildTourCounter = new Counter("nOfTourBuilts ");
	}

	
	public OfferData makeOffer(Vehicle vehicle, Tour tour, Job job, double bestKnownPrice){
		Shipment shipment = (Shipment)job;
		Delivery deliveryAct = new Delivery(shipment);
		//preCheck whether capacity is sufficient
		if(!preCheck(tour,shipment,vehicle)){
			OfferData data = new OfferData(new Offer(Double.MAX_VALUE),null);
			return data;
		}
		InsertionData insertionData = findBestInsertion(tour, deliveryAct, bestKnownPrice);
		if(insertionData.insertionIndex != null){
			OfferData data = new OfferData(new Offer(insertionData.mc),new MetaData(1,insertionData.insertionIndex));
			return data;
		}
		else{
			OfferData data = new OfferData(new Offer(Double.MAX_VALUE),null);
			return data;
		}
	}
	
	public InsertionData findBestInsertion(Tour tour, TourActivity deliveryAct, double bestKnownPrice){
		Double bestMarginalCost = bestKnownPrice;
		Integer insertionIndex = null;
		int tourSize = tour.getActivities().size();
		int indexOfFirstDelivery = Math.max(tourSize/2,1);
		Iterator<TourActivity> actIter = tour.getActivities().listIterator();
		TourActivity prevAct = actIter.next();
		while(actIter.hasNext()){
			TourActivity currAct = actIter.next();
			if(!anotherPreCheck(prevAct,currAct,deliveryAct)){
				prevAct = currAct;
				continue;
			}
			double mc = getMarginalInsertionCosts(prevAct, currAct, deliveryAct);
			if(mc < bestMarginalCost){
				bestMarginalCost = mc;
				insertionIndex = tour.getActivities().indexOf(currAct);
			}
			prevAct = currAct;
		}
		return new InsertionData(bestMarginalCost,insertionIndex);
	}
	
	private boolean anotherPreCheck(TourActivity prevAct, TourActivity currAct, TourActivity deliveryAct) {
		if(deliveryAct.getLatestOperationStartTime() < prevAct.getEarliestOperationStartTime()){
			return false;
		}
		if(deliveryAct.getEarliestOperationStartTime() > currAct.getLatestOperationStartTime()){
			return false;
		}
		return true;
	}


	private boolean preCheck(Tour tour, Shipment shipment, Vehicle vehicle) {
		if(tour.getTourStats().totalLoad + shipment.getSize() > vehicle.getCapacity()){
			return false;
		}
		return true;
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
