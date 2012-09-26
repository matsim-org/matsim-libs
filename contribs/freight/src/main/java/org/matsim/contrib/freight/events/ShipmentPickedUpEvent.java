package org.matsim.contrib.freight.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.core.api.experimental.events.Event;

public class ShipmentPickedUpEvent implements Event {

	private final Id carrierId;

	private final Id driverId;

	private final CarrierShipment shipment;

	private final double time;

	public ShipmentPickedUpEvent(Id carrierId, Id driverId,
			CarrierShipment shipment, double time) {
		this.carrierId = carrierId;
		this.time = time;
		this.shipment = shipment;
		this.driverId = driverId;
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
	public double getTime() {
		return time;
	}

	@Override
	public Map<String, String> getAttributes() {
		return null;
	}

}
