package org.matsim.vehicles;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.vehicles.BasicEngineInformation.FuelType;

public interface VehiclesFactory extends MatsimFactory {

	public BasicVehicleType createVehicleType(Id type);

	public BasicVehicleCapacity createVehicleCapacity();

	public BasicFreightCapacity createFreigthCapacity();

	public BasicEngineInformation createEngineInformation(FuelType fuelType,
			double gasConsumption);

	public BasicVehicle createVehicle(Id id, BasicVehicleType type);

}