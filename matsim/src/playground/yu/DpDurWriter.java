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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.matsim.config.Config;
import org.matsim.events.BasicEvent;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.gbl.Gbl;

/**
 * @author ychen
 * 
 */
public class DpDurWriter extends EventWriterTXT {
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
		super(filename);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.events.algorithms.EventWriterTXT#handleEvent(org.matsim.events.BasicEvent)
	 */
	@Override
	public void handleEvent(BasicEvent event) {
		String agentId = event.agentId;
		if (event instanceof EventAgentDeparture) {
			if (Integer.parseInt(event.getAttributes().getValue("leg")) == 0)
				agentDepTimes.put(agentId, (int) event.time);
		} else if (event instanceof EventAgentArrival
				&& agentDepTimes.containsKey(agentId)) {

			int depT = agentDepTimes.remove(agentId);
			int depH = depT / 3600;
			int depM = (depT - depH * 3600) / 60;
			// int depS = depT - depH * 3600 - depM * 60;

			int arrT = (int) event.time;
			int arrH = arrT / 3600;
			int arrM = (arrT - arrH * 3600) / 60;
			// int arrS = arrT - arrH * 3600 - arrM * 60;

			int dur = arrT - depT;

			String depTId = Integer.toString(depH)+":"
					+ Integer.toString((depM / 5) * 5);
			ArrayList<Integer> al = new ArrayList<Integer>();
			if (dpDurVol.containsKey(depTId)) {
				al = dpDurVol.get(depTId);
			} else {
				for (int i = 0; i < 36; i++) {
					al.add(0);
				}
				dpDurVol.put(depTId, al);
			}
			al.set(dur / 300, al.get(dur / 300).intValue() + 1);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.events.algorithms.EventWriterTXT#init(java.lang.String)
	 */
	@Override
	public void init(String outfilename) {
		super.init(outfilename);
		agentDepTimes = new HashMap<String, Integer>();
		dpDurVol = new HashMap<String, ArrayList<Integer>>();
	}

	public void writeMatrix() {
		Set<String> dpTSet = dpDurVol.keySet();
		String fileHead = "\t";
		for (int i = 5; i <= 180; i += 5) {
			fileHead += Integer.toString(i) + "\t";
		}
		writeLine(fileHead);
		String line = "";
		for (String dpT : dpTSet) {
			line="";
			line += dpT + "\t";
			ArrayList<Integer> al = dpDurVol.get(dpT);
			for (Integer i : al) {
				line += i + "\t";
			}
			writeLine(line);
		}
	}
}
