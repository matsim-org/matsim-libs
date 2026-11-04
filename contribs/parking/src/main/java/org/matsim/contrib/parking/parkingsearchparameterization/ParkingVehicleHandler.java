package org.matsim.contrib.parking.parkingsearchparameterization;

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

/**
 * Lets arriving vehicles search for parking if the network mode of their vehicle type is one of the given modes.
 */
public class ParkingVehicleHandler implements VehicleHandler, TransitDriverStartsEventHandler {
	private final Set<String> modes;
	private final Set<Id<Vehicle>> knownPtVehicles = new HashSet<>();

	/**
	 * Only cars search for parking.
	 */
	public ParkingVehicleHandler() {
		this(Set.of(TransportMode.car));
	}

	/**
	 * @param modes network modes of the vehicles that search for parking on arrival
	 */
	public ParkingVehicleHandler(Set<String> modes) {
		this.modes = Set.copyOf(modes);
	}

	@Override
	public void handleVehicleDeparture(QVehicle vehicle, Link link) {

	}

	@Override
	public VehicleArrival handleVehicleArrival(QVehicle vehicle, Link link) {
		if (!modes.contains(vehicle.getVehicle().getType().getNetworkMode())) {
			// If vehicle is not of a parking mode, do not park it
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
