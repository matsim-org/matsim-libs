package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VrpTourBuilder;
import org.matsim.contrib.freight.vrp.constraints.Constraints;
import org.matsim.core.utils.misc.Counter;

public class DistributionTourFactory implements TourFactory{
	
	private static Logger logger = Logger.getLogger(DistributionTourFactory.class);
	
	private Costs costs;
	
	private Constraints constraints;

	private TourStatusProcessor tourActivityUpdater;
	
	private Counter buildTourCounter;

	public DistributionTourFactory(Costs costs, Constraints constraints, TourStatusProcessor tourActivityUpdater) {
		super();
		this.costs = costs;
		this.constraints = constraints;
		this.tourActivityUpdater = tourActivityUpdater;
		buildTourCounter = new Counter("nOfTourBuilts ");
	}
	
	public Tour createTour(Vehicle vehicle, Tour oldTour, Job job, double bestKnownPrice){
		Tour newTour = null;
		newTour = buildTourWithNewShipment(vehicle,oldTour,(Shipment)job, bestKnownPrice);
		return newTour;
	}
	
	/*
	 * here only one tour-activity sequence is possible: pickup,pickup,...,delivery,delivery,..., i.e. a sequence of pickups is followed 
	 * by a sequence of deliveries.
	 * 
	 */
	private Tour buildTourWithNewShipment(Vehicle vehicle, Tour tour, Shipment shipment, double bestKnownPrice) {
			Delivery deliveryAct = createDelivery(shipment);
			Double bestMarginalCost = bestKnownPrice;
			Tour bestTour = null;
			int tourSize = tour.getActivities().size();
			int startIndexOfDeliveries = tourSize/2;
			int indexOfLastPickupAct = startIndexOfDeliveries-1;
			//preCheck whether capacity is sufficient
			if(tour.getActivities().get(indexOfLastPickupAct).getCurrentLoad() + shipment.getSize() > vehicle.getCapacity()){
				return null;
			}
			for(int i=startIndexOfDeliveries;i<tourSize;i++){
				TourActivity prevAct = getActivity(tour,i-1);
				TourActivity nextAct = getActivity(tour,i);
				double marginalCost = getMarginalInsertionCosts(prevAct, nextAct, deliveryAct); 
				if(marginalCost < bestMarginalCost){
					Tour newTour = buildTour(tour,shipment,i);
					if(this.constraints.judge(newTour,vehicle)){
						bestMarginalCost = marginalCost;
						bestTour = newTour;
					}
				}			
			}
			if(bestTour != null){
				return bestTour;
			}
			return null;
		}

	private double getMarginalInsertionCosts(TourActivity prevAct, TourActivity nextAct, TourActivity newAct) {
		double earliestDepTimeAtPrevAct = prevAct.getEarliestArrTime() + prevAct.getServiceTime();
		double tt_prevAct2newAct = getTravelTime(prevAct.getLocationId(), newAct.getLocationId(), earliestDepTimeAtPrevAct);
		
		double earliestArrTimeAtNewAct = earliestDepTimeAtPrevAct + tt_prevAct2newAct;
		double earliestDepTimeAtNewAct = earliestArrTimeAtNewAct + newAct.getServiceTime();
		double tt_newAct2nextAct = getTravelTime(newAct.getLocationId(),nextAct.getLocationId(),earliestDepTimeAtNewAct);
		double earliestArrTimeAtNextAct = earliestDepTimeAtNewAct + tt_newAct2nextAct;
		
		double marginalCost = 
			getGeneralizedCosts(prevAct.getLocationId(), newAct.getLocationId(), earliestDepTimeAtPrevAct) +
			getGeneralizedCosts(newAct.getLocationId(), nextAct.getLocationId(), earliestDepTimeAtNewAct) -
			getGeneralizedCosts(prevAct.getLocationId(), nextAct.getLocationId(), earliestDepTimeAtPrevAct);
		//preCheck whether time-constraints are met
		if(earliestArrTimeAtNewAct > newAct.getLatestArrTime() || earliestArrTimeAtNextAct > nextAct.getLatestArrTime()){
			return Double.MAX_VALUE;
		}
				
		double latestArrTimeAtNewAct = nextAct.getLatestArrTime() - newAct.getServiceTime() - 
			getBackwardTravelTime(newAct.getLocationId(),nextAct.getLocationId(),nextAct.getLatestArrTime());
		
		double latestArrTimeAtPrevAct = newAct.getLatestArrTime() - prevAct.getServiceTime() - 
			getBackwardTravelTime(prevAct.getLocationId(),newAct.getLocationId(), newAct.getLatestArrTime());
		
		if(latestArrTimeAtNewAct < newAct.getEarliestArrTime() || latestArrTimeAtPrevAct < prevAct.getEarliestArrTime()){
			return Double.MAX_VALUE;
		}
		return marginalCost;
	}

	private double getBackwardTravelTime(String fromId, String toId, double arrTime) {
		return costs.getBackwardTransportTime(fromId, toId, arrTime);
	}

	private Tour buildTour(Tour tour, Shipment shipment, int insertionIndexForDelivery) {
//		buildTourCounter.incCounter();
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		tourBuilder.scheduleActivity(tour.getActivities().getFirst());
		tourBuilder.schedulePickup(shipment);
		for(int i=1;i<tour.getActivities().size();i++){
			if(i == insertionIndexForDelivery){
				tourBuilder.scheduleDelivery(shipment);
			}
			tourBuilder.scheduleActivity(tour.getActivities().get(i));
		}
		Tour newTour = tourBuilder.build();
		tourActivityUpdater.process(newTour);
		return newTour;
	}

	private Delivery createDelivery(Shipment shipment) {
		return new Delivery(shipment);
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
