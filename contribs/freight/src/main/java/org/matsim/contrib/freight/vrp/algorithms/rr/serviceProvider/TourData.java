package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

class TourData {
	
	public final double penalty;
	
	public Integer pickupInsertionIndex;
	
	public Integer deliveryInsertionIndex;
	
	TourData(double penalty, Integer pickupInsertionIndex, Integer deliveryInsertionIndex) {
		super();
		this.penalty = penalty;
		this.pickupInsertionIndex = pickupInsertionIndex;
		this.deliveryInsertionIndex = deliveryInsertionIndex;
	}
	
	public boolean tourFound(){
		return this.pickupInsertionIndex != null && this.deliveryInsertionIndex != null;
	}

}
