/* *********************************************************************** *
 * project: org.matsim.*
 * EventsReader
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.polettif.crossings.analysis;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.misc.Time;


public class LinkAnalysis {
	
	private static final Logger log = Logger.getLogger(LinkAnalysis.class);

	private final int startTime;
	private final int endTime;

	private final int timespan;
	private List<Id<Link>> linkIds;
	private EventsManager events;
	private LinkAnalysisHandler handler;

	public LinkAnalysis(final List<Id<Link>> linkIds, final String inputEventsFile, final int startTime,  final int endTime) {
		this.linkIds = linkIds;
		this.startTime = startTime;
		this.endTime = endTime;
		this.timespan = endTime-startTime;

		//create an event object
		 events = EventsUtils.createEventsManager();

		//create the handler and add it
		handler = new LinkAnalysisHandler();
		handler.setLinkIds(linkIds);
		handler.setTimeSpan(startTime, endTime);
		events.addHandler(handler);

		//create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputEventsFile);
	}

	public LinkAnalysis(List<Id<Link>> linkIds, String inputEventsFile, String startTime, String endTime) {
		this(linkIds, inputEventsFile, (int) Time.parseTime(startTime), (int) Time.parseTime(endTime));
	}

	public LinkAnalysis(List<Id<Link>> linkIds, String inputEventsFile) {
		this(linkIds, inputEventsFile, 0, 24*3600);
	}

	public void runTravelTimeAnalysis(String outputCSV) throws FileNotFoundException, UnsupportedEncodingException {
		writeCsvFile(handler.getTravelTimes(), outputCSV);
	}

	public void runLinkVolumeAnalysis(String outputCSV) throws FileNotFoundException, UnsupportedEncodingException {
		writeCsvFile(handler.getVolumes(), outputCSV);
	}

	private void writeCsvFile(Map<Id<Link>, double[]> table, String filename) throws FileNotFoundException, UnsupportedEncodingException {
		// write to file
		int n = linkIds.size();
		double[][] csv = new double[n][timespan];
		int l = 0;
		String head = "time [HH:mm:ss]; time [s]";
		PrintWriter writer = new PrintWriter(filename, "UTF-8");

		//write head
		for(Entry<Id<Link>, double[]> entry : table.entrySet()) {
			csv[l] = entry.getValue();

			head = head+"; "+entry.getKey();
			l++;
		}
		writer.println(head);

		// write rest
		for(int j=0; j < (endTime-startTime); j++) {

			// time in first two cols
			writer.print(Time.writeTime(startTime+j) + "; " + startTime+j);

			// write travel times
			for(int i=0; i < n; i++) {
				writer.print("; "+csv[i][j]);
			}
			writer.println();
		}
		writer.close();
	}

}
