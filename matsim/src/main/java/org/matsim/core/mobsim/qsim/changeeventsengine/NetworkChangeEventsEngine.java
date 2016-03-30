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

package org.matsim.core.mobsim.qsim.changeeventsengine;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.interfaces.TimeVariantLink;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;

import java.util.Collection;
import java.util.PriorityQueue;

/**
 * @author dgrether
 */
public class NetworkChangeEventsEngine implements MobsimEngine {
	
	private PriorityQueue<NetworkChangeEvent> networkChangeEventsQueue = null;
	private Netsim mobsim;

	@Override
	public void setInternalInterface( InternalInterface internalInterface ) {
		this.mobsim = (QSim) internalInterface.getMobsim();
	}

	@Override
	public void afterSim() {
		
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
	public void doSimStep(double time) {
		if ((this.networkChangeEventsQueue != null) && (this.networkChangeEventsQueue.size() > 0)) {
			handleNetworkChangeEvents(time);
		}
	}

	private void handleNetworkChangeEvents(final double time) {
		while ((this.networkChangeEventsQueue.size() > 0) && (this.networkChangeEventsQueue.peek().getStartTime() <= time)) {
			NetworkChangeEvent event = this.networkChangeEventsQueue.poll();
			for (Link link : event.getLinks()) {
				final NetsimLink netsimLink = this.mobsim.getNetsimNetwork().getNetsimLink(link.getId());
				if ( netsimLink instanceof TimeVariantLink ) {
					((TimeVariantLink) netsimLink).recalcTimeVariantAttributes();
				} else {
					throw new RuntimeException("link not time variant") ;
				}
			}
		}
	}

}
