package org.matsim.contrib.freight.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;

/**
 * This informs the world that a shipment has been picked up.
 * 
 * @author sschroeder
 *
 */
public class ShipmentPickedUpEvent extends Event {

	private CarrierShipment shipment;
	
	private Id<Person> driverId;
	
	public ShipmentPickedUpEvent(Id<Carrier> carrierId, Id<Person> driverId, CarrierShipment shipment, double time) {
		super(time);
		this.shipment = shipment;
		this.driverId = driverId;
	}

	public Id<Person> getDriverId() {
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
