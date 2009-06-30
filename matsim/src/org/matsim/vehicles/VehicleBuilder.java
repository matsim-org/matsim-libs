package org.matsim.vehicles;

import org.matsim.api.basic.v01.Id;
import org.matsim.vehicles.BasicEngineInformation.FuelType;

public interface VehicleBuilder {

	public BasicVehicleType createVehicleType(Id type);

	public BasicVehicleCapacity createVehicleCapacity();

	public BasicFreightCapacity createFreigthCapacity();

	public BasicEngineInformation createEngineInformation(FuelType fuelType,
			double gasConsumption);

	public BasicVehicle createVehicle(Id id, BasicVehicleType type);

}