package org.matsim.contrib.freight.vrp.basics;


public class Delivery implements TourActivity, JobActivity{
	
	private Shipment shipment;

	private double practical_earliestArrivalTime;
	
	private double practical_latestArrivalTime;

	private int currentLoad;
	
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
	public double getOperationTime() {
		return shipment.getDeliveryServiceTime();
	}

	public String getLocationId() {
		return shipment.getToId();
	}
	
	@Override
	public void setEarliestOperationStartTime(double early) {
		practical_earliestArrivalTime = early;
	}

	@Override
	public double getEarliestOperationStartTime() {
		return practical_earliestArrivalTime;
	}

	@Override
	public double getLatestOperationStartTime() {
		return practical_latestArrivalTime;
	}

	@Override
	public void setLatestOperationStartTime(double late) {
		practical_latestArrivalTime = late;
	}

	@Override
	public Job getJob() {
		return shipment;
	}

	@Override
	public int getCurrentLoad() {
		return this.currentLoad;
	}

	@Override
	public void setCurrentLoad(int load) {
		this.currentLoad = load;
		
	}

	


}
