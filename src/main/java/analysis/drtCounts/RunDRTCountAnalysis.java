/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
  
package analysis.drtCounts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

public class RunDRTCountAnalysis {
public static void main(String[] args) throws IOException {
	
	String folder = "D:/Scenario_1/matsim_output/at_case_car23cpm/";
	
	Network network = NetworkUtils.createNetwork();
	new MatsimNetworkReader(network).readFile(folder+"output_network.xml.gz");
	
	EventsManager events = EventsUtils.createEventsManager();
	
	CountsAccumulator countsAccumulator = new CountsAccumulator(network);
	events.addHandler(countsAccumulator);
	new MatsimEventsReader(events).readFile(folder+"output_events.xml.gz");
	BufferedWriter bw = IOUtils.getBufferedWriter(folder+"output_linkVolumes.txt");
	bw.write("LinkId;carCount;drtCount");
	for (Entry<Id<Link>, Tuple<MutableInt, MutableInt>> e : countsAccumulator.getLinkCounts().entrySet()) {
		bw.newLine();
		bw.write(e.getKey().toString()+";"+e.getValue().getFirst().intValue()+";"+e.getValue().getSecond().intValue());
	}
	
	bw.flush();
	bw.close();
	
}



}

class CountsAccumulator implements LinkEnterEventHandler, ActivityEndEventHandler{

	private Set<Id<Vehicle>> drts = new HashSet<>();	
	private Map<Id<Link>,Tuple<MutableInt,MutableInt>> linkCounts = new HashMap<>();
	
	public CountsAccumulator(Network network) {
		for (Link l  : network.getLinks().values()) {
			if (l.getAllowedModes().contains(TransportMode.car)) {
				linkCounts.put(l.getId(), new Tuple<MutableInt, MutableInt>(new MutableInt(), new MutableInt()));
			}
		}
	}


	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (linkCounts.containsKey(event.getLinkId())) {
		if (drts.contains(event.getVehicleId())) {
			linkCounts.get(event.getLinkId()).getSecond().increment();
		} else {
			linkCounts.get(event.getLinkId()).getFirst().increment();
		}
		}
		
	}


	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) {
			drts.add(Id.createVehicleId(event.getPersonId()));
		}		
	}
	public Map<Id<Link>, Tuple<MutableInt, MutableInt>> getLinkCounts() {
		return linkCounts;
	}

	}
