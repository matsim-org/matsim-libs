/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
 * *********************************************************************** */
package playground.vsp.buildingEnergy.energyCalculation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyAggregatedEnergyConsumptionCalculator.EnergyConsumption;

/**
 * @author droeder
 *
 */
class BuildingEnergyAggregatedEnergyDataWriter {

	private static final Logger log = Logger
			.getLogger(BuildingEnergyAggregatedEnergyDataWriter.class);

	BuildingEnergyAggregatedEnergyDataWriter() {
	}

	/**
	 * @param outputPath
	 */
	void write(String outputPath, Map<String, EnergyConsumption> energyConsumption, List<Integer> timeBins) {
		String file = outputPath + "buildingEnergyConsumption.csv.gz";
		log.info("writing building-energy-consumption-data to " + file + ".");
		BufferedWriter writer = IOUtils.getBufferedWriter(file);
		try {
			//write the header
			writer.write("run;activityType;");
			for(Integer i: timeBins){
				writer.write(String.valueOf(i) + ";");
			}
			writer.write("\n");
			// write the content
			for(Entry<String, EnergyConsumption> run: energyConsumption.entrySet()){
				for(Entry<String, Map<Integer, Double>> activityType :run.getValue().getActType2Consumption().entrySet()){
					writer.write(run.getKey() + ";" + activityType.getKey() + ";");
					for(Integer i: timeBins){
						writer.write(String.valueOf(activityType.getValue().get(i)) + ";");
					}
					writer.write("\n");
				}
			}
			writer.flush();
			writer.close();
			log.info("finished (writing building-energy-consumption-data to " + file + ".");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

