package org.matsim.contrib.freight.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Shipment;
import org.matsim.core.api.experimental.events.Event;


public class ShipmentDeliveredEvent extends CarrierEventImpl implements Event {

	private double time;
	
	private Shipment shipment;
	
	private Id driverId;
	
	public ShipmentDeliveredEvent(Id carrierId, Id driverId, Shipment shipment, double time) {
		super(carrierId);
		this.time = time;
		this.shipment = shipment;
		this.driverId = driverId;
	}

	public Id getDriverId() {
		return driverId;
	}

	public Shipment getShipment() {
		return shipment;
	}

	@Override
	public double getTime() {
		return time;
	}

	@Override
	public Map<String, String> getAttributes() {
		return null;
	}

}
