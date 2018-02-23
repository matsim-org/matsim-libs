package org.matsim.core.mobsim.qsim.changeeventsengine;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.jdeqsim.Message;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNetwork;
import org.matsim.core.mobsim.qsim.interfaces.TimeVariantLink;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Queue;

class NewNetworkChangeEventsEngine implements MobsimEngine {

	private final MessageQueue messageQueue;
	private final Network network;
	final private NetsimNetwork netsimNetwork;

	@Inject
	NewNetworkChangeEventsEngine(Network network, MessageQueue messageQueue, NetsimNetwork netsimNetwork) {
		this.network = network;
		this.messageQueue = messageQueue;
		this.netsimNetwork = netsimNetwork;
	}

	@Override
	public void onPrepareSim() {
		Queue<NetworkChangeEvent> changeEvents = NetworkUtils.getNetworkChangeEvents(((Network) network));
		for (final NetworkChangeEvent changeEvent : changeEvents) {
			Message m = new Message() {
				@Override
				public void processEvent() {

				}

				@Override
				public void handleMessage() {
					for (Link link : changeEvent.getLinks()) {
						final NetsimLink netsimLink = netsimNetwork.getNetsimLink(link.getId());
						if ( netsimLink instanceof TimeVariantLink ) {
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
	}

	@Override
	public void doSimStep(double time) {

	}
}
