/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package playground.boescpa.lib.tools.fileMerging;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.vehicles.*;

/**
 * Merges two vehicle collections to a new one and returns this new one.
 *
 * @author boescpa
 */
public class VehicleMerger {
	private static Logger log = Logger.getLogger(VehicleMerger.class);

	private static VehiclesFactory factory;
	private static Vehicles vehicles;

	public static Vehicles mergeVehicles(Vehicles vehiclesA, Vehicles vehiclesB) {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		vehicles = scenario.getVehicles();
		factory = vehicles.getFactory();

		log.info("     Merging vehicles...");

		// vehicle types:
		Counter counter = new Counter("vehicle types # ");
		for (VehicleType vehicleType : vehiclesA.getVehicleTypes().values()) {
			vehicles.addVehicleType(copyVehicleType(vehicleType));
			counter.incCounter();
		}
		for (VehicleType vehicleType : vehiclesB.getVehicleTypes().values()) {
			vehicles.addVehicleType(copyVehicleType(vehicleType));
			counter.incCounter();
		}
		counter.printCounter();

		// vehicles:
		for (Vehicle vehicle :vehiclesA.getVehicles().values()) {
			vehicles.addVehicle(copyVehicle(vehicle));
		}
		for (Vehicle vehicle :vehiclesB.getVehicles().values()) {
			vehicles.addVehicle(copyVehicle(vehicle));
		}

		log.info("     Merging vehicles... done.");

		return vehicles;
	}

	private static Vehicle copyVehicle(Vehicle vehicle) {
		final VehicleType type = vehicles.getVehicleTypes().get(vehicle.getType().getId());
		final Vehicle newVehicle = factory.createVehicle(
				Id.create(vehicle.getId().toString(), Vehicle.class), type);
		return newVehicle;
	}

	private static VehicleType copyVehicleType(VehicleType vehicleType) {
		final VehicleType newVehicleType = factory.createVehicleType(
				Id.create(vehicleType.getId().toString(), VehicleType.class));
		newVehicleType.setDescription(vehicleType.getDescription());
		newVehicleType.setDoorOperationMode((vehicleType.getDoorOperationMode() != null) ?
				getDoorOperationMode(vehicleType.getDoorOperationMode()) : null);
		newVehicleType.setPcuEquivalents(vehicleType.getPcuEquivalents());
		newVehicleType.setLength(vehicleType.getLength());
		newVehicleType.setWidth(vehicleType.getWidth());
		newVehicleType.setMaximumVelocity(vehicleType.getMaximumVelocity());
		newVehicleType.setAccessTime(vehicleType.getAccessTime());
		newVehicleType.setEgressTime(vehicleType.getEgressTime());
		newVehicleType.setCapacity(copyVehicleCapacity(vehicleType.getCapacity()));
		newVehicleType.setEngineInformation(
				(vehicleType.getEngineInformation() != null) ? copyEngineInformation(vehicleType.getEngineInformation()) : null);
		return newVehicleType;
	}

	private static VehicleType.DoorOperationMode getDoorOperationMode(VehicleType.DoorOperationMode doorOperationMode) {
		return (doorOperationMode.equals(VehicleType.DoorOperationMode.parallel)) ?
					VehicleType.DoorOperationMode.parallel :
					VehicleType.DoorOperationMode.serial;
	}

	private static EngineInformation copyEngineInformation(EngineInformation engineInformation) {
		return factory.createEngineInformation(engineInformation.getFuelType(), engineInformation.getGasConsumption());
	}

	private static VehicleCapacity copyVehicleCapacity(VehicleCapacity capacity) {
		final VehicleCapacity newCapacity = factory.createVehicleCapacity();
		if (capacity.getFreightCapacity() != null) {
			final FreightCapacity newFreightCapacity = factory.createFreigthCapacity();
			newFreightCapacity.setVolume(capacity.getFreightCapacity().getVolume());
			newCapacity.setFreightCapacity(newFreightCapacity);
		}
		newCapacity.setSeats(capacity.getSeats());
		newCapacity.setStandingRoom(capacity.getStandingRoom());
		return newCapacity;
	}
}
