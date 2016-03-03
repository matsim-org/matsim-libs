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


public class LinkTravelTimeAnalysis {
	
	private static final Logger log = Logger.getLogger(LinkTravelTimeAnalysis.class);

	public static void run(List<Id<Link>> linkIds, String inputEventsFile, String outputCSV, final int startTime,  final int endTime) throws FileNotFoundException, UnsupportedEncodingException {

		//create an event object
		EventsManager events = EventsUtils.createEventsManager();

		//create the handler and add it
		LinkTravelTimeAnalysisHandler handler = new LinkTravelTimeAnalysisHandler();
		handler.setLinkIds(linkIds);
		handler.setTimeSpan(startTime, endTime);
		events.addHandler(handler);

        //create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputEventsFile);

		// get travelTimes on links
		Map<Id<Link>, double[]> travelTimes = handler.getTravelTimes();

		// write to file
		int n = linkIds.size();
		double[][] csv = new double[n][(endTime-startTime)];
		int l = 0;
		String head = "time [HH:mm:ss]; time [s]";
		PrintWriter writer = new PrintWriter(outputCSV, "UTF-8");

		//write head
		for(Entry<Id<Link>, double[]> entry : travelTimes.entrySet()) {
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

	public static void run(List<Id<Link>> linkIds, String inputEventsFile, String outputCSV) throws FileNotFoundException, UnsupportedEncodingException {
		run(linkIds, inputEventsFile, outputCSV, 0, 24*3600);
	}

	public static void run(List<Id<Link>> linkIds, String inputEventsFile, String outputCSV, String startTime, String endTime) throws FileNotFoundException, UnsupportedEncodingException {
		run(linkIds, inputEventsFile, outputCSV, (int) Time.parseTime(startTime), (int) Time.parseTime(endTime));
	}

	/*
	private static void averageTT(Map<Id<Link>, Id<Link>> linkIds) throws FileNotFoundException, UnsupportedEncodingException {
		EventsManager events = EventsUtils.createEventsManager();

		TTAnalysisHandler handler1 = new TTAnalysisHandler();
		handler1.setLinkIds(linkIds);
		events.addHandler(handler1);

		Map<Id<Link>, Double> avgTT = handler1.getAverageTravelTime();
		avgTT(handler1, avgTT);
		int l;
		String head;

		//print
		System.out.println("average travel times");
		for(Entry<Id<Link>, Double> tt : avgTT.entrySet()) {
			System.out.println(tt.getKey()+" "+tt.getValue());
		}

		int[][] csv = new int[8][1400];
		int l = 0;
		String head = "time [min]";
		PrintWriter writer = new PrintWriter("C:/Users/polettif/Desktop/output/analysis/volumes02.csv", "UTF-8");

		for(Entry<Id<Link>, int[]> vol : handler1.getVolumes().entrySet()) {
			csv[l] = vol.getValue();
			head = head+"; "+vol.getKey();
			l++;
		}
		writer.println(head);
		for(int j=0; j < csv[1].length; j++) {
			writer.print(j);
			for(int i=0; i < 8; i++) {
				writer.print("; "+csv[i][j]);
			}
			writer.println();
		}
		writer.close();
	}
*/

}
