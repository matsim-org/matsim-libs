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

import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyActivityProbabilityCalculator.ActivityProbabilities;

/**
 * @author droeder
 *
 */
class BuildingEnergyActivityProbabilityDataWriter {

	private static final Logger log = Logger
			.getLogger(BuildingEnergyActivityProbabilityDataWriter.class);

	BuildingEnergyActivityProbabilityDataWriter() {
	}
	
	void write(String outputPath, ActivityProbabilities proba, List<Integer> timeBins) {
		String file = outputPath + "activityProbability.csv.gz";
		log.info("writing activity-probability-data to " + file + ".");
		BufferedWriter writer = IOUtils.getBufferedWriter(file);
		try {
			//write the header
			writer.write("run;activityType;");
			for(Integer i: timeBins){
				writer.write(String.valueOf(i) + ";");
			}
			writer.write("\n");
			// write the content
			for(Entry<String, Map<String, Map<String, Double>>> run: proba.getProbabilities().entrySet()){
				for(Entry<String, Map<String, Double>> activityType :run.getValue().entrySet()){
					writer.write(run.getKey() + ";" + activityType.getKey() + ";");
					for(Integer i: timeBins){
						writer.write(String.valueOf(activityType.getValue().get(String.valueOf(i))) + ";");
					}
					writer.write("\n");
				}
			}
			writer.flush();
			writer.close();
			log.info("finished (writing activity-probability-data to " + file + ".");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

