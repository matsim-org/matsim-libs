package org.matsim.contrib.freight.events;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.core.api.experimental.events.Event;

public class ShipmentDeliveredEvent extends Event {

	private CarrierShipment shipment;
	private Id carrierId;
	private Id driverId;
	
	public ShipmentDeliveredEvent(Id carrierId, Id driverId, CarrierShipment shipment, double time) {
		super(time);
		this.shipment = shipment;
		this.driverId = driverId;
		this.carrierId = carrierId;
	}

	public Id getCarrierId() {
		return carrierId;
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
