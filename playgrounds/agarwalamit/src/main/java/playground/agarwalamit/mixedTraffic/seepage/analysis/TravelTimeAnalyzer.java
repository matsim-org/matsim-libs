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
package playground.agarwalamit.mixedTraffic.seepage.analysis;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.seepage.TestSetUp.SeepageControler;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author amit
 */
public class TravelTimeAnalyzer extends AbstractAnalysisModule {

	public TravelTimeAnalyzer(String eventsFile, String outputDir) {
		super(TravelTimeAnalyzer.class.getSimpleName());
		this.outputDir = outputDir;
		this.eventsFile = eventsFile;
		this.travelTimeHandler = new LinkTravelTimeHandler();
		this.mode2AvgTravelTime = new TreeMap<String, Double>();
		this.mode2TotalTravelTime = new TreeMap<String, Double>();
	}

	private LinkTravelTimeHandler travelTimeHandler;
	private String outputDir;
	private String eventsFile;
	private SortedMap<String, Double> mode2AvgTravelTime;
	private SortedMap<String, Double> mode2TotalTravelTime;


	public static void main(String[] args) {
		String outputDir = SeepageControler.outputDir;
		new TravelTimeAnalyzer(outputDir+"/events.xml",outputDir).run();
	}

	public void run(){
		preProcessData();
		postProcessData();
		writeResults(outputDir);
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {
		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		manager.addHandler(travelTimeHandler);
		reader.readFile(this.eventsFile);
	}

	@Override
	public void postProcessData() {
			mode2AvgTravelTime = travelTimeHandler.getAvgModeTravelTime();
			mode2TotalTravelTime = travelTimeHandler.getMode2TotalTravelTime();
	}

	@Override
	public void writeResults(String outputFolder) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/travelTimes.txt");
		try {
			writer.write("travelMode \t"+"avgTravelTimeInSecOnLink"+travelTimeHandler.travelTimeOnLink.toString()+" \t"+
					"totalTravelTimeInSecOnLink"+travelTimeHandler.travelTimeOnLink.toString()+"\n");
			for(String mode : mode2AvgTravelTime.keySet()){
				writer.write(mode+"\t"+mode2AvgTravelTime.get(mode)+"\t"+mode2TotalTravelTime.get(mode)+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason - " + e);
		}

	}

	private class LinkTravelTimeHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler {

		private SortedMap<String, Double> mode2AvgTime = new TreeMap<>();
		private SortedMap<String, Double> mode2TotalTime = new TreeMap<>();
		private Map<Id<Person>,  Double> person2LinkTravelTime = new HashMap<>();
		public final Id<Link> travelTimeOnLink = Id.createLinkId("1");
		private Map<Id<Person>, String> person2Leg = new HashMap<>();


		@Override
		public void reset(int iteration) {
			mode2AvgTime.clear();
			person2LinkTravelTime.clear();
			person2Leg.clear();
			mode2TotalTime.clear();
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if(event.getLinkId().equals(travelTimeOnLink)){
				double travelTimeSoFar = person2LinkTravelTime.get(event.getDriverId());
				person2LinkTravelTime.put(event.getDriverId(), travelTimeSoFar+event.getTime());
			}
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if(event.getLinkId().equals(travelTimeOnLink)){
				if(!person2LinkTravelTime.containsKey(event.getDriverId())){
					person2LinkTravelTime.put(event.getDriverId(), - event.getTime());
				} else throw new RuntimeException("Person is traveling on this link only once. Aborting...");
			}
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			person2Leg.put(event.getPersonId(), event.getLegMode());
		}
		
		public SortedMap<String,Double> getMode2TotalTravelTime () {
			return mode2TotalTime;
		}
		

		private SortedMap<String,Double> getAvgModeTravelTime(){
			Set<String> travelModes = new HashSet<>();
			travelModes.addAll(person2Leg.values());
			for(String mode : travelModes ){
				double sum =0;
				double count =0;
				for(Id<Person> id : person2LinkTravelTime.keySet()){
					String travelMode = person2Leg.get(id);
					if(travelMode.equals(mode)){
						sum += person2LinkTravelTime.get(id);
						count++;
					}
				}
				mode2AvgTime.put(mode, sum/count);
				mode2TotalTime.put(mode, sum);
			}
			return mode2AvgTime;
		}
	}

}
