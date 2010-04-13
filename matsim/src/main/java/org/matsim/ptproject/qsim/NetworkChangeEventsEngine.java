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
package org.matsim.ptproject.qsim;

import java.util.Collection;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;


/**
 * @author dgrether
 *
 */
public class NetworkChangeEventsEngine implements SimEngine {

	
	private PriorityQueue<NetworkChangeEvent> networkChangeEventsQueue = null;
	private QSim qsim;

	
	public NetworkChangeEventsEngine(QSim sim){
		this.qsim = sim;
	}
	
	@Override
	public void afterSim() {
		
	}

	@Override
	public QSim getQSim() {
		return this.qsim;
	}

	@Override
	public void onPrepareSim() {
		Collection<NetworkChangeEvent> changeEvents = ((NetworkImpl)this.qsim.getScenario().getNetwork()).getNetworkChangeEvents();
		if ((changeEvents != null) && (changeEvents.size() > 0)) {
			this.networkChangeEventsQueue = new PriorityQueue<NetworkChangeEvent>(changeEvents.size(), new NetworkChangeEvent.StartTimeComparator());
			this.networkChangeEventsQueue.addAll(changeEvents);
		}
	}

	public void onBeforeSimStep(double time) {
		if ((this.networkChangeEventsQueue != null) && (this.networkChangeEventsQueue.size() > 0)) {
			handleNetworkChangeEvents(time);
		}
	}

	private void handleNetworkChangeEvents(final double time) {
		while ((this.networkChangeEventsQueue.size() > 0) && (this.networkChangeEventsQueue.peek().getStartTime() <= time)){
			NetworkChangeEvent event = this.networkChangeEventsQueue.poll();
			for (Link link : event.getLinks()) {
				this.getQSim().getQNetwork().getQLink(link.getId()).recalcTimeVariantAttributes(time);
			}
		}
	}

}
