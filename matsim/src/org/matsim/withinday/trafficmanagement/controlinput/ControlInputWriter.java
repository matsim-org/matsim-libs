/* *********************************************************************** *
 * project: org.matsim.*
 * ControlInputWriter.java
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

package org.matsim.withinday.trafficmanagement.controlinput;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;

/**
 * @author dgrether
 *
 */
public class ControlInputWriter {

//	public static final double outputStartTime = 57600;

//	public static final double outputEndTime = 65400;

	public static final double outputStartTime = Double.NEGATIVE_INFINITY;

 	public static final double outputEndTime = Double.POSITIVE_INFINITY;


	private String outputDirectory;

	/**
	 * the separator used between columns
	 */
	private static final String SEPARATOR = "\t";

	/**
	 * newline
	 */
	private static final String NEWLINE = "\n";

	private static final String numberofAgentsFile = "numberOfAgentsOnLinks.txt";

	private static final String traveltimesroute1File = "travelTimesRoute1.txt";

	private static final String traveltimesroute2File = "travelTimesRoute2.txt";

	private static final String measuredPerAgentMainRouteFile = "measuredPerAgentMainRoute.txt";

	private static final String measuredPerAgentAlternativeRouteFile = "measuredPerAgentAlternativeRoute.txt";

	private BufferedWriter agentOnLinks = null;

	private BufferedWriter travelTimesRoute1 = null;

	private BufferedWriter travelTimesRoute2 = null;

	private BufferedWriter measuredPerAgentMainRoute = null;

	private BufferedWriter measuredPerAgentAlternativeRoute = null;

	private boolean writeAgentsOnLinksFirstRun = true;

	public ControlInputWriter() {
		this.outputDirectory = Gbl.getConfig().controler().getOutputDirectory();
		if (!this.outputDirectory.endsWith("/"))
			this.outputDirectory += "/";

		Logger.getLogger(ControlInputWriter.class).info("Writing to output directory: " + this.outputDirectory);
	}

	public void open() {
		try {
			this.agentOnLinks = new BufferedWriter(new FileWriter(
					this.outputDirectory + numberofAgentsFile));
			this.travelTimesRoute1 = new BufferedWriter(new FileWriter(
					this.outputDirectory + traveltimesroute1File));
			writeHeader(this.travelTimesRoute1, new String[] { "Simulation time",
					"Travel time measured", "Travel time predicted" });
			this.travelTimesRoute2 = new BufferedWriter(new FileWriter(
					this.outputDirectory + traveltimesroute2File));
			writeHeader(this.travelTimesRoute2, new String[] { "Simulation time",
					"Travel time measured", "Travel time predicted" });
			this.measuredPerAgentMainRoute = new BufferedWriter(new FileWriter(
					this.outputDirectory + measuredPerAgentMainRouteFile));
			writeHeader(this.measuredPerAgentMainRoute, new String[]{"Simulation time", "deltaT Main route"});
			this.measuredPerAgentAlternativeRoute = new BufferedWriter(
					new FileWriter(this.outputDirectory
							+ measuredPerAgentAlternativeRouteFile));
			writeHeader(this.measuredPerAgentAlternativeRoute, new String[]{"Simulation time", "deltaT Alt route"});

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeHeader(BufferedWriter w, String[] header)
			throws IOException {
		for (String s : header) {
			w.write(s);
			w.write(SEPARATOR);
		}
		w.append(NEWLINE);
	}

	public void writeAgentsOnLinks(final Map<String, Integer> numberOfAgents)
			throws IOException {
		if (this.writeAgentsOnLinksFirstRun) {
			for (String s : numberOfAgents.keySet()) {
				this.agentOnLinks.write(s);
				this.agentOnLinks.write(SEPARATOR);
			}
			this.agentOnLinks.write(NEWLINE);
			this.writeAgentsOnLinksFirstRun = false;
		}
		Integer value;
		for (Entry e : numberOfAgents.entrySet()) {
			value = (Integer) e.getValue();
			this.agentOnLinks.write(Integer.toString(value));
			this.agentOnLinks.write(SEPARATOR);
		}
		this.agentOnLinks.write(NEWLINE);
		this.agentOnLinks.flush();

	}

	public void writeTravelTimesMainRoute(final double simTime,
			final double measuredTT, final double predTT) throws IOException {
		if ((outputStartTime <= simTime) && (simTime <= outputEndTime)) {
			this.travelTimesRoute1.write(Double.toString(simTime));
			this.travelTimesRoute1.write(SEPARATOR);
			this.travelTimesRoute1.write(Double.toString(measuredTT));
			this.travelTimesRoute1.write(SEPARATOR);
			this.travelTimesRoute1.write(Double.toString(predTT));
			this.travelTimesRoute1.write(NEWLINE);
			this.travelTimesRoute1.flush();
		}
	}

	public void writeTravelTimesAlternativeRoute(final double simTime, final double measuredTT,
			final double predTT) throws IOException {
		if ((outputStartTime <= simTime) && (simTime <= outputEndTime)) {
			this.travelTimesRoute2.write(Double.toString(simTime));
			this.travelTimesRoute2.write(SEPARATOR);
			this.travelTimesRoute2.write(Double.toString(measuredTT));
			this.travelTimesRoute2.write(SEPARATOR);
			this.travelTimesRoute2.write(Double.toString(predTT));
			this.travelTimesRoute2.write(NEWLINE);
			this.travelTimesRoute2.flush();
		}
	}

	public void writeTravelTimesPerAgent(final Map<Double, Double> map,
			final Map<Double, Double> map2) throws IOException {
		Double tt;

		SortedMap<Double, Double> sortedMap = new TreeMap<Double, Double>(map);
		SortedMap<Double, Double> sortedMap2 = new TreeMap<Double, Double>(map2);

		for (Double simTime : sortedMap.keySet()) {
			if ((outputStartTime <= simTime) && (simTime <= outputEndTime)) {
				tt = sortedMap.get(simTime);
				this.measuredPerAgentMainRoute.write(Double.toString(simTime));
				this.measuredPerAgentMainRoute.write(SEPARATOR);
				this.measuredPerAgentMainRoute.write(Double.toString(tt));
				this.measuredPerAgentMainRoute.write(NEWLINE);
				this.measuredPerAgentMainRoute.flush();
			}
		}

		for (Double simTime : sortedMap2.keySet()) {
			if ((outputStartTime <= simTime) && (simTime <= outputEndTime)) {
				tt = sortedMap2.get(simTime);
				this.measuredPerAgentAlternativeRoute.write(Double.toString(simTime));
				this.measuredPerAgentAlternativeRoute.write(SEPARATOR);
				this.measuredPerAgentAlternativeRoute.write(Double.toString(tt));
				this.measuredPerAgentAlternativeRoute.write(NEWLINE);
				this.measuredPerAgentAlternativeRoute.flush();
			}
		}
	}

	public void close() {

		try {
			this.agentOnLinks.close();
			this.travelTimesRoute1.close();
			this.travelTimesRoute2.close();
			this.measuredPerAgentMainRoute.close();
			this.measuredPerAgentAlternativeRoute.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
