package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleTypeImpl;

public class CarrierVehicleType extends VehicleTypeImpl {

	public static class VehicleCostInformation {

		public final double fix;
		public final double perDistanceUnit;
		public final double perTimeUnit;

		public VehicleCostInformation(double fix, double perDistanceUnit,
				double perTimeUnit) {
			super();
			this.fix = fix;
			this.perDistanceUnit = perDistanceUnit;
			this.perTimeUnit = perTimeUnit;
		}

	}

	private VehicleCostInformation vehicleCostInformation = new VehicleCostInformation(
			0.0, 1.0, 0.0);

	public CarrierVehicleType(Id typeId) {
		super(typeId);
	}

	public void setVehicleCostParams(VehicleCostInformation vehicleCosts) {
		this.vehicleCostInformation = vehicleCosts;
	}

	public VehicleCostInformation getVehicleCostInformation() {
		return vehicleCostInformation;
	}

	// public void setVehicleCostParams(VehicleCostInformation
	// currentVehicleCosts);
	//
	// public VehicleCostInformation getVehicleCostParams();

}
