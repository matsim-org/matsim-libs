package org.matsim.contrib.dvrp.fleet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;

/**
 * Event fired when a vehicle is added to a DVRP fleet
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class VehicleAddedEvent extends AbstractFleetEvent {
	static public final String EVENT_NAME = "dvrp vehicle added";
	
	static public final String CAPACITY_ATTRIBUTE = "capacity";
	
	private final int capacity;

	protected VehicleAddedEvent(double time, String mode, Id<DvrpVehicle> vehicleId, int capacity) {
		super(time, mode, vehicleId);
		this.capacity = capacity;
	}
	
	public int getCapacity() {
		return capacity;
	}

	@Override
	public String getEventType() {
		return EVENT_NAME;
	}

	static public VehicleAddedEvent convert(GenericEvent event) {
		return new VehicleAddedEvent(event.getTime(), //
				event.getAttributes().get(MODE_ATTRIBUTE), //
				Id.create(event.getAttributes().get(VEHICLE_ATTRIBUTE), DvrpVehicle.class), //
				Integer.parseInt(event.getAttributes().get(CAPACITY_ATTRIBUTE)));
	}
}
