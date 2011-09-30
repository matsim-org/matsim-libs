package playground.mzilske.freight.events;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.Shipment;

public class Service {
	
	private Id from;
	
	private Id to;
	
	private int size;
	
	private double startPickup;
	
	private double endPickup;
	
	private double startDelivery;
	
	private double endDelivery;

	public Service(Shipment shipment){
		this.from = shipment.getFrom();
		this.to = shipment.getTo();
		this.size = shipment.getSize();
		this.startPickup = shipment.getPickupTimeWindow().getStart();
		this.endPickup = shipment.getPickupTimeWindow().getEnd();
		this.startDelivery = shipment.getDeliveryTimeWindow().getStart();
		this.endDelivery = shipment.getDeliveryTimeWindow().getEnd();
	}
	
	public Service(Id from, Id to, int size, double startPickup,double endPickup, double startDelivery, double endDelivery) {
		super();
		this.from = from;
		this.to = to;
		this.size = size;
		this.startPickup = startPickup;
		this.endPickup = endPickup;
		this.startDelivery = startDelivery;
		this.endDelivery = endDelivery;
	}

	public Id getFrom() {
		return from;
	}

	public Id getTo() {
		return to;
	}

	public int getSize() {
		return size;
	}

	public double getStartPickup() {
		return startPickup;
	}

	public double getEndPickup() {
		return endPickup;
	}

	public double getStartDelivery() {
		return startDelivery;
	}

	public double getEndDelivery() {
		return endDelivery;
	}
	
	

}
