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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.events.CarrierShipmentDeliveryStartEvent;
import org.matsim.freight.carriers.events.CarrierShipmentPickupStartEvent;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.matsim.freight.carriers.events.CarrierEventAttributes.ATTRIBUTE_CAPACITYDEMAND;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class CarrierLoadAnalysis implements BasicEventHandler {

	private static final Logger log = LogManager.getLogger(CarrierLoadAnalysis.class);

	Carriers carriers;

	private final Map<Id<Vehicle>, LinkedList<Integer>> vehicle2Load = new LinkedHashMap<>();

	public CarrierLoadAnalysis(Carriers carriers) {
		this.carriers = carriers;
	}

	@Override public void handleEvent(Event event) {
		if (event.getEventType().equals(CarrierShipmentPickupStartEvent.EVENT_TYPE)) {
			handlePickup( event);
		} if (event.getEventType().equals(CarrierShipmentDeliveryStartEvent.EVENT_TYPE)) {
			handleDelivery(event);
		}
	}

	private void handlePickup(Event event) {
		Id<Vehicle> vehicleId = Id.createVehicleId(event.getAttributes().get("vehicle"));
		Integer demand = Integer.valueOf(event.getAttributes().get(ATTRIBUTE_CAPACITYDEMAND));

		LinkedList<Integer> list;
		if (! vehicle2Load.containsKey(vehicleId)){
			list = new LinkedList<>();
			list.add(demand);
		} else {
			list = vehicle2Load.get(vehicleId);
			list.add(list.getLast() + demand);
		}
		vehicle2Load.put(vehicleId, list);
	}


	private void handleDelivery(Event event) {
		Id<Vehicle> vehicleId = Id.createVehicleId(event.getAttributes().get("vehicle"));
		Integer demand = Integer.valueOf(event.getAttributes().get(ATTRIBUTE_CAPACITYDEMAND));

		var list = vehicle2Load.get(vehicleId);
		list.add(list.getLast() - demand);
		vehicle2Load.put(vehicleId, list);
	}

	void writeLoadPerVehicle(String analysisOutputDirectory, Scenario scenario) throws IOException {
		log.info("Writing out vehicle load analysis ...");
		//Load per vehicle
		String fileName = analysisOutputDirectory + "Load_perVehicle.tsv";

		BufferedWriter bw1 = new BufferedWriter(new FileWriter(fileName));

		//Write headline:
		bw1.write("vehicleId \t capacity \t maxLoad \t load state during tour");
		bw1.newLine();

		for (Id<Vehicle> vehicleId : vehicle2Load.keySet()) {

			final LinkedList<Integer> load = vehicle2Load.get(vehicleId);
			final Integer maxLoad = load.stream().max(Comparator.naturalOrder()).get();

			final VehicleType vehicleType = VehicleUtils.findVehicle(vehicleId, scenario).getType();
			final Double capacity = vehicleType.getCapacity().getOther();

			bw1.write(vehicleId.toString());
			bw1.write("\t" + capacity);
			bw1.write("\t" + maxLoad);
			bw1.write("\t" + load);
			bw1.newLine();
		}

		bw1.close();
		log.info("Output written to " + fileName);
	}
}
