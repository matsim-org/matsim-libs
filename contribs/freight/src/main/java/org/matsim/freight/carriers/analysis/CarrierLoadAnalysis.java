/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.analysis;

import static org.matsim.freight.carriers.events.CarrierEventAttributes.ATTRIBUTE_CAPACITYDEMAND;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.events.CarrierShipmentDeliveryStartEvent;
import org.matsim.freight.carriers.events.CarrierShipmentPickupStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierShipmentDeliveryStartEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierShipmentPickupStartEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class CarrierLoadAnalysis implements CarrierShipmentPickupStartEventHandler, CarrierShipmentDeliveryStartEventHandler {

	private static final Logger log = LogManager.getLogger(CarrierLoadAnalysis.class);
	private final String delimiter;
	final Carriers carriers;

	private final Map<Id<Vehicle>, LinkedList<Integer>> vehicle2Load = new LinkedHashMap<>();
	private final Map<Id<Vehicle>, Integer> vehicle2DemandPerTour = new HashMap<>();

	public CarrierLoadAnalysis(String delimiter, Carriers carriers) {
		this.delimiter = delimiter;
		this.carriers = carriers;
	}

	@Override
	public void handleEvent(CarrierShipmentPickupStartEvent event) {
		Id<Vehicle> vehicleId = Id.createVehicleId(event.getAttributes().get("vehicle"));
		Integer demand = Integer.valueOf(event.getAttributes().get(ATTRIBUTE_CAPACITYDEMAND));

		LinkedList<Integer> list;
		if (!vehicle2Load.containsKey(vehicleId)) {
			list = new LinkedList<>();
			list.add(demand);
			vehicle2DemandPerTour.put(vehicleId, demand);
		} else {
			list = vehicle2Load.get(vehicleId);
			list.add(list.getLast() + demand);
			vehicle2DemandPerTour.put(vehicleId, vehicle2DemandPerTour.get(vehicleId) + demand);
		}
		vehicle2Load.put(vehicleId, list);
	}

	@Override
	public void handleEvent(CarrierShipmentDeliveryStartEvent event) {
		Id<Vehicle> vehicleId = Id.createVehicleId(event.getAttributes().get("vehicle"));
		Integer demand = Integer.valueOf(event.getAttributes().get(ATTRIBUTE_CAPACITYDEMAND));

		var list = vehicle2Load.get(vehicleId);
		list.add(list.getLast() - demand);
		vehicle2Load.put(vehicleId, list);
	}

	void writeLoadPerVehicle(String analysisOutputDirectory, Scenario scenario) {
		log.info("Writing out vehicle load analysis ...");
		//Load per vehicle
		String fileName = Path.of(analysisOutputDirectory).resolve("Load_perVehicle.tsv").toString();

		try (BufferedWriter bw1 = new BufferedWriter(new FileWriter(fileName))) {

			//Write headline:
			bw1.write(String.join(delimiter, "vehicleId",
				"vehicleTypeId",
				"capacity",
				"maxLoad",
				"maxLoadPercentage",
				"handledDemand",
				"load state during tour"));
			bw1.newLine();

			for (Id<Vehicle> vehicleId : vehicle2Load.keySet()) {

				final LinkedList<Integer> load = vehicle2Load.get(vehicleId);
				final Integer maxLoad = load.stream().max(Comparator.naturalOrder()).orElseThrow();

				final VehicleType vehicleType = VehicleUtils.findVehicle(vehicleId, scenario).getType();
				final Double capacity = vehicleType.getCapacity().getOther();

				final Integer demand = vehicle2DemandPerTour.get(vehicleId);
				final double maxLoadPercentage = Math.round(maxLoad / capacity * 10000) / 100.0;

				bw1.write(vehicleId.toString());
				bw1.write(delimiter + vehicleType.getId().toString());
				bw1.write(delimiter + capacity);
				bw1.write(delimiter + maxLoad);
				bw1.write(delimiter + maxLoadPercentage);
				bw1.write(delimiter + demand);
				bw1.write(delimiter + load);
				bw1.newLine();
			}

			bw1.close();
			log.info("Output written to {}", fileName);
		}
		catch (IOException e) {
			log.error("Could not write output file: {}", e.getMessage());
		}
	}
}
