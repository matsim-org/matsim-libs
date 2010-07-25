/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalQLinkImpl.java
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

package playground.christoph.multimodal.mobsim.netsimengine;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.ptproject.qsim.interfaces.QSimEngine;
import org.matsim.ptproject.qsim.netsimengine.QLinkImpl;
import org.matsim.ptproject.qsim.netsimengine.QNode;

import playground.christoph.multimodal.router.costcalculator.MultiModalTravelTime;

/*
 * Agents with different TransportModes are all collected in the 
 * same Queues. The only difference between TransportModes are the
 * different TravelTimes which are respected by the TravelTimeComparator.
 */
public class MultiModalQLinkImpl extends QLinkImpl {

	protected PlansCalcRouteConfigGroup configGroup;
	protected MultiModalTravelTime multiModalTravelTime;
	
	/*
	 * Is set to "true" if the QLink and/or the MultiModalQLink has active Agents.
	 */
	protected boolean isActive = false;
	
	protected Queue<Tuple<Double, PersonAgent>> agents = new PriorityQueue<Tuple<Double, PersonAgent>>(30, new TravelTimeComparator());
	protected Queue<PersonAgent> waitingAfterActivityAgents = new LinkedList<PersonAgent>();
	protected Queue<PersonAgent> waitingToLeaveAgents = new LinkedList<PersonAgent>();
//	protected Set<PersonAgent> waitingAfterActivityAgents = new HashSet<PersonAgent>();
//	protected Set<PersonAgent> waitingToLeaveAgents = new TreeSet<PersonAgent>(new PersonAgentComparator());
	
	public MultiModalQLinkImpl(Link link2, QSimEngine engine, QNode toNode, MultiModalTravelTime multiModalTravelTime) {
		super(link2, engine, toNode);
				
		configGroup = engine.getQSim().getScenario().getConfig().plansCalcRoute();
		if (configGroup == null) configGroup = new PlansCalcRouteConfigGroup(); 
		
		this.multiModalTravelTime = multiModalTravelTime;
	}
	
	public boolean hasWaitingToLeaveAgents() {
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

		this.getQSimEngine().getQSim().getEventsManager().processEvent(
			new LinkEnterEventImpl(now, personAgent.getPerson().getId(), this.getLink().getId()));
	}

	private void addAgent(PersonAgent personAgent, double now) {	
		double travelTime = multiModalTravelTime.getModalLinkTravelTime(this.getLink(), now, personAgent.getCurrentLeg().getMode());
		double departureTime = now + travelTime;
		
		departureTime = Math.round(departureTime);
		
		agents.add(new Tuple<Double, PersonAgent>(departureTime, personAgent));
	}
	
	public void addDepartingAgent(PersonAgent personAgent, double now) {
		this.waitingAfterActivityAgents.add(personAgent);
		this.activateLink();
	
		this.getQSimEngine().getQSim().getEventsManager().processEvent(
				new AgentWait2LinkEventImpl(now, personAgent.getPerson().getId(), this.getLink().getId()));
	}
	
	/*
	 *  We don't have to (re)activate the Link here.
	 *  This method is only called, if the Link is active.
	 *  If true is returned, it is kept active.
	 */
	@Override
	public boolean moveLink(double now) {
		/*
		 * super.moveLink(now) returns true, if the link is still active
		 * moveAgents(now) returns true, if still Agents are walking/cycling/...
		 */
		boolean ret = super.moveLink(now);		
		boolean ret2 = moveAgents(now);
		
//		qLinkIsActive = ret;
		
		moveWaitingAfterActivityAgents();
		/*
		 * super: active, this: inactive -> active
		 * super: inactive, this: active -> active
		 * super: inactive, this: inactive -> inactive
		 * super: active, this: active -> active
		 */
		isActive = (ret || ret2);
		return isActive;
	}
	
	@Override
	public void activateLink() {
		/*
		 *  If the QLink and/or the MultiModalQLink is already active
		 *  we do not do anything.
		 */
		if (isActive) return;
		
		else super.activateLink();
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
			if ((this.getLink().getId().equals(driver.getDestinationLinkId())) && (driver.chooseNextLinkId() == null)) {
				driver.legEnds(now);
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
		
		if (waitingToLeaveAgents.size() > 0) ((MultiModalQNode) this.getToQueueNode()).activiateNode();
	}
	
	public PersonAgent getNextWaitingAgent(double now) {
		PersonAgent personAgent = waitingToLeaveAgents.poll();
//		PersonAgent personAgent = ((TreeSet<PersonAgent>)waitingToLeaveAgents).pollFirst();
		if (personAgent != null) {
			this.getQSimEngine().getQSim().getEventsManager().processEvent(new LinkLeaveEventImpl(now, personAgent.getPerson().getId(), this.getLink().getId()));			
		}
		return personAgent;
	}
		
	@Override
	public void clearVehicles() {
		super.clearVehicles();
		
		double now = this.getQSimEngine().getQSim().getSimTimer().getTimeOfDay();
		
		for (Tuple<Double, PersonAgent> tuple : agents) {
			PersonAgent personAgent = tuple.getSecond();
			this.getQSimEngine().getQSim().getEventsManager().processEvent(
					new AgentStuckEventImpl(now, personAgent.getPerson().getId(), this.getLink().getId(), personAgent.getCurrentLeg().getMode()));
		}
		this.getQSimEngine().getQSim().getAgentCounter().decLiving(this.agents.size());
		this.getQSimEngine().getQSim().getAgentCounter().incLost(this.agents.size());
		this.agents.clear();
	}
	
//	private static class PersonAgentComparator implements Comparator<PersonAgent> {
//		@Override
//		public int compare(PersonAgent a1, PersonAgent a2) {
//			return a1.getPerson().getId().compareTo(a2.getPerson().getId());
//		}
//	}
	
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
