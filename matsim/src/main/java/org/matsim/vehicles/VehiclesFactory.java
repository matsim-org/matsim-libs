package org.matsim.vehicles;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.vehicles.EngineInformation.FuelType;

public interface VehiclesFactory extends MatsimFactory {

	public VehicleType createVehicleType(Id type);

	public VehicleCapacity createVehicleCapacity();

	public FreightCapacity createFreigthCapacity();

	public EngineInformation createEngineInformation(FuelType fuelType,
			double gasConsumption);

	public Vehicle createVehicle(Id id, VehicleType type);

}