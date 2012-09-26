package org.matsim.contrib.freight.vrp.utils.matsim2vrp;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

public class MatsimVehicleAdapter implements Vehicle {

	private CarrierVehicle carrierVehicle;

	public MatsimVehicleAdapter(VRPVehicleAdapter vrpVehicleAdapter) {
		super();
		this.carrierVehicle = vrpVehicleAdapter.getCarrierVehicle();
	}

	public MatsimVehicleAdapter(CarrierVehicle vehicle) {
		this.carrierVehicle = vehicle;
	}

	@Override
	public Id getId() {
		return carrierVehicle.getVehicleId();
	}

	@Override
	public VehicleType getType() {
		return carrierVehicle.getVehicleType();
	}

	public CarrierVehicle getCarrierVehicle() {
		return carrierVehicle;
	}

}
