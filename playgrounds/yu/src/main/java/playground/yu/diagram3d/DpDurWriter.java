/* *********************************************************************** *
 * project: org.matsim.*
 * DpDurWriter.java
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
/**
 *
 */
package playground.yu.diagram3d;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.io.IOUtils;

/**
 * creats a matrix (departureTime, Duration, volume)
 *
 * @author ychen
 *
 */
public class DpDurWriter implements AgentDepartureEventHandler,
		AgentArrivalEventHandler {
	private int maxDur = 0;

	private BufferedWriter out = null;

	/**
	 * @param args0
	 *            - agentId;
	 * @param i
	 *            - departureTime;
	 */
	private HashMap<String, Integer> agentDepTimes;

	/**
	 * @param args0
	 *            - departureTime
	 * @param list
	 *            - a list of volume with certain travel- duration
	 */
	private HashMap<String, ArrayList<Integer>> dpDurVol;

	/**
	 * @param filename
	 */
	public DpDurWriter(String filename) {
		init(filename);
	}

	public void init(String outfilename) {
		if (this.out != null) {
			try {
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			this.out = IOUtils.getBufferedWriter(outfilename);
		} catch (IOException e) {
			e.printStackTrace();
		}
		agentDepTimes = new HashMap<String, Integer>();
		dpDurVol = new HashMap<String, ArrayList<Integer>>();
	}

	public void writeMatrix() {
		Set<String> dpTSet = dpDurVol.keySet();
		StringBuilder fileHead = new StringBuilder("\t");
		for (int i = 0; i <= maxDur / 60; i += 5) {
			fileHead.append(i + "\t");
		}// "5" --> 5min ~ 9min59s
		writeLine(fileHead.toString());
		String line = "";
		for (String dpT : dpTSet) {
			line = "";
			line += dpT + "\t";
			ArrayList<Integer> al = dpDurVol.get(dpT);
			for (Integer i : al) {
				line += i + "\t";
			}
			writeLine(line);
		}// dpT : 7:05 --> 7:05:00 ~ 7:09:59
	}

	private void writeLine(String line) {
		try {
			this.out.write(line + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reset(int iteration) {
		closeFile();
	}

	public void closeFile() {
		if (this.out != null) {
			try {
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		String agentId = event.getPersonId().toString();
		agentDepTimes.put(agentId, (int) event.getTime());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		String agentId = event.getPersonId().toString();
		if (agentDepTimes.containsKey(agentId)) {
			int depT = agentDepTimes.remove(agentId);
			int depH = depT / 3600;
			int depM = (depT - depH * 3600) / 60;
			int arrT = (int) event.getTime();
			int dur = arrT - depT;
			if (dur > maxDur) {
				maxDur = dur;
			}
			String depTId = Integer.toString(depH) + ":"
					+ Integer.toString((depM / 5) * 5);
			ArrayList<Integer> al = new ArrayList<Integer>();
			if (dpDurVol.containsKey(depTId)) {
				al = dpDurVol.get(depTId);
			} else {
				dpDurVol.put(depTId, al);
			}
			if (dur / 300 >= al.size()) {
				for (int i = al.size(); i <= dur / 300; i++) {
					al.add(0);
				}
			}
			al.set(dur / 300, al.get(dur / 300).intValue() + 1);
		}
	}

	public static void main(String[] args) {
		Config config = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(args[0]).loadScenario().getConfig();
		DpDurWriter ddw = new DpDurWriter("DpDurMatrix.txt");

		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(ddw);// TODO ...

		System.out.println("@reading the eventsfile (TXTv1) ...");
		new MatsimEventsReader(events).readFile(null /*filename not specified*/);
		System.out.println("@done.");

		ddw.writeMatrix();
		ddw.closeFile();
	}
}
