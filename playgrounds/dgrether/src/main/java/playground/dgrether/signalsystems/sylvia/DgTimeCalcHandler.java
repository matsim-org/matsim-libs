/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.dgrether.signalsystems.sylvia;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.handler.LaneEnterEventHandler;
import org.matsim.core.utils.io.IOUtils;

public class DgTimeCalcHandler implements LinkEnterEventHandler, LinkLeaveEventHandler,
		AgentArrivalEventHandler, AgentDepartureEventHandler, LaneEnterEventHandler {

	private static final Logger log = Logger.getLogger(DgTimeCalcHandler.class);

	private Map<Id, Double> arrivaltimesSPN2FB;
	private Map<Id, Double> arrivaltimesCB2FB;

	private Map<Id, Double> arrivaltimesFB2SPN;
	private Map<Id, Double> arrivaltimesFB2CB;
	private Map<Id, Double> ttmap;
	private Set<Id> carsPassed;
	private Set<Id> wannabeadaptiveLanes;

	public DgTimeCalcHandler() {
		this.wannabeadaptiveLanes = new HashSet<Id>();
		this.fillWannaBes();
		this.reset(0);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Double agentTt = this.ttmap.get(event.getPersonId());
		this.ttmap.put(event.getPersonId(), agentTt - event.getTime());
	}

	@Override
	public void reset(int iteration) {
		this.ttmap = new TreeMap<Id, Double>();
		this.carsPassed = new HashSet<Id>();
		this.arrivaltimesFB2CB = new TreeMap<Id, Double>();
		this.arrivaltimesFB2SPN = new TreeMap<Id, Double>();
		this.arrivaltimesCB2FB = new TreeMap<Id, Double>();
		this.arrivaltimesSPN2FB = new TreeMap<Id, Double>();
		this.arrivaltimesFB2CB.put(new IdImpl(0), 0.0);
		this.arrivaltimesSPN2FB.put(new IdImpl(0), 0.0);
		this.arrivaltimesCB2FB.put(new IdImpl(0), 0.0);
		this.arrivaltimesFB2SPN.put(new IdImpl(0), 0.0);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Double agentTt = this.ttmap.get(event.getPersonId());
		this.ttmap.put(event.getPersonId(), agentTt + event.getTime());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Double agentTt = this.ttmap.get(event.getPersonId());

		this.ttmap.put(event.getPersonId(), agentTt + event.getTime());
		if (event.getPersonId().toString().endsWith("SPN_SDF")) {
			Double tr = this.arrivaltimesSPN2FB.get(event.getPersonId());

			if (tr == null) {
				this.arrivaltimesSPN2FB.put(event.getPersonId(), event.getTime());
			}
			else {
				this.arrivaltimesFB2SPN.put(event.getPersonId(), event.getTime());

			}
		}
		if (event.getPersonId().toString().endsWith("CB_SDF")) {
			Double tr = this.arrivaltimesCB2FB.get(event.getPersonId());
			if (tr == null) {
				this.arrivaltimesCB2FB.put(event.getPersonId(), event.getTime());
			}
			else {
				this.arrivaltimesFB2CB.put(event.getPersonId(), event.getTime());

			}
		}

	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		Double agentTt = this.ttmap.get(event.getPersonId());
		if (agentTt == null) {
			this.ttmap.put(event.getPersonId(), 0 - event.getTime());

		}
		else {
			this.ttmap.put(event.getPersonId(), agentTt - event.getTime());

		}

	}

	public Map<Id, Double> getTtmap() {
		return ttmap;
	}

	private void fillWannaBes() {
		// mock up adaptive lanes to create comparable travel times LSA-SLV
		for (int i = 2100; i < 2113; i++) { // Signalsystem 17
			this.wannabeadaptiveLanes.add(new IdImpl(i));
		}
		for (int i = 2000; i < 2013; i++) { // Signalsystem 18
			this.wannabeadaptiveLanes.add(new IdImpl(i));
		}
		for (int i = 1900; i < 1913; i++) { // Signalsystem 1
			this.wannabeadaptiveLanes.add(new IdImpl(i));
		}

	}

	@Override
	public void handleEvent(LaneEnterEvent event) {
		// if (this.ach.laneIsAdaptive(event.getLaneId()) & (!event.getLaneId().toString().endsWith(".ol")))
		// actually the nicer way

		if (this.wannabeadaptiveLanes.contains(event.getLaneId()))
			this.carsPassed.add(event.getPersonId());

	}

	public long getPassedAgents() {
		return this.carsPassed.size();
	}

	public Set<Id> getPassedCars() {
		return carsPassed;
	}

	public void exportArrivalTime(int iteration, String outdir) {
		String filename = outdir + iteration + ".arrivalTimesFromFB_CB.csv";
		this.exportMaptoCVS(this.arrivaltimesFB2CB, filename);
		filename = outdir + iteration + ".arrivalTimesFromFB_SPN.csv";
		this.exportMaptoCVS(this.arrivaltimesFB2SPN, filename);
		filename = outdir + iteration + ".arrivalTimes_CB_FB.csv";
		this.exportMaptoCVS(this.arrivaltimesCB2FB, filename);
		filename = outdir + iteration + ".arrivalTimes_SPN_FB.csv";
		this.exportMaptoCVS(this.arrivaltimesSPN2FB, filename);
		
		filename = outdir + iteration + ".latestArrivals.csv";
		this.exportLatestArrivals(filename);
	}

	private void exportLatestArrivals(String filename) {
		
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			writer.append("CB2FB;" + "SPN2FB;" + "FB2CB;" + "FB2SPN;");
			writer.newLine();
			
			writer.append(Collections.max(this.arrivaltimesCB2FB.values()) + ";"
					+ Collections.max(this.arrivaltimesSPN2FB.values()) + ";"
					+ Collections.max(this.arrivaltimesFB2CB.values()) + ";"
					+ Collections.max(this.arrivaltimesFB2SPN.values()) + ";");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void exportMaptoCVS(Map<Id, Double> atmap, String filename) {
		try {
			FileWriter writer = new FileWriter(filename);

			for (Entry<Id, Double> e : atmap.entrySet()) {
				writer.append(e.getKey().toString() + ";" + e.getValue() + ";" + "\n");
			}
			writer.flush();
			writer.close();
			log.info("Wrote " + filename);
		} catch (IOException e1) {
			log.error("cannot write to file: " + filename);
			e1.printStackTrace();
		}

	}

	public double getLatestArrivalCBSDF() {
		return Collections.max(this.arrivaltimesCB2FB.values()).doubleValue();

	}

	public double getLatestArrivalSPNSDF() {
		return Collections.max(this.arrivaltimesSPN2FB.values()).doubleValue();

	}

	public double getLatestArrivalSDFCB() {
		return Collections.max(this.arrivaltimesFB2CB.values()).doubleValue();

	}

	public double getLatestArrivalSDFSPN() {
		return Collections.max(this.arrivaltimesFB2SPN.values()).doubleValue();

	}

	public int getAverageTravelTime() {

		Double att = 0.0;
		for (Entry<Id, Double> entry : ttmap.entrySet()) {
			att += entry.getValue();
		}
		att = att / ttmap.size();
		return att.intValue();
	}

	public int getAverageAdaptiveTravelTime() {
		if (this.getPassedAgents() == 0)
			return 0;
		Double att = 0.0;
		for (Entry<Id, Double> entry : ttmap.entrySet()) {
			if (this.getPassedCars().contains(entry.getKey())) {
				att += entry.getValue();
			}
		}
		att = att / this.getPassedAgents();
		return att.intValue();

	}
}
