package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.basics.Constraints;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VrpTourBuilder;

public class DistributionTourFactory implements TourFactory{
	
	private static Logger logger = Logger.getLogger(DistributionTourFactory.class);
	
	private Costs costs;
	
	private Constraints constraints;

	private TourStatusProcessor tourActivityUpdater;

	public DistributionTourFactory(Costs costs, Constraints constraints, TourStatusProcessor tourActivityUpdater) {
		super();
		this.costs = costs;
		this.constraints = constraints;
		this.tourActivityUpdater = tourActivityUpdater;
	}
	
	public Tour createTour(Vehicle vehicle, Tour oldTour, Job job, double bestKnownPrice){
		Tour newTour = null;
		newTour = buildTourWithNewShipment(vehicle,oldTour,(Shipment)job, bestKnownPrice);
		return newTour;
	}
	
	private Tour buildTourWithNewShipment(Vehicle vehicle, Tour tour, Shipment shipment, double bestKnownPrice) {
			Delivery delivery = createDelivery(shipment);
			Double bestMarginalCost = bestKnownPrice;
			Tour bestTour = null;
			int tourSize = tour.getActivities().size();
			for(int i=tourSize/2;i<tourSize;i++){
				double marginalCost = getMarginalInsertionCosts(getActivity(tour,i-1), getActivity(tour,i), delivery);
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

	private double getMarginalInsertionCosts(TourActivity act_i, TourActivity act_j, TourActivity newAct) {
		double tt_acti2newAct = getTravelTime(act_i.getLocationId(), newAct.getLocationId(), 
				act_i.getEarliestArrTime() + act_i.getServiceTime());
		double earliestArrTimeAtNewAct = act_i.getEarliestArrTime() + act_i.getServiceTime() + tt_acti2newAct;
	 
		double marginalCost = getGeneralizedCosts(act_i.getLocationId(), newAct.getLocationId(), 
				act_i.getEarliestArrTime()+act_i.getServiceTime()) +
			getGeneralizedCosts(newAct.getLocationId(), act_j.getLocationId(), earliestArrTimeAtNewAct + newAct.getServiceTime()) -
			getGeneralizedCosts(act_i.getLocationId(), act_j.getLocationId(), act_i.getEarliestArrTime() + act_i.getServiceTime());
		
		return marginalCost;
	}

	private Tour buildTour(Tour tour, Shipment shipment, int insertionIndexForDelivery) {
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
		return costs.getGeneralizedCost(fromId, toId, time);
	}

	private TourActivity getActivity(Tour tour, int i) {
		return tour.getActivities().get(i);
	}


}
