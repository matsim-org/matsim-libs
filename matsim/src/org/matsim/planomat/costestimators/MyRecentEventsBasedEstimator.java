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

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicAgentDepartureEvent;
import org.matsim.api.basic.v01.events.handler.BasicAgentArrivalEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentDepartureEventHandler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;

/**
 * @author meisterk
 *
 * @deprecated This estimator is deprecated because it leads to wrong results, and it is referenced nowhere. 
 * It's (possibly wrong) results were reported in STRC and IATBR papers of 2006.
 */
@Deprecated
public class MyRecentEventsBasedEstimator
implements LegTravelTimeEstimator, BasicAgentDepartureEventHandler, BasicAgentArrivalEventHandler {

	public void resetPlanSpecificInformation() {
	}

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

	public void handleEvent(final BasicAgentDepartureEvent event) {

		DepartureEvent depEvent = new DepartureEvent(event.getPersonId());

		this.departureEventsTimes.put(depEvent, event.getTime());
		this.departureEventsLinkIDs.put(depEvent, event.getLinkId());
	}

	public void handleEvent(final BasicAgentArrivalEvent event) {

		Id agentId = event.getPersonId();

		DepartureEvent removeMe = new DepartureEvent(agentId);

		Double departureTime = this.departureEventsTimes.remove(removeMe);
		Id departureLinkId = this.departureEventsLinkIDs.remove(removeMe);

		Double travelTime = event.getTime() - departureTime;

		LegTravelTimeEntry newLtte = new LegTravelTimeEntry(agentId, departureLinkId, event.getLinkId(), "car");
		this.legTravelTimeEstimations.put(newLtte, travelTime);
	}

	public double getLegTravelTimeEstimation(Id personId, double departureTime,
			ActivityImpl actOrigin, ActivityImpl actDestination,
			LegImpl legIntermediate, boolean doModifyLeg) {
		return this.legTravelTimeEstimations.get(new LegTravelTimeEntry(personId, actOrigin.getLinkId(), actDestination.getLinkId(), "car"));
	}

	public LegImpl getNewLeg(
			TransportMode mode, 
			ActivityImpl actOrigin,
			ActivityImpl actDestination,
			int legPlanElementIndex,
			double departureTime) {
		// not implemented here
		return null;
	}

	public void initPlanSpecificInformation(PlanImpl plan) {
		// not implemented here
	}

}
