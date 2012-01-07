package org.matsim.contrib.freight.vrp.basics;


public class Pickup implements TourActivity, JobActivity{

	private Shipment shipment;
	
	private double practical_earliestArrivalTime;
	
	private double practical_latestArrivalTime;
	
	public Pickup(Shipment shipment) {
		super();
		this.shipment = shipment;
		practical_earliestArrivalTime = shipment.getPickupTW().getStart();
		practical_latestArrivalTime = shipment.getPickupTW().getEnd();
	}
	
	@Override
	public String getType() {
		return "Pickup";
	}

	public int getCapacityDemand(){
		return shipment.getSize();
	}

	@Override
	public double getServiceTime() {
		return shipment.getPickupServiceTime();
	}

	public String getLocationId() {
		return shipment.getFromId();
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
