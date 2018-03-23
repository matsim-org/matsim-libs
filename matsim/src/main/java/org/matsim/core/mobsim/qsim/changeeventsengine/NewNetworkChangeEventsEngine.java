package org.matsim.core.mobsim.qsim.changeeventsengine;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.jdeqsim.Message;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNetwork;
import org.matsim.core.mobsim.qsim.interfaces.TimeVariantLink;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;

import javax.inject.Inject;
import java.util.Queue;

class NewNetworkChangeEventsEngine implements NetworkChangeEventsEngineI {
	private static final Logger log = Logger.getLogger( NewNetworkChangeEventsEngine.class ) ;

	private final MessageQueue messageQueue;
	private final Network network;
	private final NetsimNetwork netsimNetwork;
	private final MobsimTimer mobsimTimer;

	@Inject
	NewNetworkChangeEventsEngine(Network network, MessageQueue messageQueue, NetsimNetwork netsimNetwork, MobsimTimer mobsimTimer) {
		this.network = network;
		this.messageQueue = messageQueue;
		this.netsimNetwork = netsimNetwork;
		this.mobsimTimer = mobsimTimer;
	}

	@Override
	public void onPrepareSim() {
		Queue<NetworkChangeEvent> changeEvents = NetworkUtils.getNetworkChangeEvents(network);
		for (final NetworkChangeEvent changeEvent : changeEvents) {
			addNetworkChangeEventToMessageQ(changeEvent);
		}
	}
	
	private void addNetworkChangeEventToMessageQ(NetworkChangeEvent changeEvent) {
		Message m = new Message() {
			@Override
			public void processEvent() {

			}

			@Override
			public void handleMessage() {
				applyTheChangeEvent(changeEvent);
			}
		};
		m.setMessageArrivalTime(changeEvent.getStartTime());
		messageQueue.putMessage(m);
	}
	
	private void applyTheChangeEvent(NetworkChangeEvent changeEvent) {
		for (Link link : changeEvent.getLinks()) {
			final NetsimLink netsimLink = netsimNetwork.getNetsimLink(link.getId());
			if ( netsimLink instanceof TimeVariantLink) {
				((TimeVariantLink) netsimLink).recalcTimeVariantAttributes();
			} else {
				throw new RuntimeException("link not time variant") ;
			}

		}
	}
	
	public final void addNetworkChangeEvent( NetworkChangeEvent event ) {
		// used (and thus implicitly tested) by bdi-abm-integration project.  A separate core test would be good. kai, feb'18
		
		log.warn("add within-day network change event:" + event);
		
		final Queue<NetworkChangeEvent> centralNetworkChangeEvents =
				NetworkUtils.getNetworkChangeEvents(network);
		if ( centralNetworkChangeEvents.contains( event ) ) {
			log.warn("network change event already in central data structure; not adding it again") ;
		} else {
			log.warn("network change event not yet in central data structure; adding it") ;
//			centralNetworkChangeEvents.add( event ) ;
			NetworkUtils.addNetworkChangeEvent(network, event);
			// need to add this here since otherwise speed lookup in mobsim does not work. And need to hedge against
			// code that may already have added it by itself.  kai, feb'18
		}
		
		if ( event.getStartTime()<= mobsimTimer.getTimeOfDay() ) {
			this.applyTheChangeEvent(event);
		} else {
			this.addNetworkChangeEventToMessageQ(event);
		}
		
	}
	
	
	@Override
	public void afterSim() {

	}

	/*@Override
	public void setInternalInterface(InternalInterface internalInterface) {
	}*/

	@Override
	public void doSimStep(double time) {

	}
}
