/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkChangeEventsEngine
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

package playground.mzilske.d4d;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

/**
 * @author dgrether
 */
public class SightingsEngine implements MobsimEngine {

	private PriorityQueue<Sighting> sightingsQueue;
	// private Netsim mobsim;
	private AgentLocator agentLocator;



	public SightingsEngine(Map<Id, List<Sighting>> sightings, AgentLocator agentLocator) {
		this.agentLocator = agentLocator;
		this.sightingsQueue = new PriorityQueue<Sighting>(sightings.size(), new Sighting.StartTimeComparator());
		for (Collection<Sighting> sightings2 : sightings.values()) {
			this.sightingsQueue.addAll(sightings2);
		}
	}


	@Override
	public void setInternalInterface( InternalInterface internalInterface ) {
		// this.mobsim = internalInterface.getMobsim();
	}

	@Override
	public void afterSim() {

	}

	@Override
	public void onPrepareSim() {

	}

	@Override
	public void doSimStep(double time) {
		handleNetworkChangeEvents(time);
	}

	private void handleNetworkChangeEvents(final double time) {
		while ((this.sightingsQueue.size() > 0) && (this.sightingsQueue.peek().getTime() <= time)) {
			Sighting event = this.sightingsQueue.poll();
			Id linkId = agentLocator.locations.get(event.getAgentId());
			if (linkId != null) { // vielleicht sind wir noch nie gesehen worden
				
			}
		}
	}

}
