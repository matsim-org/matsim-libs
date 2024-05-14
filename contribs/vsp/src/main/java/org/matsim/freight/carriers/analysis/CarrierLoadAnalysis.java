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
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.events.CarrierShipmentDeliveryStartEvent;
import org.matsim.freight.carriers.events.CarrierShipmentPickupStartEvent;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

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

	void writeLoadAnalysis(String analysisOutputDirectory, Scenario scenario) throws IOException {
		log.info("Writing out vehicle load analysis ...");
		//Load per vehicle
		String fileName = analysisOutputDirectory + "Load_perVehicle"+RunFreightAnalysisEventBased.fileExtension;

		BufferedWriter bw1 = new BufferedWriter(new FileWriter(fileName));

		//Write headline:
		bw1.write("vehicleId"+RunFreightAnalysisEventBased.delimiter+
				"vehicleTypeId" + RunFreightAnalysisEventBased.delimiter +
				"capacity"+RunFreightAnalysisEventBased.delimiter+
				"maxLoad"+RunFreightAnalysisEventBased.delimiter+
				"usedCapacity[%]" + RunFreightAnalysisEventBased.delimiter +
				"load state during tour");
		bw1.newLine();

		// for calculation
		List<Double> perc = new ArrayList();
		List<String> types = new ArrayList();

		// for capacity display
		List<String> capPerType = new ArrayList<>();


		for (Id<Vehicle> vehicleId : vehicle2Load.keySet()) {

			final LinkedList<Integer> load = vehicle2Load.get(vehicleId);
			final Integer maxLoad = load.stream().max(Comparator.naturalOrder()).get();

			final VehicleType vehicleType = VehicleUtils.findVehicle(vehicleId, scenario).getType();
			final Double capacity = vehicleType.getCapacity().getOther();

			perc.add(maxLoad/capacity);
			String cap = vehicleType.getId().toString() + RunFreightAnalysisEventBased.delimiter + capacity;
			if (!capPerType.contains(cap)) {
				capPerType.add(cap);
			}
			if (!types.contains(vehicleType.getId().toString())) {
				types.add(vehicleType.getId().toString());
			}

			bw1.write(vehicleId.toString());
			bw1.write(RunFreightAnalysisEventBased.delimiter + vehicleType.getId().toString());
			bw1.write(RunFreightAnalysisEventBased.delimiter + capacity);
			bw1.write(RunFreightAnalysisEventBased.delimiter + maxLoad);
			bw1.write(RunFreightAnalysisEventBased.delimiter + Math.round(100*100*maxLoad/capacity)/100);
			bw1.write(RunFreightAnalysisEventBased.delimiter + load);
			bw1.newLine();
		}

		bw1.close();
		log.info("Output written to " + fileName);

		//Tiles with used vehicleTypes & average load and capacity per vehicle type

		log.info("Writing out summary of vehicle load analysis ...");

		//Write file for tiles
		String fileName1 = analysisOutputDirectory + "Load_summary.csv";
		BufferedWriter bw2 = new BufferedWriter(new FileWriter(fileName1));

		//For calculation of average usage (%)
		double use = Math.round(perc.stream().mapToDouble(Double::doubleValue).sum()/ perc.size()*100);

		// Determination of all VehicleTypes in CarriervehicleTypes container. Used so that even unused vehTypes appear in the output
		TreeMap<Id<VehicleType>, VehicleType> vehicleTypesMap = new TreeMap<>(CarriersUtils.getCarrierVehicleTypes(scenario).getVehicleTypes());
		//For the case that there are additional vehicle types found in the events.
		for (Id<Vehicle> vehicleId  : vehicle2Load.keySet()) {
			VehicleType vehicleType  = VehicleUtils.findVehicle(vehicleId, scenario).getType();
			vehicleTypesMap.putIfAbsent(vehicleType.getId(),vehicleType);
		}

		bw2.write("Used vehicle types"+ RunFreightAnalysisEventBased.delimiter
				+ types.size() +"/"+vehicleTypesMap.size()+ RunFreightAnalysisEventBased.delimiter +
				"truck");
		bw2.newLine();
		bw2.write("Average use of capacity"+ RunFreightAnalysisEventBased.delimiter
				+ use + "%" + RunFreightAnalysisEventBased.delimiter
				+"chart-pie");
		bw2.close();
		log.info("Output written to " + fileName1);

		//Capacity per vehicle type
		String fileName2 = analysisOutputDirectory + "Capacity_summary.csv";
		BufferedWriter bw3 = new BufferedWriter(new FileWriter(fileName2));

		//Write file
		bw3.write("vehicleTypeId"+ RunFreightAnalysisEventBased.delimiter
				+ "maxCapacity");
		bw3.newLine();

		for (String cap : capPerType) {
			bw3.write(cap);
			bw3.newLine();
		}

		bw3.close();
		log.info("Output written to " + fileName2);

	}
}
