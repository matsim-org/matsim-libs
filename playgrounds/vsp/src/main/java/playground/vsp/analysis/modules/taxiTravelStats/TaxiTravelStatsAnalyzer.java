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

package playground.vsp.analysis.modules.taxiTravelStats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * Writes counts data for pax, paxMeter, transit vehicle capacity, capacityMeter, and number of taxi cabs each per link and mode.
 * 
 * @author aneumann
 */
public class TaxiTravelStatsAnalyzer extends AbstractAnalysisModule {

	public final static String TAXI_PREFIX = "taxi";
	public final static String CAPACITY = "capacity";
	public final static String CAPACITY_METER = "capacityMeter";
	public final static String PAX = "pax";
	public final static String PAX_METER = "paxMeter";
	public final static String VEHICLE = "vehicle";
	public final static String OCCUPANCY = "occupancy";
	
	private final static Logger log = Logger.getLogger(TaxiTravelStatsAnalyzer.class);
	private final String separator = ";";
	private final String header = "linkId" + separator + "x1" + separator + "y1" + separator + "x2" + separator + "y2" + separator + "total";
	private Scenario scenario;
	private TaxiTravelStatsHandler handler;
	
	public TaxiTravelStatsAnalyzer(Scenario scenario, Double interval) {
		super(TaxiTravelStatsAnalyzer.class.getSimpleName());
		this.scenario = scenario;
		this.handler = new TaxiTravelStatsHandler(scenario, interval);
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new ArrayList<EventHandler>();
		handler.add(this.handler);
		return handler;
	}

	@Override
	public void preProcessData() {
		// nothing to pre-process
	}

	@Override
	public void postProcessData() {
		// nothing to post-process
	}

	@Override
	public void writeResults(String outputFolder) {
		
		Counts<Link> mode2countsMap;
		String countsName;
		
		mode2countsMap = this.handler.getCountsCapacity();
		countsName = TaxiTravelStatsAnalyzer.CAPACITY;
		writeCounts(outputFolder, mode2countsMap, countsName);
		
		mode2countsMap = this.handler.getCountsCapacity_m();
		countsName = TaxiTravelStatsAnalyzer.CAPACITY_METER;
		writeCounts(outputFolder, mode2countsMap, countsName);
		
		mode2countsMap = this.handler.getCountsPax();
		countsName = TaxiTravelStatsAnalyzer.PAX;
		writeCounts(outputFolder, mode2countsMap, countsName);
		
		mode2countsMap = this.handler.getCountsPax_m();
		countsName = TaxiTravelStatsAnalyzer.PAX_METER;
		writeCounts(outputFolder, mode2countsMap, countsName);
		
		mode2countsMap = this.handler.getCountsVehicles();
		countsName = TaxiTravelStatsAnalyzer.VEHICLE;
		writeCounts(outputFolder, mode2countsMap, countsName);
		
		this.writeOccupancy(outputFolder, this.handler.getCountsPax(), this.handler.getCountsCapacity());
	}

	/**
	 * Write the counts data to file
	 * 
	 * @param outputFolder
	 * @param countsMap
	 * @param countsName
	 */
	private void writeCounts(String outputFolder, Counts<Link> countsMap, String countsName) {
			
			String fileName = outputFolder + this.createFilename(TAXI_PREFIX, countsName);

			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));
				bw.write(this.header);
				for (int i = 0; i < 25; i++) {
					bw.write(separator + i);
				}
				
				for (Entry<Id<Link>, Count<Link>> linkId2Count : countsMap.getCounts().entrySet()) {
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
						if (linkId2Count.getValue().getVolume(i+1) != null) {
							value = linkId2Count.getValue().getVolume(i+1).getValue();
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
	
	/**
	 * This is kind of an awkward implementation duplicating all that code.
	 * 
	 * Calculates the occupancy for the given pax and capacity counts.
	 * 
	 * @param outputFolder
	 * @param paxCounts
	 * @param capCounts
	 */
	private void writeOccupancy(String outputFolder, Counts<Link> paxCounts, Counts<Link> capCounts) {
			
			String fileName = outputFolder + this.createFilename(TAXI_PREFIX, TaxiTravelStatsAnalyzer.OCCUPANCY);

			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));
				bw.write(this.header);
				for (int i = 0; i < 25; i++) {
					bw.write(separator + i);
				}
				
				for (Entry<Id<Link>, Count<Link>> linkId2Count : capCounts.getCounts().entrySet()) {
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
					for (Volume volume : paxCounts.getCounts().get(linkId2Count.getKey()).getVolumes().values()) {
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
						if(paxCounts.getCounts().get(linkId2Count.getKey()).getVolume(i) != null){
							paxValue = paxCounts.getCounts().get(linkId2Count.getKey()).getVolume(i).getValue();
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
	
	private String createFilename(String mode, String identifier){
		return mode + "_" + identifier + ".csv";
	}
}
