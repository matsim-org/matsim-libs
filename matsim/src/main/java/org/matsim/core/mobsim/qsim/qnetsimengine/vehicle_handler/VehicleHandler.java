package org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

public interface VehicleHandler {
	void handleVehicleDeparture(QVehicle vehicle, Link link);

	boolean handleVehicleArrival(QVehicle vehicle, Link link);

	void handleInitialVehicleArrival(QVehicle vehicle, Link link);
}
