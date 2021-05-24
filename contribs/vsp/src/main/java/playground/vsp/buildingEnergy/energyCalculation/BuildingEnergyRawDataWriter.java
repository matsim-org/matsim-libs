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
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyMATSimDataReader.LinkOccupancyStats;
import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyMATSimDataReader.PopulationStats;
import playground.vsp.buildingEnergy.linkOccupancy.LinkActivityOccupancyCounter;

/**
 * @author droeder
 *
 */
public class BuildingEnergyRawDataWriter {

	private static final Logger log = Logger
			.getLogger(BuildingEnergyRawDataWriter.class);
	
	private String baseRunId;
	private Map<String, Map<String, LinkOccupancyStats>> run2type2RawOccupancy;
	private Map<String, PopulationStats> run2PopulationStats;
	private List<Integer> bins;

	private List<Id> links;

	public BuildingEnergyRawDataWriter(String baseRunId, 
						Map<String, Map<String, LinkOccupancyStats>> run2type2RawOccupancy, 
						Map<String, PopulationStats> run2PopulationStats,
						List<Integer> timeBins,
						List<Id> links) {
		this.baseRunId = baseRunId;
		this.run2PopulationStats = run2PopulationStats;
		this.run2type2RawOccupancy = run2type2RawOccupancy;
		this.bins = timeBins;
		this.links = links;
	}

	/**
	 */
	void write(String path) {
		log.info("writing raw-data to " + path + ".");
		StringBuffer b = new StringBuffer();
		b.append(";personWithWorkActivity;personsWithHomeActivity;personWithHomeAndWorkActivity;\n");
		dumpSingleRunRawData(path, baseRunId, run2type2RawOccupancy.get(baseRunId));
		b.append(baseRunId + ";" + run2PopulationStats.get(baseRunId).getWorkCnt() + ";" + 
							run2PopulationStats.get(baseRunId).getHomeCnt() + ";" + 
							run2PopulationStats.get(baseRunId).getHomeAndWorkCnt() + ";\n");
		for(String id : run2type2RawOccupancy.keySet()){
			if(id.equals(baseRunId)) continue; // skip the base-run we want it on first position
			dumpSingleRunRawData(path, id, run2type2RawOccupancy.get(id));
			b.append(id + ";" + run2PopulationStats.get(id).getWorkCnt() + ";" + 
					run2PopulationStats.get(id).getHomeCnt() + ";" + 
					run2PopulationStats.get(id).getHomeAndWorkCnt() + ";\n");
		}
		BufferedWriter w =  IOUtils.getBufferedWriter(path + "populationActivityStats.csv.gz");
		try {
			w.write(b.toString());
			w.flush();
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("finished (writing raw-data to " + path + ").");
	}

	/**
	 * @param id
	 * @param rawRunAnalysis 
	 */
	private void dumpSingleRunRawData(String path, String id, Map<String, LinkOccupancyStats> rawRunAnalysis) {
		for(Entry<String, LinkOccupancyStats> e: rawRunAnalysis.entrySet()){
			writeOccupancy(path, id + "." + e.getKey(), e.getValue().getStats());
		}
	}

	/**
	 * @param id
	 * @param map
	 */
	private void writeOccupancy(String path, String id, Map<String, LinkActivityOccupancyCounter> map) {
		BufferedWriter writer =  IOUtils.getBufferedWriter(path + id + ".activityCounts.csv.gz");
		try {
			// write header
			writer.write(";");
			for(int t: bins){
				writer.write(String.valueOf(t) + ";");
			}
			writer.write(BuildingEnergyAnalyzer.all+ ";");
			//write content
			writer.write("\n");
			for(Id l: this.links){
				writer.write(l.toString() + ";");
				for(int t: bins){
					writer.write(map.get(String.valueOf(t)).getMaximumOccupancy(l)+ ";");
				}
				writer.write(map.get(BuildingEnergyAnalyzer.all).getMaximumOccupancy(l)+ ";");
				writer.write("\n");
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}

