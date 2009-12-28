/* *********************************************************************** *
 * project: org.matsim.*
 * TimeWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.yu.bottleneck;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.io.IOUtils;

/**
 * prepare Departure time- arrival Time- Diagramm
 * 
 * @author ychen
 */
public class TimeWriter implements AgentDepartureEventHandler,
		AgentArrivalEventHandler {
	// -------------------------MEMBER
	// VARIABLES---------------------------------
	private BufferedWriter out = null;
	private HashMap<String, Double> agentDepTimes;
	private final List<Double> depTimes = new ArrayList<Double>();
	private final List<Double> arrTimes = new ArrayList<Double>();

	// --------------------------CONSTRUCTOR-------------------------------------
	public TimeWriter(final String filename) {
		init(filename);
	}

	/**
	 * If an agent departures, will the information be saved in a hashmap
	 * (agentDepTimes).
	 */
	public void handleEvent(final AgentDepartureEvent event) {
		if (!this.agentDepTimes.containsKey(event.getPersonId().toString())) { // only
																				// store
																				// first
																				// departure
			agentDepTimes.put(event.getPersonId().toString(), event.getTime());
		}
	}

	/**
	 * If an agent arrives, will the "agent-ID", "depTime" and "arrTime" be
	 * written in a .txt-file
	 */
	public void handleEvent(final AgentArrivalEvent event) {
		String agentId = event.getPersonId().toString();
		if (agentDepTimes.containsKey(agentId)) {
			int depT = (int) agentDepTimes.remove(agentId).doubleValue();
			depTimes.add((double) depT);
			int depH = depT / 3600;
			int depMin = (depT - depH * 3600) / 60;
			int depSec = depT - depH * 3600 - depMin * 60;
			int time = (int) event.getTime();
			arrTimes.add((double) time);
			int h = time / 3600;
			int min = (time - h * 3600) / 60;
			int sec = time - h * 3600 - min * 60;
			writeLine(agentId + "\t" + depH + ":" + depMin + ":" + depSec
					+ "\t" + h + ":" + min + ":" + sec);
		}
	}

	public void init(final String outfilename) {
		if (out != null)
			try {
				out.close();
				out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		try {
			out = IOUtils.getBufferedWriter(outfilename);
			writeLine("agentId\tdepTime\tarrTime");
		} catch (IOException e) {
			e.printStackTrace();
		}
		agentDepTimes = new HashMap<String, Double>();
	}

	private void writeLine(final String line) {
		try {
			out.write(line);
			out.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeChart(final String chartFilename) {
		XYScatterChart chart = new XYScatterChart("departure and arrival Time",
				"departureTime", "arrivalTime");
		double[] dTArray = new double[depTimes.size()];
		double[] aTArray = new double[arrTimes.size()];
		for (int i = 0; i < depTimes.size(); i++) {
			dTArray[i] = depTimes.get(i).doubleValue();
			aTArray[i] = arrTimes.get(i).doubleValue();
		}
		chart.addSeries("depTime/arrTime", dTArray, aTArray);
		chart.saveAsPng(chartFilename, 1024, 768);
	}

	public void reset(final int iteration) {
		agentDepTimes.clear();
	}

	public void closeFile() {
		if (out != null)
			try {
				out.close();
				out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		// final String netFilename = "./test/yu/ivtch/input/network.xml";
		final String netFilename = "./test/yu/equil_test/equil_net.xml";
		// final String eventsFilename =
		// "./test/yu/test/input/run265opt100.events.txt.gz";
		final String eventsFilename = "./test/yu/test/input/7-9-6.100.events.txt.gz";
		final String outputFilename = "./test/yu/test/output/7-9-6.times.txt.gz";
		final String chartFilename = "./test/yu/test/output/7-9-6.times.png";

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		EventsManagerImpl events = new EventsManagerImpl();

		TimeWriter tw = new TimeWriter(outputFilename);
		events.addHandler(tw);

		new MatsimEventsReader(events).readFile(eventsFilename);

		tw.writeChart(chartFilename);
		tw.closeFile();

		System.out.println("-> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
