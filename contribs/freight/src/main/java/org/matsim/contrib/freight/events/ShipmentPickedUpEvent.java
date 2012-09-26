package org.matsim.contrib.freight.events;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.core.api.experimental.events.Event;

public class ShipmentPickedUpEvent extends Event {

	private CarrierShipment shipment;
	
	private Id driverId;
	
	public ShipmentPickedUpEvent(Id carrierId, Id driverId, CarrierShipment shipment, double time) {
		super(time);
		this.shipment = shipment;
		this.driverId = driverId;
	}

	public Id getDriverId() {
		return driverId;
	}

	public CarrierShipment getShipment() {
		return shipment;
	}

	@Override
	public String getEventType() {
		// TODO Auto-generated method stub
		return null;
	}

}
