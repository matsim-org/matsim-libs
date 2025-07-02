/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkChangeEventsEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.messagequeue.Message;
import org.matsim.core.mobsim.messagequeue.MessageQueue;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.interfaces.TimeVariantLink;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;

import jakarta.inject.Inject;
import java.util.Queue;

class NetworkChangeEventsEngine implements NetworkChangeEventsEngineI {
	private static final Logger log = LogManager.getLogger( NetworkChangeEventsEngine.class ) ;

	private final MessageQueue messageQueue;
	private final Network network;
	private InternalInterface internalInterface;

	@Inject
	NetworkChangeEventsEngine(Network network, MessageQueue messageQueue) {
		this.network = network;
		this.messageQueue = messageQueue;
	}

	@Override
	public void onPrepareSim() {
		Queue<NetworkChangeEvent> changeEvents = NetworkUtils.getNetworkChangeEvents(this.network);
		for (final NetworkChangeEvent changeEvent : changeEvents) {
			addNetworkChangeEventToMessageQ(changeEvent);
		}
	}

	private void addNetworkChangeEventToMessageQ(NetworkChangeEvent changeEvent) {
		Message m = new Message(changeEvent.getStartTime()) {
			@Override
			public void handleMessage() {
				applyTheChangeEvent(changeEvent);
			}
		};
		this.messageQueue.putMessage(m);
	}

	private void applyTheChangeEvent(NetworkChangeEvent changeEvent) {
		for (Link link : changeEvent.getLinks()) {
			final NetsimLink netsimLink = this.internalInterface.getMobsim().getNetsimNetwork().getNetsimLink(link.getId());
			if ( netsimLink instanceof TimeVariantLink) {
				((TimeVariantLink) netsimLink).recalcTimeVariantAttributes();
			} else {
				throw new RuntimeException("link not time variant") ;
			}
		}
	}

	public final void addNetworkChangeEvent( NetworkChangeEvent event ) {
		log.warn("add within-day network change event:" + event);

		final Queue<NetworkChangeEvent> centralNetworkChangeEvents =
				NetworkUtils.getNetworkChangeEvents(this.internalInterface.getMobsim().getScenario().getNetwork());
		if ( centralNetworkChangeEvents.contains( event ) ) {
			log.warn("network change event already in central data structure; not adding it again") ;
		} else {
			log.warn("network change event not yet in central data structure; adding it") ;
			NetworkUtils.addNetworkChangeEvent(this.internalInterface.getMobsim().getScenario().getNetwork(), event);
			// need to add this here since otherwise speed lookup in mobsim does not work. And need to hedge against
			// code that may already have added it by itself.  kai, feb'18
		}

		if ( event.getStartTime()<= this.internalInterface.getMobsim().getSimTimer().getTimeOfDay() ) {
			this.applyTheChangeEvent(event);
		} else {
			this.addNetworkChangeEventToMessageQ(event);
		}

	}


	@Override
	public void afterSim() {

	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	@Override
	public void doSimStep(double time) {

	}
}
