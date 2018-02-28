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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNetwork;
import org.matsim.core.mobsim.qsim.interfaces.TimeVariantLink;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * @author dgrether
 */
public final class NetworkChangeEventsEngine implements NetworkChangeEventsEngineI {
	private static final Logger log = Logger.getLogger(NetworkChangeEventsEngine.class) ;
	
	private Queue<NetworkChangeEvent> networkChangeEventsQueue = null;
	final private Network network;
	final private NetsimNetwork netsimNetwork;
	
	@Inject
	public NetworkChangeEventsEngine(Network network, NetsimNetwork netsimNetwork) {
		this.network = network;
		this.netsimNetwork = netsimNetwork;
	}

	/*@Override
	public void setInternalInterface( InternalInterface internalInterface ) {
		//this.mobsim = (QSim) internalInterface.getMobsim();
	}*/

	public static NetworkChangeEventsEngineI createNetworkChangeEventsEngine(Network network, NetsimNetwork netsimNetwork) {
		return new NetworkChangeEventsEngine(network, netsimNetwork);
	}

	@Override
	public void afterSim() {
		
	}

	@Override
	public void onPrepareSim() {
		Queue<NetworkChangeEvent> changeEvents = NetworkUtils.getNetworkChangeEvents(network);
		if ((changeEvents != null) && (changeEvents.size() > 0)) {
			this.networkChangeEventsQueue = new PriorityQueue<>(changeEvents.size(), new NetworkChangeEvent.StartTimeComparator());
			this.networkChangeEventsQueue.addAll(changeEvents);
		}

		// one could replace the above lines by
//		this.networkChangeEventsQueue = NetworkUtils.getNetworkChangeEvents(this.mobsim.getScenario().getNetwork()) ;
		// This also passes at least the core tests.  However, the code below "consumes" the network change events queue.
		// It seems that we should rather not do this.

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
			handleNetworkChangeEvent(event);
		}
	}
	
	public final void addNetworkChangeEvent( NetworkChangeEvent event ) {
		// used (and thus implicitly tested) by bdi-abm-integration project.  A separate core test would be good. kai, feb'18
		
		log.warn("add change event coming from external (i.e. not in network change events data structure):" + event);
		this.networkChangeEventsQueue.add(event);
		final Queue<NetworkChangeEvent> centralNetworkChangeEvents = NetworkUtils.getNetworkChangeEvents(network);
		if ( !centralNetworkChangeEvents.contains( event ) ) {
			centralNetworkChangeEvents.add( event ) ;
			// need to add this here since otherwise speed lookup in mobsim does not work. And need to hedge against
			// code that may already have added it by itself.  kai, feb'18
		}

		handleNetworkChangeEvent(event);
	}
	
	private void handleNetworkChangeEvent(NetworkChangeEvent event) {
		for (Link link : event.getLinks()) {
			final NetsimLink netsimLink = netsimNetwork.getNetsimLink(link.getId());
			if ( netsimLink instanceof TimeVariantLink) {
				((TimeVariantLink) netsimLink).recalcTimeVariantAttributes();
			} else {
				throw new RuntimeException("link not time variant") ;
			}
		}
	}
	
//	public void rereadNetworkChangeEvents() {
		// not sure if this is the way to go since this would mean re-reading everything from the beginning.  Could instead just
		// insert it.  Issue then is that it needs to be inserted both int
		// (1) networkChangEventsQueue of present class
		// (2) networkChangeEvents data of scenario
		// Reason is that (1) only only triggers the reCalcTimeDep method in the QSim, but the actual new values
		// are in (2). kai, feb'18
		// --> yy might consider to get rid of (2), since (1) is now also time-sorted (since sometimes 2017/18). kai, feb'18

		// Note that it _would_ be possible to add flowCap/nLanes change events into the mobsim w/o adding them to the central
		// network change events, since those are pushed into the mobsim.  It would, however, not work with freeSpeed change events,
		// since these are pulled from the mobsim.  For consistency, thus, I opt for running all added network change events through
		// the central network change events.  kai, feb'18
		
//		throw new RuntimeException(Gbl.NOT_IMPLEMENTED ) ;
//	}
}
