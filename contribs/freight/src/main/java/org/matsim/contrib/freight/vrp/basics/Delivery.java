package org.matsim.contrib.freight.vrp.basics;


public class Delivery implements TourActivity, JobActivity{
	
	private Shipment shipment;

	private double practical_earliestArrivalTime;
	
	private double practical_latestArrivalTime;
	
	public Delivery(Shipment shipment) {
		super();
		this.shipment = shipment;
		practical_earliestArrivalTime = shipment.getDeliveryTW().getStart();
		practical_latestArrivalTime = shipment.getDeliveryTW().getEnd();
	}
	
	@Override
	public String getType() {
		return "Delivery";
	}

	public int getCapacityDemand(){
		return -1*shipment.getSize();
	}

	@Override
	public double getServiceTime() {
		return shipment.getDeliveryServiceTime();
	}

	public String getLocationId() {
		return shipment.getToId();
	}
	
	@Override
	public void setEarliestArrTime(double early) {
		practical_earliestArrivalTime = early;
	}

	@Override
	public double getEarliestArrTime() {
		return practical_earliestArrivalTime;
	}

	@Override
	public double getLatestArrTime() {
		return practical_latestArrivalTime;
	}

	@Override
	public void setLatestArrTime(double late) {
		practical_latestArrivalTime = late;
	}

	@Override
	public Job getJob() {
		return shipment;
	}


}
