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
import java.util.*;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;


public class LinkAnalysis {
	
	private static final Logger log = Logger.getLogger(LinkAnalysis.class);

	private final int startTime;
	private final int endTime;

	private final int timespan;
	private List<Id<Link>> linkIds;
	private EventsManager events;
	private LinkAnalysisHandler handler;
	private Network network = NetworkUtils.createNetwork();

	public LinkAnalysis(final List<Id<Link>> linkIds, final String inputEventsFile, final int startTime,  final int endTime, String networkFile) {
		this.linkIds = linkIds;
		this.startTime = startTime;
		this.endTime = endTime;
		this.timespan = endTime-startTime;

		//create an event object
		 events = EventsUtils.createEventsManager();
		new MatsimNetworkReader(network).readFile(networkFile);

		//create the handler and add it
		handler = new LinkAnalysisHandler();
		handler.setLinkIds(linkIds);
		handler.setTimeSpan(startTime, endTime);
		handler.loadNetwork(network);
		events.addHandler(handler);

		//create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputEventsFile);
	}

	public LinkAnalysis(List<Id<Link>> linkIds, String inputEventsFile, String startTime, String endTime, String networkFile) {
		this(linkIds, inputEventsFile, (int) Time.parseTime(startTime), (int) Time.parseTime(endTime), networkFile);
	}

	public LinkAnalysis(List<Id<Link>> linkIds, String inputEventsFile) {
		this(linkIds, inputEventsFile, 0, 24*3600, null);
	}


	public void runTravelTimeAnalysis(String outputCSV) throws FileNotFoundException, UnsupportedEncodingException {
		writeCsvFile(handler.getTravelTimes(), outputCSV);
	}

	public void runLinkVolumeAnalysis(String outputCSV) throws FileNotFoundException, UnsupportedEncodingException {
		writeCsvFile(handler.getVolumes(), outputCSV);
	}

	public void runLinkVolumeAnalysisXY(String outputCSV) throws FileNotFoundException, UnsupportedEncodingException {
		writeXYCsvFile(handler.getVolumesXY(), outputCSV);
	}

	public void runTimeSpaceAnalysis(String outputCSV) throws FileNotFoundException, UnsupportedEncodingException {
		writeXYCsvFile(handler.getTimeSpace(), outputCSV);
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

	private void writeXYCsvFile(Map<String, Map<Double, Double>> input, String filename) throws FileNotFoundException, UnsupportedEncodingException {
		// write to file
		Map<String, Integer> columns = new HashMap<>();

		String head = "";
		int c=1;
		for(String id : input.keySet()) {
			head += "veh:"+id+";;";
			columns.put(id, c);
			c+=2;
		}

		PrintWriter writer = new PrintWriter(filename, "UTF-8");

		writer.println(head);


		// prepare for generateCsv
		// <<line, column>, value>
		Map<Tuple<Integer, Integer>, String> keyTable = new HashMap<>();

		for(Entry<String, Map<Double, Double>> entry : input.entrySet()) {
			Integer col = columns.get(entry.getKey());
			int line = 1;
			for(Entry<Double, Double> xy : entry.getValue().entrySet()) {
				keyTable.put(new Tuple<>(line, col), Time.writeTime(xy.getKey(), "HH:mm:ss"));
				keyTable.put(new Tuple<>(line++, col+1), xy.getValue().toString());
			}
		}

		List<String> csv = generateCsvLines(keyTable);

		for(String printLine : csv) {
			writer.println(printLine);
		}

		writer.close();
	}

	private List<String> generateCsvLines(Map<Tuple<Integer, Integer>, String> keyTable) {
		// From <<line, column>, value> to <line, <column, value>>
		Map<Integer, Map<Integer, String>> lin_colVal = new TreeMap<>();
		for(Entry<Tuple<Integer, Integer>, String> entry : keyTable.entrySet()) {
			Map<Integer, String> line = MapUtils.getMap(entry.getKey().getFirst(), lin_colVal);
			line.put(entry.getKey().getSecond(), entry.getValue());
		}

		// From <line, <column, value>> value> to <line, String>
		Map<Integer, String> csvLines = new TreeMap<>();
		for(Entry<Integer, Map<Integer, String>> entry : lin_colVal.entrySet()) {
			String line = "";
			for(String value : entry.getValue().values()) {
				line += value+";";
			}
			csvLines.put(entry.getKey(), line);
		}

		return new LinkedList<String>(csvLines.values());
	}

}
