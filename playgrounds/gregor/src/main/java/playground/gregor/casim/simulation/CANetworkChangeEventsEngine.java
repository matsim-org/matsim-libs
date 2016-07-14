/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.gregor.casim.simulation;

import java.util.Collection;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkImpl;

import playground.gregor.casim.simulation.physics.CALink;
import playground.gregor.casim.simulation.physics.CAMultiLaneLink;

public class CANetworkChangeEventsEngine implements MobsimEngine {

	private static final Logger log = Logger.getLogger(CANetworkChangeEventsEngine.class);
	
	private QSim mobsim;
	private PriorityQueue<NetworkChangeEvent> networkChangeEventsQueue;
	private CANetsimEngine cae;

	public CANetworkChangeEventsEngine(CANetsimEngine cae) {
		this.cae = cae;
	}


	@Override
	public void doSimStep(double time) {
		if ((this.networkChangeEventsQueue != null) && (this.networkChangeEventsQueue.size() > 0)) {
			handleNetworkChangeEvents(time);
		}
	}
	
	
	private void handleNetworkChangeEvents(final double time) {
		while ((this.networkChangeEventsQueue.size() > 0) && (this.networkChangeEventsQueue.peek().getStartTime() <= time)) {
			NetworkChangeEvent event = this.networkChangeEventsQueue.poll();
			if (event.getFreespeedChange() == null || event.getFreespeedChange().getType() == ChangeType.FACTOR) {
				log.warn("Unsupported network change event! Only freespeed change with absolut value is allowed here");
			}
			
			for (Link link : event.getLinks()) {
				CALink caLink = this.cae.getCANetwork().getCALink(link.getId());
				if (caLink instanceof CAMultiLaneLink && event.getFreespeedChange() != null) {
					((CAMultiLaneLink)caLink).changeFreeSpd(event.getFreespeedChange().getValue());
				}
			}
		}
	}

	@Override
	public void onPrepareSim() {
		Collection<NetworkChangeEvent> changeEvents = ((NetworkImpl)this.mobsim.getScenario().getNetwork()).getNetworkChangeEvents();
		if ((changeEvents != null) && (changeEvents.size() > 0)) {
			this.networkChangeEventsQueue = new PriorityQueue<>(changeEvents.size(), new NetworkChangeEvent.StartTimeComparator());
			this.networkChangeEventsQueue.addAll(changeEvents);
		}

	}

	@Override
	public void afterSim() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.mobsim = (QSim) internalInterface.getMobsim();

	}

}
