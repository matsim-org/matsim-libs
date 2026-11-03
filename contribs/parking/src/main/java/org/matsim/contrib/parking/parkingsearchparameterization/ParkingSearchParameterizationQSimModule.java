package org.matsim.contrib.parking.parkingsearchparameterization;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import java.util.Set;

/**
 * The QSim components of the parking search parameterization: arrivals of vehicles of the given modes are turned into parking
 * searches, whose duration is calculated from the occupancy around the arrival link.
 * <p>
 * This module is installed by {@link ParkingSearchParameterizationModule}, which also provides the bindings it depends on
 * ({@link ParkingOccupancyObserver}, {@link ParkingSearchTimeFunction}, ...). It is therefore package-private, so that users
 * cannot add it on its own or a second time.
 */
class ParkingSearchParameterizationQSimModule extends AbstractQSimModule {
	private final Set<String> modes;

	ParkingSearchParameterizationQSimModule(Set<String> modes) {
		this.modes = modes;
	}

	@Override
	protected void configureQSim() {
		// configureQSim is called for every QSim, so each mobsim gets a new handler.
		addVehicleHandlerBinding().toInstance(new ParkingVehicleHandler(modes));
		addParkingSearchTimeCalculatorBinding().to(ParkingOccupancyObservingSearchTimeCalculator.class);
	}
}
