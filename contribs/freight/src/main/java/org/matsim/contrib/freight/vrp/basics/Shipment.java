package org.matsim.contrib.freight.vrp.basics;

public class Shipment implements Job{
	
	private String fromId;
	
	private String toId;
	
	private int size;
	
	private double pickupServiceTime;
	
	private double deliveryServiceTime;
	
	private TimeWindow pickupTW;
	
	private TimeWindow deliveryTW;

	private String id;

	public Shipment(String id, String fromId, String toId, int size) {
		super();
		this.fromId = fromId;
		this.toId = toId;
		this.size = size;
		this.id = id;
	}

	public TimeWindow getPickupTW() {
		return pickupTW;
	}

	public void setPickupTW(TimeWindow pickupTW) {
		this.pickupTW = pickupTW;
	}

	public TimeWindow getDeliveryTW() {
		return deliveryTW;
	}

	public void setDeliveryServiceTime(double deliveryServiceTime) {
		this.deliveryServiceTime = deliveryServiceTime;
	}

	public void setDeliveryTW(TimeWindow deliveryTW) {
		this.deliveryTW = deliveryTW;
	}

	public String getFromId() {
		return fromId;
	}

	public void setPickupServiceTime(double pickupServiceTime) {
		this.pickupServiceTime = pickupServiceTime;
	}

	public double getPickupServiceTime() {
		return pickupServiceTime;
	}

	public double getDeliveryServiceTime() {
		return deliveryServiceTime;
	}

	public String getToId() {
		return toId;
	}

	public int getSize() {
		return size;
	}

	@Override
	public String getId() {
		return id;
	}
	
}
