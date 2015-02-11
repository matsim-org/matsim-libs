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
package playground.agarwalamit.mixedTraffic.patnaIndia.analysis;

import java.io.BufferedWriter;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.legMode.distributions.LegModeTravelTimeHandler;
import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * @author amit
 */

public class PatnaTravelTimeAnalyzer extends AbstractAnalyisModule {


	public PatnaTravelTimeAnalyzer(String inputEventsFile) {
		super(PatnaTravelTimeAnalyzer.class.getSimpleName());
		this.inputEventsFile = inputEventsFile;
	}

	private SortedMap<String, Double> mode2AvgTripTime = new TreeMap<String, Double>();
	private SortedMap<String, Double> mode2TotalTravelTime = new TreeMap<String, Double>();
	private LegModeTravelTimeHandler travelTimeHandler = new LegModeTravelTimeHandler();

	private String inputEventsFile;
	
	
	public static void main(String[] args) {
		String dir = "/Users/amit/Documents/repos/runs-svn/patnaIndia/run103/";
		String eventFile = dir+"/seepage/ITERS/it.200/200.events.xml.gz";
		String outputFolder = dir+"/seepage/analysis/";
		PatnaTravelTimeAnalyzer timeAnalyzer  = new PatnaTravelTimeAnalyzer(eventFile);
		timeAnalyzer.preProcessData();
		timeAnalyzer.postProcessData();
		timeAnalyzer.writeResults(outputFolder);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}
	@Override
	public void preProcessData() {
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);

		events.addHandler(travelTimeHandler);
		reader.readFile(inputEventsFile);

	}
	@Override
	public void postProcessData() {
		SortedMap<String, Map<Id<Person>, List<Double>>> times = travelTimeHandler.getLegMode2PesonId2TripTimes();
		for(String mode :times.keySet()){
			double tripTimes =0;
			double count = 0;
			for(Id<Person> id : times.get(mode).keySet()){
				for(Double d :times.get(mode).get(id)){
					tripTimes+=d;
					count++;
				}
			}
			mode2TotalTravelTime.put(mode, tripTimes);
			mode2AvgTripTime.put(mode, tripTimes/count);
		}
	}
	@Override
	public void writeResults(String outputFolder) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/modalTravelTime.txt");
		try {
			writer.write("mode \t avgTripTime(min) \t totalTripTime(hr) \n");

			for(String mode:mode2AvgTripTime.keySet()){
				writer.write(mode+"\t"+mode2AvgTripTime.get(mode)/60+"\t"+mode2TotalTravelTime.get(mode)/3600+"\n");
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}


}
