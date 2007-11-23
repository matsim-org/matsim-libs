/* *********************************************************************** *
 * project: org.matsim.*
 * MyRecentEventsBasedEstimator.java
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

package org.matsim.planomat.costestimators;

import java.util.HashMap;

import org.matsim.basic.v01.Id;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventAgentStuck;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerAgentStuckI;
import org.matsim.plans.Route;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.Location;

public class MyRecentEventsBasedEstimator 
implements LegTravelTimeEstimator, EventHandlerAgentDepartureI, EventHandlerAgentArrivalI, EventHandlerAgentStuckI {

	private static final String MSG_PREFIX = "[ MyRecentEventsBasedEstimator ] ";
	
	public MyRecentEventsBasedEstimator() {
		super();
	}

	private HashMap<DepartureEvent, Double> departureEventsTimes = new HashMap<DepartureEvent, Double>();
	private HashMap<DepartureEvent, IdI> departureEventsLinkIDs = new HashMap<DepartureEvent, IdI>();

	private class LegTravelTimeEntry {
		
		private IdI agentId;
		private IdI originLocationId;
		private IdI destinationLocationId;
		private String mode;
		
		public LegTravelTimeEntry(IdI agentId, IdI originLocationId, IdI destinationLocationId, String mode) {
			super();
			this.agentId = agentId;
			this.originLocationId = originLocationId;
			this.destinationLocationId = destinationLocationId;
			this.mode = mode;
		}

		@Override
		public boolean equals(Object arg0) {

			LegTravelTimeEntry entry = (LegTravelTimeEntry)arg0;
			
			return 
				(this.agentId.equals(entry.agentId)) &&
				(this.originLocationId.equals(entry.originLocationId)) &&
				(this.destinationLocationId.equals(entry.destinationLocationId)) &&
				(this.mode.equals(entry.mode));
		}

		@Override
		public int hashCode() {

			// I'm sure the computation of this is very expensive (string concatenation), and should be replaced.
			
			return new String(this.agentId.toString() + this.originLocationId.toString() + this.destinationLocationId + this.mode).hashCode();
		}

		@Override
		public String toString() {
			return new String(
					"[ agentId = " + this.agentId + 
					" ][ " + this.originLocationId + 
					" ][ " + this.destinationLocationId + 
					" ][ " + this.mode + " ]");
		}
		
	}
	
	private HashMap<LegTravelTimeEntry, Double> legTravelTimeEstimations = new HashMap<LegTravelTimeEntry, Double>();
	
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	public double getLegTravelTimeEstimation(
			IdI personId,
			double departureTime, 
			Location origin, 
			Location destination,
			Route route,
			String mode) {
//		System.out.println(personId + " " + origin.getId() + " " + destination.getId());
		return legTravelTimeEstimations.get(new LegTravelTimeEntry(personId, origin.getId(), destination.getId(), "car"));
	}

	public void handleEvent(EventAgentDeparture event) {

		DepartureEvent depEvent = new DepartureEvent(
				new Id(event.agentId), 
				event.getAttributes().getValue("leg"));

		departureEventsTimes.put(depEvent, event.time);
		departureEventsLinkIDs.put(depEvent, new Id(event.linkId));
//		System.out.println(MSG_PREFIX + "Added one. Num of dep events: " + departureEventsTimes.size());

	}

	public void handleEvent(EventAgentArrival event) {

		Id agentId = new Id(event.agentId);
		String legId = event.getAttributes().getValue("leg");

		DepartureEvent removeMe = new DepartureEvent(agentId, legId);

		Double departureTime = departureEventsTimes.remove(removeMe);
		Id departureLinkID = (Id) departureEventsLinkIDs.remove(removeMe);

		Double travelTime = event.time - departureTime;

		LegTravelTimeEntry newLtte = new LegTravelTimeEntry(agentId, departureLinkID, new Id(event.linkId), "car");
		this.legTravelTimeEstimations.put(newLtte, travelTime);
//		System.out.println(MSG_PREFIX + "Added the following travel time entry: " + newLtte.toString() + ": " + travelTime + "sec.");
//		System.out.println("Num of travel time entries: " + this.legTravelTimeEstimations.size());
		
	}

	public void handleEvent(EventAgentStuck event) {
		// TODO Auto-generated method stub

	}

}
