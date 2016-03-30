package org.matsim.core.mobsim.qsim.changeeventsengine;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.jdeqsim.Message;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.interfaces.TimeVariantLink;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;

import javax.inject.Inject;
import java.util.Collection;

class NewNetworkChangeEventsEngine implements MobsimEngine {

	private final MessageQueue messageQueue;
	private final Network network;
	private InternalInterface internalInterface;

	@Inject
	NewNetworkChangeEventsEngine(Network network, MessageQueue messageQueue) {
		this.network = network;
		this.messageQueue = messageQueue;
	}

	@Override
	public void onPrepareSim() {
		Collection<NetworkChangeEvent> changeEvents = ((NetworkImpl) network).getNetworkChangeEvents();
		for (final NetworkChangeEvent changeEvent : changeEvents) {
			Message m = new Message() {
				@Override
				public void processEvent() {

				}

				@Override
				public void handleMessage() {
					for (Link link : changeEvent.getLinks()) {
						final NetsimLink netsimLink = internalInterface.getMobsim().getNetsimNetwork().getNetsimLink(link.getId());
						if ( netsimLink instanceof TimeVariantLink ) {
							final double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
							((TimeVariantLink) netsimLink).recalcTimeVariantAttributes();
						} else {
							throw new RuntimeException("link not time variant") ;
						}
					}
				}
			};
			m.setMessageArrivalTime(changeEvent.getStartTime());
			messageQueue.putMessage(m);
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
