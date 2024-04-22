package org.matsim.contrib.dvrp.fleet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;

/**
 * Event fired when a vehicle is removed from a DVRP fleet
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class VehicleRemovedEvent extends AbstractFleetEvent {
	static public final String EVENT_NAME = "dvrp vehicle removed";

	protected VehicleRemovedEvent(double time, String mode, Id<DvrpVehicle> vehicleId) {
		super(time, mode, vehicleId);
	}

	@Override
	public String getEventType() {
		return EVENT_NAME;
	}

	static public VehicleRemovedEvent convert(GenericEvent event) {
		return new VehicleRemovedEvent(event.getTime(), //
				event.getAttributes().get(MODE_ATTRIBUTE), //
				Id.create(event.getAttributes().get(VEHICLE_ATTRIBUTE), DvrpVehicle.class));
	}
}
