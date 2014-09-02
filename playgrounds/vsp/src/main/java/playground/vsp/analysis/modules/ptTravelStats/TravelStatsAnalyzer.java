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

package playground.vsp.analysis.modules.ptTravelStats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * @author aneumann, sfuerbas
 */
public class TravelStatsAnalyzer extends AbstractAnalyisModule {

	private final static Logger log = Logger.getLogger(TravelStatsAnalyzer.class);
	private final String separator = "\t";
	private Scenario scenario;
	private TravelStatsHandler handler;
	
	public TravelStatsAnalyzer(Scenario scenario, Double interval) {
		super(TravelStatsAnalyzer.class.getSimpleName());
		this.scenario = scenario;
		this.handler = new TravelStatsHandler(scenario, interval);
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new ArrayList<EventHandler>();
		handler.add(this.handler);
		return handler;
	}

	@Override
	public void preProcessData() {
		// nothing to pre-processed
	}

	@Override
	public void postProcessData() {
		// nothing to post-processed
	}

	@Override
	public void writeResults(String outputFolder) {
		
		HashMap<String, Counts> mode2countsMap;
		String countsName;
		
		mode2countsMap = this.handler.getMode2CountsCapacity();
		countsName = "capacity";
		writeCounts(outputFolder, mode2countsMap, countsName);
		
		mode2countsMap = this.handler.getMode2CountsCapacity_m();
		countsName = "capacityMeter";
		writeCounts(outputFolder, mode2countsMap, countsName);
		
		mode2countsMap = this.handler.getMode2CountsPax();
		countsName = "pax";
		writeCounts(outputFolder, mode2countsMap, countsName);
		
		mode2countsMap = this.handler.getMode2CountsPax_m();
		countsName = "paxMeter";
		writeCounts(outputFolder, mode2countsMap, countsName);
		
		mode2countsMap = this.handler.getMode2CountsVolume();
		countsName = "vehicle";
		writeCounts(outputFolder, mode2countsMap, countsName);
		
		this.writeOccupancy(outputFolder, this.handler.getMode2CountsPax(), this.handler.getMode2CountsCapacity());
	}

	private void writeCounts(String outputFolder, HashMap<String, Counts> mode2countsMap, String countsName) {
		for (Entry<String, Counts> mode2Counts : mode2countsMap.entrySet()) {
			
			String fileName = outputFolder + mode2Counts.getKey() + "_" + countsName + ".txt";

			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));
				bw.write("# Link Id" + separator + "x1" + separator + "y1" + separator + "x2" + separator + "y2" + separator + "total");
				for (int i = 0; i < 25; i++) {
					bw.write(separator + i);
				}
				
				for (Entry<Id<Link>, Count> linkId2Count : mode2Counts.getValue().getCounts().entrySet()) {
					// write one line for each count station registered - links without any count will not be written to file
					bw.newLine();
					bw.write(linkId2Count.getKey().toString());
					
					Link link = this.scenario.getNetwork().getLinks().get(linkId2Count.getKey());
					bw.write(separator + link.getFromNode().getCoord().getX());
					bw.write(separator + link.getFromNode().getCoord().getY());
					bw.write(separator + link.getToNode().getCoord().getX());
					bw.write(separator + link.getToNode().getCoord().getY());
					
					double sum = 0.0;
					for (Volume volume : linkId2Count.getValue().getVolumes().values()) {
						sum += volume.getValue();
					}
					bw.write(separator + sum);
					
					for (int i = 0; i < 25; i++) {
						double value = 0.0;
						if (linkId2Count.getValue().getVolume(i) != null) {
							value = linkId2Count.getValue().getVolume(i).getValue();
						}
						bw.write(separator + value);
					}
				}
				
				bw.close();
				
				log.info("Output written to " + fileName);
				
			} catch (IOException e) {
				e.printStackTrace();
			}		
		}
	}
	
	/**
	 * This is kind of an awkward implementation duplicating all that code
	 * 
	 * @param outputFolder
	 * @param paxCounts
	 * @param capCounts
	 */
	private void writeOccupancy(String outputFolder, HashMap<String, Counts> paxCounts, HashMap<String, Counts> capCounts) {
		for (Entry<String, Counts> mode2CapCounts : capCounts.entrySet()) {
			
			String fileName = outputFolder + mode2CapCounts.getKey() + "_occupancy.txt";

			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));
				bw.write("# Link Id" + separator + "x1" + separator + "y1" + separator + "x2" + separator + "y2" + separator + "total");
				for (int i = 0; i < 25; i++) {
					bw.write(separator + i);
				}
				
				for (Entry<Id<Link>, Count> linkId2Count : mode2CapCounts.getValue().getCounts().entrySet()) {
					// write one line for each count station registered - links without any count will not be written to file
					bw.newLine();
					bw.write(linkId2Count.getKey().toString());
					
					Link link = this.scenario.getNetwork().getLinks().get(linkId2Count.getKey());
					bw.write(separator + link.getFromNode().getCoord().getX());
					bw.write(separator + link.getFromNode().getCoord().getY());
					bw.write(separator + link.getToNode().getCoord().getX());
					bw.write(separator + link.getToNode().getCoord().getY());
					
					double capSum = 0.0;
					for (Volume volume : linkId2Count.getValue().getVolumes().values()) {
						capSum += volume.getValue();
					}
					
					double paxSum = 0.0;
					for (Volume volume : paxCounts.get(mode2CapCounts.getKey()).getCounts().get(linkId2Count.getKey()).getVolumes().values()) {
						paxSum += volume.getValue();
					}
					
					if (capSum == 0.0) {
						bw.write(separator + 0.0); // Probably Double.NaN
					} else {
						bw.write(separator + paxSum / capSum);
					}
					
					for (int i = 0; i < 25; i++) {
						double capValue = 0.0;
						if (linkId2Count.getValue().getVolume(i) != null) {
							capValue = linkId2Count.getValue().getVolume(i).getValue();
						}
						
						double paxValue = 0.0;
						if(paxCounts.get(mode2CapCounts.getKey()).getCounts().get(linkId2Count.getKey()).getVolume(i) != null){
							paxValue = paxCounts.get(mode2CapCounts.getKey()).getCounts().get(linkId2Count.getKey()).getVolume(i).getValue();
						}
						
						if (capValue == 0.0) {
							bw.write(separator + 0.0); // Probably Double.NaN
						} else {
							bw.write(separator + paxValue / capValue);
						}
					}
				}
				
				bw.close();
				
				log.info("Output written to " + fileName);
				
			} catch (IOException e) {
				e.printStackTrace();
			}		
		}
	}
}
