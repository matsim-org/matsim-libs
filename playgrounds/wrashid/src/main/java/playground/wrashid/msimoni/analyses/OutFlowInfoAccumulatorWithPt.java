/* *********************************************************************** *
 * project: org.matsim.*
 * OutFlowInfoAccumulatorWithPt.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.wrashid.msimoni.analyses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

public class OutFlowInfoAccumulatorWithPt implements LinkLeaveEventHandler,
		PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler,
		VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler() ;

	private final int binSizeInSeconds; // set the length of interval
	private final int numBins;
	
	public HashMap<Id, int[]> linkOutFlow;
	private Map<Id<Link>, ? extends Link> filteredEquilNetLinks;
	private Map<Id, Integer> leavesPerLink;
	private Set<Id> carAgents;
	
	public OutFlowInfoAccumulatorWithPt(Map<Id<Link>, ? extends Link> filteredEquilNetLinks,
			int binSizeInSeconds) { 
		// to create the class FlowInfoCollector and give the link set
		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.binSizeInSeconds = binSizeInSeconds;
		this.numBins = (86400 / this.binSizeInSeconds) + 1;
	}

	@Override
	public void reset(int iteration) {
		delegate.reset(iteration);
		linkOutFlow = new HashMap<Id, int[]>();
		leavesPerLink = new HashMap<Id, Integer>();
		carAgents = new HashSet<Id>();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> driverId = delegate.getDriverOfVehicle( event.getVehicleId() ) ;
		
		// ignore non-car travelers
		if (!this.carAgents.contains(driverId)) return;
		
		// call from NetworkReadExample
		linkLeave(event.getLinkId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) carAgents.add(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		
		// if the agent can be removed from the set, it was a car traveler
		if (this.carAgents.remove(event.getPersonId())) {
			linkLeave(event.getLinkId(), event.getTime());			
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		// nothing to do here. link leave events are created for stuck agents 
//		linkLeave(event.getLinkId(), event.getTime());		
	}
	
	private void linkLeave(Id linkId, double time) {
		
		if (time > 86400) return;
		
		if (!filteredEquilNetLinks.containsKey(linkId)) {
			return; // if the link is not in the link set, then exit the method
		}

		if (!linkOutFlow.containsKey(linkId)) {
			// set the number of intervals
			linkOutFlow.put(linkId, new int[numBins]); 
		}

		int[] bins = linkOutFlow.get(linkId);

		int binIndex = (int) Math.round(Math.floor(time / binSizeInSeconds));

//		// count the number of agents for each link at each time interval
//		bins[binIndex] = bins[binIndex] + 1;
		
		Integer leftVehicles = this.leavesPerLink.get(linkId);
		if (leftVehicles == null) leftVehicles = 1;
		else leftVehicles++;
		this.leavesPerLink.put(linkId, leftVehicles);
		
		// count the number of agents entered the link so far
		for (int i = binIndex; i < numBins; i++) bins[i] = leftVehicles;
	}

	public void printLinkFlows() { // print
		for (Id linkId : linkOutFlow.keySet()) {
			int[] bins = linkOutFlow.get(linkId);

			Link link = filteredEquilNetLinks.get(linkId);

			boolean hasTraffic = false;
			for (int i = 0; i < bins.length; i++) {
				if (bins[i] != 0.0) {
					hasTraffic = true;
					break;
				}
			}

			if (hasTraffic) {
				System.out.print(linkId + " - " + link.getCoord() + ": \t");

				for (int i = 0; i < bins.length; i++) {
					System.out.print(bins[i] * 3600 / binSizeInSeconds + "\t");
				}

				System.out.println();
			}
		}
	}

	public HashMap<Id, int[]> getLinkOutFlow() {
		return linkOutFlow;
	}

	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

}