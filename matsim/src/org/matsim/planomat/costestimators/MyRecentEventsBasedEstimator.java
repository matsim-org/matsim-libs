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
import org.matsim.basic.v01.IdImpl;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Route;
import org.matsim.world.Location;

public class MyRecentEventsBasedEstimator
implements LegTravelTimeEstimator, AgentDepartureEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler {

	public MyRecentEventsBasedEstimator() {
		super();
	}

	private final HashMap<DepartureEvent, Double> departureEventsTimes = new HashMap<DepartureEvent, Double>();
	private final HashMap<DepartureEvent, Id> departureEventsLinkIDs = new HashMap<DepartureEvent, Id>();

	private static class LegTravelTimeEntry {

		private final Id agentId;
		private final Id originLocationId;
		private final Id destinationLocationId;
		private final String mode;

		public LegTravelTimeEntry(final Id agentId, final Id originLocationId, final Id destinationLocationId, final String mode) {
			super();
			this.agentId = agentId;
			this.originLocationId = originLocationId;
			this.destinationLocationId = destinationLocationId;
			this.mode = mode;
		}

		@Override
		public boolean equals(final Object arg0) {
			if (!(arg0 instanceof LegTravelTimeEntry)) return false;

			LegTravelTimeEntry entry = (LegTravelTimeEntry)arg0;

			return
				(this.agentId.equals(entry.agentId)) &&
				(this.originLocationId.equals(entry.originLocationId)) &&
				(this.destinationLocationId.equals(entry.destinationLocationId)) &&
				(this.mode.equals(entry.mode));
		}

		@Override
		public int hashCode() {

			// TODO [KM] I'm sure the computation of this is very expensive (string concatenation), and should be replaced.

			// Well, as all the members are final, why not calculate the hashCode once and cache it? -marcel/26feb08

			return (this.agentId.toString() + this.originLocationId.toString() + this.destinationLocationId + this.mode).hashCode();
		}

		@Override
		public String toString() {
			return "[ agentId = " + this.agentId +
					" ][ " + this.originLocationId +
					" ][ " + this.destinationLocationId +
					" ][ " + this.mode + " ]";
		}

	}

	private final HashMap<LegTravelTimeEntry, Double> legTravelTimeEstimations = new HashMap<LegTravelTimeEntry, Double>();

	public void reset(final int iteration) {
	}

	public void handleEvent(final AgentDepartureEvent event) {

		DepartureEvent depEvent = new DepartureEvent(new IdImpl(event.agentId), event.legId);

		this.departureEventsTimes.put(depEvent, event.time);
		this.departureEventsLinkIDs.put(depEvent, new IdImpl(event.linkId));
	}

	public void handleEvent(final AgentArrivalEvent event) {

		IdImpl agentId = new IdImpl(event.agentId);

		DepartureEvent removeMe = new DepartureEvent(agentId, event.legId);

		Double departureTime = this.departureEventsTimes.remove(removeMe);
		Id departureLinkId = this.departureEventsLinkIDs.remove(removeMe);

		Double travelTime = event.time - departureTime;

		LegTravelTimeEntry newLtte = new LegTravelTimeEntry(agentId, departureLinkId, new IdImpl(event.linkId), "car");
		this.legTravelTimeEstimations.put(newLtte, travelTime);
	}

	public void handleEvent(final AgentStuckEvent event) {
	}

	public double getLegTravelTimeEstimation(Id personId, double departureTime,
			Act actOrigin, Act actDestination, Leg legIntermediate) {

		return this.legTravelTimeEstimations.get(new LegTravelTimeEntry(personId, actOrigin.getLinkId(), actDestination.getLinkId(), "car"));
	
	}

}
