package org.matsim.contrib.freight.vrp.utils.matsim2vrp;

import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleImpl;
import org.matsim.contrib.freight.vrp.basics.VehicleImpl.Type;

public class VRPVehicleAdapter implements
		org.matsim.contrib.freight.vrp.basics.Vehicle {

	private final CarrierVehicle carrierVehicle;

	private final Type type;

	public VRPVehicleAdapter(CarrierVehicle carrierVehicle) {
		super();
		this.carrierVehicle = carrierVehicle;
		type = createType(carrierVehicle);
	}

	private final Type createType(CarrierVehicle carrierVehicle) {
		Type type = VehicleImpl.getFactory().createType(
				carrierVehicle.getVehicleType().getId().toString(),
				carrierVehicle.getCapacity(),
				VehicleImpl.getFactory().createVehicleCostParams(
						carrierVehicle.getVehicleType()
								.getVehicleCostInformation().fix,
						carrierVehicle.getVehicleType()
								.getVehicleCostInformation().perTimeUnit,
						carrierVehicle.getVehicleType()
								.getVehicleCostInformation().perDistanceUnit));
		return type;
	}

	@Override
	public double getEarliestDeparture() {
		return carrierVehicle.getEarliestStartTime();
	}

	@Override
	public double getLatestArrival() {
		return carrierVehicle.getLatestEndTime();
	}

	@Override
	public String getLocationId() {
		return carrierVehicle.getLocation().toString();
	}

	@Override
	public Type getType() {
		return type;
	}

	public CarrierVehicle getCarrierVehicle() {
		return carrierVehicle;
	}

	@Override
	public String getId() {
		return carrierVehicle.getVehicleId().toString();
	}

	@Override
	public int getCapacity() {
		return carrierVehicle.getCapacity();
	}

}
