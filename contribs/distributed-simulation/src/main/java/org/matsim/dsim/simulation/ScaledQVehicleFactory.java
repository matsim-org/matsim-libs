package org.matsim.dsim.simulation;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.Vehicle;

/**
 * QVehicle factory which creates {@link QVehicleImpl} instances and sclaes the pce of the vehicle according to
 * config:qsim.flowCapacityFactor.
 */
public class ScaledQVehicleFactory implements QVehicleFactory {

	private final Config config;

	public ScaledQVehicleFactory(Config config) {this.config = config;}

	@Override
	public QVehicle createQVehicle(Vehicle vehicle) {
		return new QVehicleImpl(vehicle, 1 / config.qsim().getFlowCapFactor());
	}
}
