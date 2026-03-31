package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;
import org.matsim.vehicles.Vehicle;

import java.util.HashSet;
import java.util.Set;

public class ParkingVehicleHandler implements VehicleHandler, TransitDriverStartsEventHandler {
	private final Set<Id<Vehicle>> knownPtVehicles = new HashSet<>();

	@Override
	public void handleVehicleDeparture(QVehicle vehicle, Link link) {

	}

	@Override
	public VehicleArrival handleVehicleArrival(QVehicle vehicle, Link link) {
		if (!vehicle.getVehicle().getType().getNetworkMode().equals(TransportMode.car)) {
			// If vehicle is no car, do not park it
			return VehicleArrival.ALLOWED;
		}

		if (knownPtVehicles.contains(vehicle.getId())) {
			// if vehicle is pt vehicle, do not park it
			return VehicleArrival.ALLOWED;
		}

		// otherwise force parking
		return VehicleArrival.PARKING;
	}

	@Override
	public void handleInitialVehicleArrival(QVehicle vehicle, Link link) {

	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		knownPtVehicles.add(event.getVehicleId());
	}
}
