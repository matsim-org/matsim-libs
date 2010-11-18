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

package org.matsim.ptproject.qsim.multimodalsimengine;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;
import org.matsim.ptproject.qsim.qnetsimengine.QNode;

public class MultiModalQLinkExtension {

	private final NetsimLink qLink;
	private final MultiModalQNodeExtension toNode;
	protected MultiModalSimEngine simEngine;
	private PlansCalcRouteConfigGroup configGroup;
//	private final MultiModalTravelTime multiModalTravelTime;
	
	/*
	 * Is set to "true" if the MultiModalQLinkExtension has active Agents.
	 */
	protected boolean isActive = false;
	
	protected Queue<Tuple<Double, PersonAgent>> agents = new PriorityQueue<Tuple<Double, PersonAgent>>(30, new TravelTimeComparator());
	protected Queue<PersonAgent> waitingAfterActivityAgents = new LinkedList<PersonAgent>();
	protected Queue<PersonAgent> waitingToLeaveAgents = new LinkedList<PersonAgent>();
	
	public MultiModalQLinkExtension(NetsimLink qLink, MultiModalSimEngine simEngine, QNode toNode) {
		this.qLink = qLink;
		this.simEngine = simEngine;
		this.toNode = simEngine.getMultiModalQNodeExtension(toNode);
		this.configGroup = simEngine.getMobsim().getScenario().getConfig().plansCalcRoute();

		if (configGroup == null) configGroup = new PlansCalcRouteConfigGroup(); 	
	}
	
	/*package*/ void setMultiModalSimEngine(MultiModalSimEngine simEngine) {
		this.simEngine = simEngine;
	}
	
	/*package*/ boolean hasWaitingToLeaveAgents() {
		return waitingToLeaveAgents.size() > 0;
	}

	/**
	 * Adds a personAgent to the link (i.e. the "queue"), called by
	 * {@link MultiModalQNode#moveAgentOverNode(PersonAgent, double)}.
	 *
	 * @param personAgent
	 *          the personAgent
	 */
	public void addAgentFromIntersection(PersonAgent personAgent, double now) {
		this.activateLink();
		
		this.addAgent(personAgent, now);

		this.simEngine.getMobsim().getEventsManager().processEvent(
			new LinkEnterEventImpl(now, personAgent.getPerson().getId(), qLink.getLink().getId()));
	}

	private void addAgent(PersonAgent personAgent, double now) {	
		double travelTime = simEngine.getMultiModalTravelTime().getModalLinkTravelTime(qLink.getLink(), now, personAgent.getCurrentLeg().getMode());
		double departureTime = now + travelTime;
		
		departureTime = Math.round(departureTime);
		
		agents.add(new Tuple<Double, PersonAgent>(departureTime, personAgent));
	}
	
	public void addDepartingAgent(PersonAgent personAgent, double now) {
		this.waitingAfterActivityAgents.add(personAgent);
		this.activateLink();
	
		this.simEngine.getMobsim().getEventsManager().processEvent(
				new AgentWait2LinkEventImpl(now, personAgent.getPerson().getId(), qLink.getLink().getId()));
	}
	
	protected boolean moveLink(double now) {
		boolean isActive = moveAgents(now);
		
		moveWaitingAfterActivityAgents();

		return isActive;
	}
	
	private void activateLink() {
		/*
		 *  If the QLink and/or the MultiModalQLink is already active
		 *  we do not do anything.
		 */
		if (isActive) return;
		
		else simEngine.activateLink(this);
	}
	
	/*
	 * Returns true, if the Link has to be still active.
	 */
	private boolean moveAgents(double now) {
		Tuple<Double, PersonAgent> tuple = null;
		
		while ((tuple = agents.peek()) != null) {
			/*
			 * If the PersonAgent cannot depart now:
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
			
			// Check if PersonAgent has reached destination:
			PersonDriverAgent driver = (PersonDriverAgent) tuple.getSecond();
			if ((qLink.getLink().getId().equals(driver.getDestinationLinkId())) && (driver.chooseNextLinkId() == null)) {
				driver.endLegAndAssumeControl(now);
			}
			/*
			 * The PersonAgent can leave, therefore we move him to the waitingToLeave Queue.
			 */	
			else {
				waitingToLeaveAgents.add(tuple.getSecond());
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
		
		if (waitingToLeaveAgents.size() > 0) toNode.activateNode();
	}
	
	public PersonAgent getNextWaitingAgent(double now) {
		PersonAgent personAgent = waitingToLeaveAgents.poll();
		if (personAgent != null) {
			this.simEngine.getMobsim().getEventsManager().processEvent(new LinkLeaveEventImpl(now, personAgent.getPerson().getId(), qLink.getLink().getId()));			
		}
		return personAgent;
	}
		
	public void clearVehicles() {
		double now = this.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		
		for (Tuple<Double, PersonAgent> tuple : agents) {
			PersonAgent personAgent = tuple.getSecond();
			this.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentStuckEventImpl(now, personAgent.getPerson().getId(), qLink.getLink().getId(), personAgent.getCurrentLeg().getMode()));
		}
		this.simEngine.getMobsim().getAgentCounter().decLiving(this.agents.size());
		this.simEngine.getMobsim().getAgentCounter().incLost(this.agents.size());
		this.agents.clear();
	}
	
	private static class TravelTimeComparator implements Comparator<Tuple<Double, PersonAgent>>, Serializable {
		private static final long serialVersionUID = 1L;
		@Override
		public int compare(final Tuple<Double, PersonAgent> o1, final Tuple<Double, PersonAgent> o2) {
			int ret = o1.getFirst().compareTo(o2.getFirst()); // first compare time information
			if (ret == 0) {
				ret = o2.getSecond().getPerson().getId().compareTo(o1.getSecond().getPerson().getId()); // if they're equal, compare the Ids: the one with the larger Id should be first
			}
			return ret;
		}
	}
}
