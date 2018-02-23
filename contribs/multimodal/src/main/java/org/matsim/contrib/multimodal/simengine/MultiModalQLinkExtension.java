/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalQLinkExtension.java
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

package org.matsim.contrib.multimodal.simengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

class MultiModalQLinkExtension {

	private final Link link;
	private final MultiModalQNodeExtension toNode;
	private MultiModalSimEngine simEngine;
	private NetworkElementActivator activator = null;
	
	/*
	 * Is set to "true" if the MultiModalQLinkExtension has active Agents.
	 */
    private final AtomicBoolean isActive = new AtomicBoolean(false);

	private final Queue<Tuple<Double, MobsimAgent>> agents = new PriorityQueue<>(30, new TravelTimeComparator());
	private final Queue<MobsimAgent> waitingAfterActivityAgents = new LinkedList<>();
	private final Queue<MobsimAgent> waitingToLeaveAgents = new LinkedList<>();
	
	private final EventsManager eventsManager;
	private final AgentCounter agentCounter;
	private final MobsimTimer mobsimTimer;

	/*package*/ MultiModalQLinkExtension(Link link, MultiModalSimEngine simEngine, MultiModalQNodeExtension multiModalQNodeExtension, EventsManager eventsManager, AgentCounter agentCounter, MobsimTimer mobsimTimer) {
		this.link = link;
		this.simEngine = simEngine;
		this.toNode = multiModalQNodeExtension;
		this.eventsManager = eventsManager;
		this.agentCounter = agentCounter;
		this.mobsimTimer = mobsimTimer;
	}

	/*package*/ void setNetworkElementActivator(NetworkElementActivator activator) {
		this.activator = activator;
	}

	/*package*/ boolean hasWaitingToLeaveAgents() {
		return this.waitingToLeaveAgents.size() > 0;
	}
	
	/*package*/ Link getLink() {
		return this.link;
	}

	public void addAgentFromIntersection(MobsimAgent mobsimAgent, double now) {
		this.activateLink();

		this.addAgent(mobsimAgent, now);

		this.simEngine.getEventsManager().processEvent(new LinkEnterEvent(now, Id.create(mobsimAgent.getId(), Vehicle.class), link.getId()));
	}

	private void addAgent(MobsimAgent mobsimAgent, double now) {

		Map<String, TravelTime> multiModalTravelTime = this.simEngine.getMultiModalTravelTimes();
		Person person = null;
		if (mobsimAgent instanceof HasPerson) {
			person = ((HasPerson) mobsimAgent).getPerson(); 
		}
		double travelTime = multiModalTravelTime.get(mobsimAgent.getMode()).getLinkTravelTime(link, now, person, null);
		double departureTime = now + travelTime;

		departureTime = Math.round(departureTime);

		this.agents.add(new Tuple<>(departureTime, mobsimAgent));
	}

	public void addDepartingAgent(MobsimAgent mobsimAgent, double now) {
		this.waitingAfterActivityAgents.add(mobsimAgent);
		this.activateLink();

		this.simEngine.getEventsManager().processEvent(
				new PersonEntersVehicleEvent(now, mobsimAgent.getId(), Id.create(mobsimAgent.getId(), Vehicle.class)));
	
		this.simEngine.getEventsManager().processEvent(
				new VehicleEntersTrafficEvent(now, mobsimAgent.getId(), link.getId(), Id.create(mobsimAgent.getId(), Vehicle.class), mobsimAgent.getMode(), 1.0));
	}

	boolean moveLink(double now) {
		
		boolean keepLinkActive = moveAgents(now);
		this.isActive.set(keepLinkActive);

		moveWaitingAfterActivityAgents();

		// If agents are ready to leave the link, ensure that the to Node is active and handles them.
		if (this.hasWaitingToLeaveAgents()) toNode.activateNode();
		
		return keepLinkActive;
	}

	private void activateLink() {
		/*
		 * If isActive is false, then it is set to true ant the
		 * link is activated. Using an AtomicBoolean is thread-safe.
		 * Otherwise, it could be activated multiple times concurrently.
		 */
		if (this.isActive.compareAndSet(false, true)) {			
			this.activator.activateLink(this);
		}
	}

	/*
	 * Returns true, if the Link has to be still active.
	 */
	private boolean moveAgents(double now) {
		Tuple<Double, MobsimAgent> tuple;

		while ((tuple = agents.peek()) != null) {
			/*
			 * If the MobsimAgent cannot depart now:
			 * At least still one Agent is still walking/cycling/... on the Link, therefore
			 * it cannot be deactivated. We return true (Link has to be kept active).
			 */
			if (tuple.getFirst() > now) {
				return true;
			}

			/*
			 *  Agent starts next Activity at the same link or leaves the Link.
			 *  Therefore remove him from the Queue.
			 */
			agents.poll();

			// Check if MobsimAgent has reached destination:
			MobsimDriverAgent driver = (MobsimDriverAgent) tuple.getSecond();

//			if ((link.getId().equals(driver.getDestinationLinkId())) && (driver.chooseNextLinkId() == null)) {
			if ((link.getId().equals(driver.getDestinationLinkId())) && (driver.isWantingToArriveOnCurrentLink())) {
				// Christoph, the "isArrivingOnCurrentLink" method is new.  You may decide that this is enough, and getDestinationLinkId
				// does not need to be queried.  kai, nov'14
				
				eventsManager.processEvent(
						new VehicleLeavesTrafficEvent(now, driver.getId(), link.getId(), Id.create(driver.getId(), Vehicle.class), driver.getMode(), 1.0));
				
				eventsManager.processEvent(
						new PersonLeavesVehicleEvent(now, driver.getId(), Id.create(driver.getId(), Vehicle.class)));				
				
				driver.endLegAndComputeNextState(now);
				this.simEngine.internalInterface.arrangeNextAgentState(driver);
			}
			/*
			 * The PersonAgent can leave, therefore we move him to the waitingToLeave Queue.
			 */
			else {
				this.waitingToLeaveAgents.add(driver);
			}
		}
		
		return agents.size() > 0;
	}

	/*
	 * Add all Agents that have ended an Activity to the waitingToLeaveLink Queue.
	 * If waiting Agents exist, the toNode of this Link is activated.
	 */
	private void moveWaitingAfterActivityAgents() {
		waitingToLeaveAgents.addAll(waitingAfterActivityAgents);
		waitingAfterActivityAgents.clear();
	}

	public MobsimAgent getNextWaitingAgent(double now) {
		MobsimAgent personAgent = waitingToLeaveAgents.poll();
		if (personAgent != null) {
			this.simEngine.getEventsManager().processEvent(new LinkLeaveEvent(now, Id.create(personAgent.getId(), Vehicle.class), link.getId()));
		}
		return personAgent;
	}

	public void clearVehicles() {
		double now = mobsimTimer.getTimeOfDay();

		for (Tuple<Double, MobsimAgent> tuple : agents) {
			MobsimAgent mobsimAgent = tuple.getSecond();
			eventsManager.processEvent(
					new VehicleAbortsEvent(now, Id.create(mobsimAgent.getId(), Vehicle.class), link.getId()));
			eventsManager.processEvent(
					new PersonStuckEvent(now, mobsimAgent.getId(), link.getId(), mobsimAgent.getMode()));
			agentCounter.incLost();
			agentCounter.decLiving();
		}
		
		for (MobsimAgent mobsimAgent : this.waitingAfterActivityAgents) {
			eventsManager.processEvent(
					new VehicleAbortsEvent(now, Id.create(mobsimAgent.getId(), Vehicle.class), link.getId()));
			eventsManager.processEvent(
					new PersonStuckEvent(now, mobsimAgent.getId(), link.getId(), mobsimAgent.getMode()));
			agentCounter.incLost();
			agentCounter.decLiving();
		}
		
		for (MobsimAgent mobsimAgent : this.waitingToLeaveAgents) {
			eventsManager.processEvent(
					new VehicleAbortsEvent(now, Id.create(mobsimAgent.getId(), Vehicle.class), link.getId()));
			eventsManager.processEvent(
					new PersonStuckEvent(now, mobsimAgent.getId(), link.getId(), mobsimAgent.getMode()));
			agentCounter.incLost();
			agentCounter.decLiving();
		}
		
		this.agents.clear();
	}

	private static class TravelTimeComparator implements Comparator<Tuple<Double, MobsimAgent>>, Serializable {
		private static final long serialVersionUID = 1L;
		@Override
		public int compare(final Tuple<Double, MobsimAgent> o1, final Tuple<Double, MobsimAgent> o2) {
			int ret = o1.getFirst().compareTo(o2.getFirst()); // first compare time information
			if (ret == 0) {
				ret = o2.getSecond().getId().compareTo(o1.getSecond().getId()); // if they're equal, compare the Ids: the one with the larger Id should be first
			}
			return ret;
		}
	}
}