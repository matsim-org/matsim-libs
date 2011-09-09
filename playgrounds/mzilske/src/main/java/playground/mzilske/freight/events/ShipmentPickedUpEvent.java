package playground.mzilske.freight.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;

import playground.mzilske.freight.Shipment;

public class ShipmentPickedUpEvent extends CarrierEventImpl implements Event {

	private double time;
	
	private Shipment shipment;
	
	private Id driverId;
	
	public ShipmentPickedUpEvent(Id carrierId, Id driverId, Shipment shipment, double time) {
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
