package org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

/**
 * Default implementation of a {{@link #VehicleHandler()}. It always allows
 * vehicles to arrive at a link.
 * 
 * @author Sebastian Hörl <sebastian.hoerl@ivt.baug.ethz.ch>
 */
public class DefaultVehicleHandler implements VehicleHandler {
	@Override
	public void handleVehicleDeparture(QVehicle vehicle, Link link) {

	}

	@Override
	public boolean handleVehicleArrival(QVehicle vehicle, Link link) {
		return true;
	}

	@Override
	public void handleInitialVehicleArrival(QVehicle vehicle, Link link) {

	}
}
