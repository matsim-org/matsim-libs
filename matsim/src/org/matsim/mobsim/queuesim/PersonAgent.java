/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.mobsim.queuesim;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.utils.misc.Time;

/**
 * @author dgrether
 */
public class PersonAgent {

	private static final Logger log = Logger.getLogger(PersonAgent.class);

	private final Person person;
	private Vehicle vehicle;
	protected Link cachedNextLink = null;

	private Link currentLink;
	/**
	 * specifies the position of the next activity in the acts legs list
	 */
	private int nextActivity;

	private transient Link destinationLink;

	private Leg currentLeg;

	private int currentNodeIndex;

	public PersonAgent(final Person p) {
		this.person = p;
	}

	public Person getPerson() {
		return this.person;
	}

	/**
	 * Convenience method delegating to person's selected plan
	 * @return list of {@link Act}s and {@link Leg}s of this agent's plan
	 */
	public List<Object> getActsLegs() {
		return this.person.getSelectedPlan().getActsLegs();
	}

	public void setVehicle(final Vehicle veh) {
		this.vehicle = veh;
	}

	public Vehicle getVehicle() {
		return this.vehicle;
	}

	public double getDepartureTime() {
		return this.vehicle.getDepartureTime_s();
	}

	public void setDepartureTime(final double seconds) {
		this.vehicle.setDepartureTime_s(seconds);
	}

	public void setCurrentLink(final Link link) {
		this.currentLink = link;
	}

	public Link getCurrentLink() {
		return this.currentLink;
	}

	public Leg getCurrentLeg() {
		return this.currentLeg;
	}

	protected void setCurrentLeg(final Leg leg) {
		this.currentLeg  = leg;
	}

	public int getCurrentNodeIndex() {
		return this.currentNodeIndex;
	}

	public int getNextActivity() {
		return this.nextActivity;
	}

	public Link getDestinationLink() {
		return this.destinationLink;
	}

	public boolean initialize() {
		this.nextActivity = 0;
		Act firstAct = (Act) this.getActsLegs().get(0);

		SimulationTimer.updateSimStartTime(firstAct.getEndTime());
		setCurrentLink(firstAct.getLink());

		if (initNextLeg()) {
			Simulation.incLiving();
			// this is the starting point for our vehicle, so put it in the queue
			return true;
		}
		return false;
	}

	private boolean initNextLeg() {
		double now = SimulationTimer.getTime();
		Act act = (Act)this.getActsLegs().get(this.nextActivity);

		if (act.getLink() != this.currentLink) {
			log.error("The vehicle with driver " + this.getPerson().getId() + " should be on link " + act.getLink().getId().toString()
					+ ", but is on link " + this.currentLink.getId().toString() + ". Removing the agent from the simulation.");
			return false;
		}

		if (this.nextActivity == this.getActsLegs().size()-1) {
			// if this is the last activity, then stop agent
			return false;
		}

		double departure = 0;
		/* WELL, THAT'S IMPORTANT:
		 * The person leaves the activity either 'actDur' later or
		 * when the end is defined of the activity, whatever comes first. */
		if (act.getDuration() == Time.UNDEFINED_TIME) {
			departure = act.getEndTime();
		} else if (act.getEndTime() == Time.UNDEFINED_TIME) {
			departure = now + act.getDuration();
		} else {
			departure = Math.min(act.getEndTime(), now + act.getDuration());
		}
		if (departure < now) {
			// we cannot depart before we arrived, thus change the time so the timestamp in events will be right
			departure = now;
			// actually, we will depart in (now+1) because we already missed the departing in this time step
		}
		setDepartureTime(departure);

		this.destinationLink = ((Act)this.getActsLegs().get(this.nextActivity +2)).getLink();

		// set the route according to the next leg
		Leg leg = (Leg) this.getActsLegs().get(this.nextActivity+1);
		this.currentLeg = leg;
		this.currentNodeIndex = 1;
		this.cachedNextLink = null;
		this.nextActivity += 2;
		return true;
	}

	/**
	 * Notifies the agent that it leaves its current activity location (and
	 * accordingly starts moving on its current route).
	 *
	 * @param now the current time
	 */
	public void leaveActivity(final double now) {
		Act act = (Act)this.getActsLegs().get(this.nextActivity - 2);
		QueueSimulation.getEvents().processEvent(new ActEndEvent(now, this.getPerson(), this.currentLink, act));
	}

	/**
	 * Notifies the agent that it reaches its aspired activity location.
	 *
	 * @param now the current time
	 * @param currentQueueLink
	 */
	public void reachActivity(final double now, final QueueLink currentQueueLink) {
		Act act = (Act)this.getActsLegs().get(this.nextActivity);
		// no actStartEvent for first act.
		QueueSimulation.getEvents().processEvent(new ActStartEvent(now, this.getPerson(), this.currentLink, act));
		// 	 this is the starting point for our vehicle, so put it in the queue
		if (!initNextLeg()) {
			Simulation.decLiving();
		}
		else {
			currentQueueLink.addParking(this.vehicle);
		}
	}

	public void incCurrentNode() {
		this.currentNodeIndex++;
		this.cachedNextLink = null; //reset cached nextLink
	}

	/**
	 * Returns the next link the vehicle will drive along.
	 *
	 * @return The next link the vehicle will drive on, or null if an error has happened.
	 */
	public Link chooseNextLink() {
		if (this.cachedNextLink != null) {
			return this.cachedNextLink;
		}
		List<Node> route = this.currentLeg.getRoute().getRoute();

		if (this.currentNodeIndex >= route.size() ) {
			// we have no more information for the route, so we should have arrived at the destination link
			if (this.currentLink.getToNode().equals(this.destinationLink.getFromNode())) {
				this.cachedNextLink = this.destinationLink;
				return this.cachedNextLink;
			}
			// there must be something wrong. Maybe the route is too short, or something else, we don't know...
			log.error("The vehicle with driver " + this.getPerson().getId() + ", currently on link " + this.currentLink.getId().toString()
					+ ", is at the end of its route, but has not yet reached its destination link " + this.destinationLink.getId().toString());
			return null;
		}

		Node destNode = route.get(this.currentNodeIndex);

		for (Link link :  this.currentLink.getToNode().getOutLinks().values()) {
			if (link.getToNode() == destNode) {
				this.cachedNextLink = link; //save time in later calls, if link is congested
				return this.cachedNextLink;
			}
		}
		log.warn(this + " [no link to next routenode found: routeindex= " + this.currentNodeIndex + " ]");
		return null;
	}

}
