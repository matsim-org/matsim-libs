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

package org.matsim.freight.carriers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.vehicles.CostInformation;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;

import java.io.BufferedWriter;
import java.io.IOException;

@Deprecated // only there if someone insists on writing V1
public final class CarrierVehicleTypeWriterV1 extends MatsimXmlWriter {

	@SuppressWarnings("unused")
	private static final  Logger logger = LogManager.getLogger(CarrierVehicleTypeWriter.class );

	private final CarrierVehicleTypes vehicleTypes;


	public CarrierVehicleTypeWriterV1(CarrierVehicleTypes carrierVehicleTypes) {
		super();
		this.vehicleTypes = carrierVehicleTypes;
	}


	public void write(String filename) {
		logger.info("write vehicle-types");
		try {
			openFile(filename);
			writeXmlHead();
			writeTypes(this.writer);
			close();
			logger.info("done");
		} catch ( IOException e) {
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
		}
	}

	private void writeTypes( BufferedWriter writer )throws IOException {
		writer.write("\t<vehicleTypes>\n");
		for( VehicleType type : vehicleTypes.getVehicleTypes().values()){
			writer.write("\t\t<vehicleType id=\"" + type.getId() + "\">\n");
			writer.write("\t\t\t<description>" + type.getDescription() + "</description>\n");
			EngineInformation engineInformation = type.getEngineInformation();
			if(engineInformation != null && !engineInformation.getAttributes().isEmpty()) {
				writer.write("\t\t\t<engineInformation fuelType=\"" + engineInformation.getFuelType().toString() + "\" gasConsumption=\"" + engineInformation.getFuelConsumption() + "\"/>\n");
			}
			writer.write("\t\t\t<capacity>" + type.getCapacity().getWeightInTons() + "</capacity>\n" );
			CostInformation vehicleCostInformation = type.getCostInformation();
			if(vehicleCostInformation == null) throw new IllegalStateException("vehicleCostInformation is missing.");
			writer.write("\t\t\t<costInformation fix=\"" + vehicleCostInformation.getFixedCosts() + "\" perMeter=\"" + vehicleCostInformation.getCostsPerMeter() +
						   "\" perSecond=\"" + vehicleCostInformation.getCostsPerSecond() + "\"/>\n");
			writer.write("\t\t</vehicleType>\n");
		}
		writer.write("\t</vehicleTypes>\n\n");
	}



}
