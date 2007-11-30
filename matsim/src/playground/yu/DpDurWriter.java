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
package playground.yu;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.utils.io.IOUtils;

/**
 * creats a matrix (departureTime, Duration, volume)
 * 
 * @author ychen
 * 
 */
public class DpDurWriter implements EventHandlerAgentDepartureI, EventHandlerAgentArrivalI {
	private int maxDur = 0;

	private BufferedWriter out = null;

	/**
	 * @param args0 -
	 *            agentId;
	 * @param i -
	 *            departureTime;
	 */
	private HashMap<String, Integer> agentDepTimes;

	/**
	 * @param args0 -
	 *            departureTime
	 * @param list -
	 *            a list of volume with certain travel- duration
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
		String fileHead = "\t";
		for (int i = 0; i <= maxDur / 60; i += 5) {
			fileHead += Integer.toString(i) + "\t";
		}// "5" --> 5min ~ 9min59s
		writeLine(fileHead);
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

	public void reset(int iteration) {
		closefile();
	}

	public void closefile() {
		if (this.out != null) {
			try {
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void handleEvent(EventAgentDeparture event) {
		String agentId=event.agentId;
		agentDepTimes.put(agentId, (int) event.time);
	}

	public void handleEvent(EventAgentArrival event) {
		String agentId=event.agentId;
		if (agentDepTimes.containsKey(agentId)) {
			int depT = agentDepTimes.remove(agentId);
			int depH = depT / 3600;
			int depM = (depT - depH * 3600) / 60;
			int arrT = (int) event.time;
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
}
