/* *********************************************************************** *
 * project: org.matsim.*
 * TraVolCnter.java
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

package playground.yu.volCount;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;

/**
 * counts Agents in network for every timeBin
 * 
 * @author ychen
 */
public class TraVolCnter implements LinkEnterEventHandler,
		LinkLeaveEventHandler {
	/**
	 * netVols<tIndex, netVol>
	 */
	private final HashMap<Integer, Integer> netVols = new HashMap<Integer, Integer>();

	/**
	 * agentTimer<agentId, enterTime>
	 */
	private final HashMap<String, Double> agentTimer = new HashMap<String, Double>();

	public void handleEvent(LinkEnterEvent event) {
		// TODO save entertime into agentTimer
		String agentId = event.getPersonId().toString();
		if (!agentTimer.containsKey(agentId))
			agentTimer.put(agentId, event.getTime());
		else
			System.err
					.println("error: a left link event of this agent dispears!");
	}

	public void handleEvent(LinkLeaveEvent event) {
		String agentId = event.getPersonId().toString();
		if (agentTimer.containsKey(agentId)) {
			for (int tbIdx = agentTimer.remove(agentId).intValue(); tbIdx <= event
					.getTime(); tbIdx++) {
				Integer vol = netVols.get(tbIdx);
				if (vol == null)
					vol = 0;
				netVols.put(tbIdx, vol++);
			}
		}
	}

	public void reset(int iteration) {
		agentTimer.clear();
		netVols.clear();
	}

	public void write(String fileName) {
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(fileName))));
			out.writeBytes("timeBin\tVolume of the network");
			for (Entry<Integer, Integer> tbIdxEntry : netVols.entrySet())
				out.writeBytes(tbIdxEntry.getKey() + "\t"
						+ tbIdxEntry.getValue());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public static void main(final String[] args) {
		final String netFilename = "./equil/equil_net.xml";
		final String plansFilename = "./equil/equil_plans.xml";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(plansFilename);

		EventsManagerImpl events = new EventsManagerImpl();

		TraVolCnter traVolCounter = new TraVolCnter();
		events.addHandler(traVolCounter);

		QueueSimulation sim = new QueueSimulation(scenario, events);
		sim.run();
	}
}
