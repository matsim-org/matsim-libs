package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;


public class OfferData {
	
	static class MetaData {
		int pickupInsertionIndex;
		int deliveryInsertionIndex;
		public MetaData(int pickupInsertionIndex, int deliveryInsertionIndex) {
			super();
			this.pickupInsertionIndex = pickupInsertionIndex;
			this.deliveryInsertionIndex = deliveryInsertionIndex;
		}
		
	}
	
	static class Offer {
		double price;

		public Offer(double price) {
			super();
			this.price = price;
		}
		
	}
	
	public Offer offer;
	
	public MetaData metaData;

	public OfferData(Offer offer, MetaData metaData) {
		super();
		this.offer = offer;
		this.metaData = metaData;
	}
}
