/* *********************************************************************** *
 * project: org.matsim.*
 * MeanQueueLengthCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.analysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.ConfigUtils;

public class MeanQueueLengthCalculator implements LinkEnterEventHandler, LinkLeaveEventHandler, 
	AgentArrivalEventHandler, AgentDepartureEventHandler, AgentStuckEventHandler {

	private static final Logger log = Logger.getLogger(MeanQueueLengthCalculator.class);
	
	private static String networkFile = "../matsim/mysimulations/telaviv/network/network.xml";
	private static String eventsFile = "../matsim/mysimulations/telaviv/output_QSim/ITERS/it.100/100.events.xml.gz";
	private static String outFile = "../matsim/mysimulations/telaviv/output_QSim/ITERS/it.100/100.meanQueueLength.csv";
	
	private static Charset charset = Charset.forName("UTF-8");
	private static String separator = ","; 
		
	private Scenario scenario;
	private Map<Id, AtomicInteger> counts;	// current counts
	private Map<Id, AtomicInteger> sumCountsPerHour;	// sum of counts in every second of the current hour
	private int hours = 48;
	private Map<Id, double[]> meanCounts;
	
	private Map<Id, LinkInfo> linkInfos;
	
	public static void main(String[] args) throws Exception {
		new MeanQueueLengthCalculator();
	}
	
	public MeanQueueLengthCalculator() throws Exception {
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		
		linkInfos = new HashMap<Id, LinkInfo>();
		
		counts = new HashMap<Id, AtomicInteger>();
		sumCountsPerHour = new HashMap<Id, AtomicInteger>();
		meanCounts = new HashMap<Id, double[]>();
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			linkInfos.put(link.getId(), new LinkInfo(link.getId()));
			
			counts.put(link.getId(), new AtomicInteger());
			sumCountsPerHour.put(link.getId(), new AtomicInteger());
			meanCounts.put(link.getId(), new double[hours]);
		}
		
		log.info("start reading events file...");
		EventsManager eventsManager = new EventsManagerImpl();
		eventsManager.addHandler(this);
		
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readFile(eventsFile);
		log.info("done.");
		
		for (LinkInfo linkInfo : linkInfos.values()) {
			getLinkStatistic(linkInfo);			
		}
		
		log.info("start writing file...");
		writeFile();
	    log.info("done.");
	}
	
	private void writeFile() throws Exception {
	    FileOutputStream fos = null; 
		OutputStreamWriter osw = null; 
	    BufferedWriter bw = null;
		fos = new FileOutputStream(outFile);
		osw = new OutputStreamWriter(fos, charset);
		bw = new BufferedWriter(osw);
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			StringBuffer sb = new StringBuffer();
			sb.append(link.getId());
			
			double[] meanArray = meanCounts.get(link.getId());
			for (double mean : meanArray) {
				sb.append(separator);
				sb.append(mean);
			}
			bw.write(sb.toString());
			bw.write("\n");
		}
		
		bw.close();
		osw.close();
		fos.close();
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		LinkInfo linkInfo = linkInfos.get(event.getLinkId());
		linkInfo.addEvent(event);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		LinkInfo linkInfo = linkInfos.get(event.getLinkId());
		linkInfo.addEvent(event);
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {	
		// count only car travelers
		if (!event.getLegMode().equals(TransportMode.car)) return;
		
		LinkInfo linkInfo = linkInfos.get(event.getLinkId());
		linkInfo.addEvent(event);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		// count only car travelers
		if (!event.getLegMode().equals(TransportMode.car)) return;

		LinkInfo linkInfo = linkInfos.get(event.getLinkId());
		linkInfo.addEvent(event);
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		log.warn("Agent got stuck!");
	}

	@Override
	public void reset(int iteration) {		
	}
	
	private void getLinkStatistic(LinkInfo linkInfo) {
		StringBuffer sb = new StringBuffer();
		sb.append(linkInfo.id);
		
		List<Tuple<Double, Integer>> vehCountChange = new ArrayList<Tuple<Double, Integer>>();
		vehCountChange.add(new Tuple<Double, Integer>(0.0, 0));
				
		int count = 0;
		TimeSlot nextTimeSlot = null;
		while ((nextTimeSlot = linkInfo.timeSlots.poll()) != null) {
		
			Event nextEvent = null;
			while ((nextEvent = nextTimeSlot.eventQueue.poll()) != null) {
	
				if (nextEvent instanceof LinkEnterEvent) count++;
				else if (nextEvent instanceof LinkLeaveEvent) count--;
				else if (nextEvent instanceof AgentArrivalEvent) count--;
				else if (nextEvent instanceof AgentDepartureEvent) count++;				
			}
			
			if (count < 0) {
				log.warn("Id " + linkInfo.id + ": negative vehicle count found :(");
				return;
			}
			vehCountChange.add(new Tuple<Double, Integer>(nextTimeSlot.time, count));
		}
		
		// generate dummy last Tuple, if necessary
		if (vehCountChange.get(vehCountChange.size() - 1).getFirst() <= hours * 3600.0) {
			int lastCount = vehCountChange.get(vehCountChange.size() - 1).getSecond();
			vehCountChange.add(new Tuple<Double, Integer>(hours * 3600.0 + 1, lastCount));
		}
		
		int firstIndex = -1;
		int lastIndex = -1;
		double startHour = 0.0;
		double endHour = 3600.0;
		
		double[] meanArray = meanCounts.get(linkInfo.id);
		for (int i = 0; i < hours; i++) {
			
			for (int j = 0; j < vehCountChange.size(); j++) {
				Tuple<Double, Integer> t = vehCountChange.get(j);
				
				if (t.getFirst() <= startHour) firstIndex = j;
				if (t.getFirst() < endHour) lastIndex = j; 
				else break;
			}
			
			double meanCount = 0.0;
					
			if (firstIndex == lastIndex) {
				if (firstIndex > 0) {
					Tuple<Double, Integer> t = vehCountChange.get(firstIndex);
					meanCount = t.getSecond();					
				}
				else meanCount = 0.0;
			}
			else {
				for (int j = firstIndex; j <= lastIndex; j++) {
					Tuple<Double, Integer> current = vehCountChange.get(j);
					Tuple<Double, Integer> next = vehCountChange.get(j+1);
					
					double tStart = 0.0;
					double tEnd = 0.0;
					if (current.getFirst() < startHour) tStart = startHour;
					else tStart = current.getFirst();
					
					if (next.getFirst() > endHour) tEnd = endHour;
					else tEnd = next.getFirst();
					
					meanCount = meanCount + (tEnd - tStart) * current.getSecond();
				}
				meanCount = meanCount / 3600.0;
			}
			
			meanArray[i] = meanCount;
			startHour = startHour + 3600.0;
			endHour = endHour + 3600.0;
		}
	}
	
	private static class LinkInfo {
		Id id;
		Queue<TimeSlot> timeSlots = new LinkedList<TimeSlot>();
		private TimeSlot lastTimeSlot = null;
		
		public LinkInfo(Id id) {
			this.id = id;
		}
		
		public void addEvent(Event event) {
			// if no TimeSlot exists
			if (lastTimeSlot == null) {
				TimeSlot timeSlot = new TimeSlot(event.getTime());
				timeSlot.eventQueue.add(event);
				timeSlots.add(timeSlot);
				lastTimeSlot = timeSlot;
			} 
			// there is already a last TimeSlot
			else {
				// if the time is equal
				if (lastTimeSlot.time == event.getTime()) {
					lastTimeSlot.eventQueue.add(event);
				}
				// the time is different
				else {
					TimeSlot timeSlot = new TimeSlot(event.getTime());
					timeSlot.eventQueue.add(event);
					timeSlots.add(timeSlot);
					lastTimeSlot = timeSlot;
				}
			}
		}
	}
	
	/*
	 * The order of the Events within a TimeSlot is undefined, therefore we handle them all together.
	 */
	private static class TimeSlot {
		double time;
		Queue<Event> eventQueue = new LinkedList<Event>();
		
		public TimeSlot(double time) {
			this.time = time;
		}
	}
}