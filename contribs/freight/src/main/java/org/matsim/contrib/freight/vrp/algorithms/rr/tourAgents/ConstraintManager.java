package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public class ConstraintManager {
	
	private Vehicle vehicle;
	
	private Tour tour;
	
	private Pickup pickup;
	
	private Delivery delivery;
	
	private boolean isContinue = false;
	
	private boolean isBreak = false;

	boolean fine = true;

	private int outerLoadRecorder = 0;
	
	private int currentLoad = 0;
	
	private int currentPickupIndex = 0;
	
	public ConstraintManager(Vehicle vehicle, Tour tour, Pickup pickup, Delivery delivery) {
		this.vehicle = vehicle;
		this.tour = tour;
		this.pickup = pickup;
		this.delivery = delivery;
	}

		
	public void reset(){
		fine = true;
	}

	private boolean feasable(int pickupIndex) {
		TourActivity prevAct = getActivity(pickupIndex-1);
		TourActivity nextAct = getActivity(pickupIndex);
		if(pickup.getLatestOperationStartTime() < prevAct.getEarliestOperationStartTime()){
			return false;
		}
		else if(pickup.getEarliestOperationStartTime() > nextAct.getLatestOperationStartTime()){
			return false;
		}
		return true;		
	}


	public void informInsertionStarts() {
		if(tour.costs.totalLoad + pickup.getCapacityDemand() > vehicle.getCapacity()){
			isBreak = true;
		}
		
	}

	public void informPickupIterationStarts(int pickupIndex) {
		resetContinueAndBreak();
		recordOuterLoad(pickupIndex);
		currentPickupIndex = pickupIndex;
		if(outerLoadRecorder + pickup.getCapacityDemand() > vehicle.getCapacity()){
			isContinue = true;
		}
		if(!feasable(pickupIndex)){
			isContinue = true;
		}
		if(!sequenceFeasable(pickupIndex)){
			isBreak = true;
		}
		currentLoad = outerLoadRecorder + pickup.getCapacityDemand();
	}

	private boolean sequenceFeasable(int pickupIndex) {
		if(getActivity(pickupIndex-1) instanceof Delivery){
			return false;
		}
		return true;
	}


	private void resetContinueAndBreak() {
		isContinue = false;
		isBreak = false;
	}


	private void recordOuterLoad(int pickupIndex) {
		if(getActivity(pickupIndex-1) instanceof JobActivity){
			outerLoadRecorder += ((JobActivity)getActivity(pickupIndex-1)).getCapacityDemand();
		}		
	}

	private TourActivity getActivity(int i) {
		return tour.getActivities().get(i);
	}

	public void informDeliveryIterationStarts(int deliveryIndex) {
		resetContinueAndBreak();
		if(deliveryIndex != currentPickupIndex){
			if(getActivity(deliveryIndex-1) instanceof JobActivity){
				currentLoad += ((JobActivity)getActivity(deliveryIndex-1)).getCapacityDemand();
			}
		}
		if(currentLoad > vehicle.getCapacity()){
			isBreak = true;
		}
		if(!delSequenceFeasable(deliveryIndex)){
			isContinue = true;
		}
		
	}

	private boolean delSequenceFeasable(int deliveryIndex) {
		if(getActivity(deliveryIndex) instanceof Pickup){
			return false;
		}
		return true;
	}


	public void informDeliveryIterationEnds(int deliveryIndex) {
		// TODO Auto-generated method stub
		
	}

	public void informPickupIterationEnds(int pickupIndex) {
		// TODO Auto-generated method stub
		
	}

	public void informInsertionEnds() {
		// TODO Auto-generated method stub
		
	}

	public boolean isContinue() {
		return isContinue;
	}

	public boolean isBreak() {
		return isBreak;
	}

}
