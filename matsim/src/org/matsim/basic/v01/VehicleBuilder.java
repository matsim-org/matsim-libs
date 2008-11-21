package org.matsim.basic.v01;

import org.matsim.basic.v01.BasicEngineInformation.FuelType;

public interface VehicleBuilder {

	public BasicVehicleType createVehicleType(String type);

	public BasicVehicleCapacity createVehicleCapacity();

	public BasicFreightCapacity createFreigthCapacity();

	public BasicEngineInformation createEngineInformation(FuelType fuelType,
			double gasConsumption);

	public BasicVehicle createVehicle(Id id, String type);

}