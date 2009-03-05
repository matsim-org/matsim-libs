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

import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;

/**
 * counts Agents in network for every timeBin
 * 
 * @author ychen
 * 
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.demandmodeling.events.handler.EventHandlerLinkEnterI#handleEvent
	 * (org.matsim.demandmodeling.events.EventLinkEnter)
	 */
	public void handleEvent(LinkEnterEvent event) {
		// TODO save entertime into agentTimer
		String agentId = event.agentId;
		if (!agentTimer.containsKey(agentId))
			agentTimer.put(agentId, event.time);
		else
			System.err
					.println("error: a left link event of this agent dispears!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.demandmodeling.events.handler.EventHandlerLinkLeaveI#handleEvent
	 * (org.matsim.demandmodeling.events.EventLinkLeave)
	 */
	public void handleEvent(LinkLeaveEvent event) {
		String agentId = event.agentId;
		if (agentTimer.containsKey(agentId)) {
			for (int tbIdx = agentTimer.remove(agentId).intValue(); tbIdx <= event.time; tbIdx++) {
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
		try {
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(new File(
							fileName))));
			out.writeBytes("timeBin\tVolume of the network");
			for (Integer tbIdx : netVols.keySet())
				out.writeBytes(tbIdx.toString() + "\t"
						+ netVols.get(tbIdx).toString());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		final String netFilename = "./equil/equil_net.xml";
		final String plansFilename = "./equil/equil_plans.xml";
		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();
		PopulationReader plansReader = new MatsimPopulationReader(population,
				network);
		plansReader.readFile(plansFilename);

		Events events = new Events();

		TraVolCnter traVolCounter = new TraVolCnter();
		events.addHandler(traVolCounter);

		QueueSimulation sim = new QueueSimulation(network, population, events);
		sim.run();
	}
}
