/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.legMode.distributions;

import java.io.BufferedWriter;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * 
 * @author amit
 */

public class FilteredDepartureTimeAnalyzer {
	private final FilteredDepartureTimeHandler handler;
	private final String eventsFile ;
	
	public FilteredDepartureTimeAnalyzer (final String eventsFile, final double simulationEndTime, final int noOfTimeBins){
		this(eventsFile, simulationEndTime/noOfTimeBins, null);
	}
	
	public FilteredDepartureTimeAnalyzer (final String eventsFile, final double timeBinSize){
		this(eventsFile, timeBinSize, null);
	}
	
	/**
	 *  @param userGroup
	 * Data will include persons from the given user group.
	 */
	public FilteredDepartureTimeAnalyzer (final String eventsFile, final double timeBinSize, final String userGroup){
		this.handler = new FilteredDepartureTimeHandler(timeBinSize, userGroup);
		this.eventsFile = eventsFile;
	}

	public static void main(String[] args) {
		String outDir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/iatbr/output/bau/";
		String eventsFile = outDir+"/ITERS/it.1500/1500.events.xml.gz";

		double timebinSize = 3600.;
		
		for (UserGroup ug : UserGroup.values()) {
			if(ug.equals(UserGroup.REV_COMMUTER)) continue;
			FilteredDepartureTimeAnalyzer lmtdd = new FilteredDepartureTimeAnalyzer(eventsFile, timebinSize);
			lmtdd.run();
			lmtdd.writeResults(outDir+"/analysis/departureCounts_"+ug.toString()+".txt");	
		}
	}

	public void run() {
		EventsManager events  = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(this.handler);
		reader.readFile(eventsFile);
		this.handler.handleRemainingTransitUsers();
	}
	
	public void writeResults(String outputFile) {
		Map<String, SortedMap<Double, Integer>> mode2time2count = this.handler.getMode2TimeBin2Count();
		
		// get a common time bin
		SortedSet<Double> timebins = new TreeSet<>();
		for(String m : mode2time2count.keySet()){
			timebins.addAll(mode2time2count.get(m).keySet());
		}
		
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			writer.write("departureTime \t");
			for(double time : timebins) {
				writer.write(time+"\t");
			}
			writer.newLine();
			
			for(String mode : mode2time2count.keySet()){
				writer.write(mode+"\t");
				for(double time : timebins){
					double count = 0;
					if(mode2time2count.get(mode).containsKey(time)) count = mode2time2count.get(mode).get(time); 
					writer.write(count + "\t");
				}
				writer.newLine();
			}
			writer.close();
		}  catch (Exception e) {
			throw new RuntimeException("Data is not written to the file. Reason "+e);
		}
	}
}