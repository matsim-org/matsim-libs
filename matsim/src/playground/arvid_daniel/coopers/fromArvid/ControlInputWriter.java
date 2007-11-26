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

package playground.arvid_daniel.coopers.fromArvid;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;


/**
 * @author dgrether
 *
 */
public class ControlInputWriter {

	private static final String outputDirectory = "../studies/arvidDaniel/output/";

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
	
	private BufferedWriter agentOnLinks = null;
	
	private BufferedWriter travelTimesRoute1 = null;
	
	private BufferedWriter travelTimesRoute2 = null;
	
	private boolean writeAgentsOnLinksFirstRun = true;
	
	public ControlInputWriter() {
	}

	public void open() {
		try {
			this.agentOnLinks = new BufferedWriter(new FileWriter(outputDirectory + numberofAgentsFile));
			this.travelTimesRoute1 = new BufferedWriter(new FileWriter(outputDirectory + traveltimesroute1File));
			this.travelTimesRoute2 = new BufferedWriter(new FileWriter(outputDirectory + traveltimesroute2File));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeAgentsOnLinks(final Map<String, Integer> numberOfAgents) throws IOException {
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
	
	public void writeTravelTimesMainRoute(final double measuredTT, final double predTT) throws IOException {
		this.travelTimesRoute1.write(Double.toString(measuredTT));
		this.travelTimesRoute1.write(SEPARATOR);
		this.travelTimesRoute1.write(Double.toString(predTT));
		this.travelTimesRoute1.write(NEWLINE);
		this.travelTimesRoute1.flush();
	}

	public void writeTravelTimesAlternativeRoute(final double measuredTT, final double predTT) throws IOException {
		this.travelTimesRoute2.write(Double.toString(measuredTT));
		this.travelTimesRoute2.write(SEPARATOR);
		this.travelTimesRoute2.write(Double.toString(predTT));
		this.travelTimesRoute2.write(NEWLINE);
		this.travelTimesRoute2.flush();
	}
	
	public void close() {
		try {
			this.agentOnLinks.close();
			this.travelTimesRoute1.close();
			this.travelTimesRoute2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
