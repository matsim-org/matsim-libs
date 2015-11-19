/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerQueuesManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.hitchiking.qsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import playground.thibautd.hitchiking.HitchHikingConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public class PassengerQueuesPerLink {
	private static final Logger log =
		Logger.getLogger(PassengerQueuesPerLink.class);

	private final Map<Id, QueuesPerDestination> queuesPerLink = new HashMap<Id, QueuesPerDestination>();

	public synchronized QueuesPerDestination getQueuesAtLink(final Id link) {
		QueuesPerDestination qs = queuesPerLink.get( link );

		if (qs == null) {
			qs = new QueuesPerDestination();
			queuesPerLink.put( link , qs );
		}

		return qs;
	}

	public void endIteration(final double endTime, final EventsManager events) {
		int nLocationsWithWaitingAgents = 0;
		int nWaitingAgents = 0;

		for (Map.Entry<Id, QueuesPerDestination> queues : queuesPerLink.entrySet()) {
			boolean thereWereAgents = false;
			Id linkId = queues.getKey();
			for (Queue queue : queues.getValue().queues.values()) {
				int size = queue.size();
				if (size > 0) {
					nWaitingAgents += size;
					thereWereAgents = true;

					for (MobsimAgent agent : queue.queue) {
						events.processEvent(
								new PersonStuckEvent(endTime, agent.getId(), linkId, HitchHikingConstants.PASSENGER_MODE) );
					}
				}
			}
			if (thereWereAgents) {
				nLocationsWithWaitingAgents++;
			}
		}

		log.info( nWaitingAgents+" agents waiting at "+nLocationsWithWaitingAgents+" locations" );
		log.info( "they were notified as stuck" );
	}

	// /////////////////////////////////////////////////////////////////////////
	// classes 
	// /////////////////////////////////////////////////////////////////////////
	public static class QueuesPerDestination {
		private final Map<Id, Queue> queues = new HashMap<Id, Queue>();

		public synchronized Queue getQueueForDestination(final Id destination) {
			Queue q = queues.get( destination );

			if (q == null) {
				q = new Queue();
				queues.put( destination , q );
			}

			return q;
		}
	}

	public static class Queue {
		private final LinkedList<MobsimAgent> queue = new LinkedList<MobsimAgent>();

		public int size() {
			return queue.size();
		}

		public Collection<MobsimAgent> callAgents(final int nAgents) {
			List<MobsimAgent> agents = new ArrayList<MobsimAgent>();

			int count = 0;
			while (count < nAgents && queue.size() > 0) {
				count++;
				agents.add( queue.removeFirst() );
			}

			return agents;
		}

		public void addWaitingAgent(final MobsimAgent agent) {
			queue.addLast( agent );
		}
	}
}

