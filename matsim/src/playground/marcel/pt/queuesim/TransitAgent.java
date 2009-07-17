/* *********************************************************************** *
 * project: org.matsim.*
 * TransitAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.pt.queuesim;

import org.matsim.core.mobsim.queuesim.PersonAgent;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.population.PersonImpl;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitStopFacility;

import playground.marcel.pt.routes.ExperimentalTransitRoute;

public class TransitAgent extends PersonAgent implements PassengerAgent {

	public TransitAgent(final PersonImpl p, final QueueSimulation simulation) {
		super(p, simulation);
	}

	public boolean arriveAtStop(final TransitStopFacility stop) {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) getCurrentLeg().getRoute();
		return route.getEgressStopId().equals(stop.getId());
	}

	public boolean ptLineAvailable(TransitLine line) {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) getCurrentLeg().getRoute();
		return line.getId().equals(route.getLineId());
	}

}
