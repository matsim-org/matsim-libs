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

package org.matsim.core.mobsim.queuesim;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.utils.misc.Time;

/**
 * @author dgrether
 */
public class PersonAgent implements DriverAgent {

	private static final Logger log = Logger.getLogger(PersonAgent.class);

	private final Person person;
	private QueueVehicle vehicle;
	protected Link cachedNextLink = null;

	private final QueueSimulation simulation;

	private double activityDepartureTime = Time.UNDEFINED_TIME;

	private Link currentLink = null;

	/**
	 * specifies the position of the next activity in the acts legs list
	 */
	@Deprecated
	private int nextActivity;

	private int currentPlanElementIndex = 0;

	private transient Link destinationLink;

	private LegImpl currentLeg;
	private List<Node> cacheRouteNodes = null;

	private int currentNodeIndex;

	public PersonAgent(final Person p, final QueueSimulation simulation) {
		this.person = p;
		this.simulation = simulation;
	}

	public Person getPerson() {
		return this.person;
	}

	/**
	 * Convenience method delegating to person's selected plan
	 * @return list of {@link ActivityImpl}s and {@link LegImpl}s of this agent's plan
	 */
	private List<? extends PlanElement> getPlanElements() {
		return this.person.getSelectedPlan().getPlanElements();
	}

	public void setVehicle(final QueueVehicle veh) {
		this.vehicle = veh;
	}

	public QueueVehicle getVehicle() {
		return this.vehicle;
	}

	public double getDepartureTime() {
		return this.activityDepartureTime;
	}

	private void setDepartureTime(final double seconds) {
		this.activityDepartureTime = seconds;
	}

	protected Link getCurrentLink() {
		return this.currentLink;
	}

	public Leg getCurrentLeg() {
		return this.currentLeg;
	}

	protected void setCurrentLeg(final LegImpl leg) {
		this.currentLeg  = leg;
		this.cacheRouteNodes = null;
	}

	public int getCurrentNodeIndex() {
		return this.currentNodeIndex;
	}

	@Deprecated
	public int getNextActivity() {
		return this.nextActivity;
	}

	public Link getDestinationLink() {
		return this.destinationLink;
	}

	public boolean initialize() {
		this.nextActivity = 0;
		List<? extends PlanElement> planElements = this.getPlanElements();
		this.currentPlanElementIndex = 0;
		ActivityImpl firstAct = (ActivityImpl) planElements.get(0);
		double departureTime = firstAct.getEndTime();
		
		this.currentLink = firstAct.getLink();
		if ((departureTime != Time.UNDEFINED_TIME) && (planElements.size() > 1)) {
			setDepartureTime(departureTime);
//			SimulationTimer.updateSimStartTime(departureTime);
			this.simulation.scheduleActivityEnd(this);
			AbstractSimulation.incLiving();
			return true;
		}
		return false; // the agent has no leg, so nothing more to do
	}

	private void initNextLeg(double now, final LegImpl leg) {
		RouteWRefs route = leg.getRoute();
		if (route == null) {
			log.error("The agent " + this.getPerson().getId() + " has no route in its leg. Removing the agent from the simulation.");
			AbstractSimulation.decLiving();
			AbstractSimulation.incLost();
			return;
		}
		this.destinationLink = route.getEndLink();

		// set the route according to the next leg
		this.currentLeg = leg;
		this.cacheRouteNodes = null;
		this.currentNodeIndex = 1;
		this.cachedNextLink = null;
		this.nextActivity += 2;

		this.simulation.agentDeparts(now, this, this.currentLink);
	}

	/**
	 * Notifies the agent that it leaves its current activity location (and
	 * accordingly starts moving on its current route).
	 *
	 * @param now the current time
	 */
	public void activityEnds(final double now) {
		ActivityImpl act = (ActivityImpl) this.getPlanElements().get(this.currentPlanElementIndex);
		QueueSimulation.getEvents().processEvent(new ActivityEndEventImpl(now, this.getPerson(), act.getLinkId(), act));
		advancePlanElement(now);
	}

	public void legEnds(final double now) {
		this.simulation.handleAgentArrival(now, this);
		if (this.currentLink != this.destinationLink) {
			log.error("The agent " + this.getPerson().getId() + " has destination link " + this.destinationLink.getId()
					+ ", but arrived on link " + this.currentLink.getId() + ". Removing the agent from the simulation.");
			AbstractSimulation.decLiving();
			AbstractSimulation.incLost();
			return;
		}
		advancePlanElement(now);
	}

	public void teleportToLink(final Link link) {
		this.currentLink = link;
	}

	private void advancePlanElement(final double now) {
		this.currentPlanElementIndex++;

		PlanElement pe = this.getPlanElements().get(this.currentPlanElementIndex);
		if (pe instanceof ActivityImpl) {
			reachActivity(now, (ActivityImpl) pe);

			if ((this.currentPlanElementIndex+1) < this.getPlanElements().size()) {
				// there is still at least on plan element left
				this.simulation.scheduleActivityEnd(this);
			} else {
				// this is the last activity
				AbstractSimulation.decLiving();
			}

		} else if (pe instanceof LegImpl) {
			initNextLeg(now, (LegImpl) pe);
		} else {
			throw new RuntimeException("Unknown PlanElement of type " + pe.getClass().getName());
		}
	}

	/**
	 * Notifies the agent that it reaches its aspired activity location.
	 *
	 * @param now the current time
	 * @param act the activity the agent reaches
	 */
	private void reachActivity(final double now, final ActivityImpl act) {
		QueueSimulation.getEvents().processEvent(new ActivityStartEventImpl(now, this.getPerson(),  this.currentLink.getId(), act));
		/* schedule a departure if either duration or endtime is set of the activity.
		 * Otherwise, the agent will just stay at this activity for ever...
		 */
		if ((act.getDuration() == Time.UNDEFINED_TIME) && (act.getEndTime() == Time.UNDEFINED_TIME)) {
			setDepartureTime(Double.POSITIVE_INFINITY);
		} else {

			double departure = 0;

			if (this.simulation.isUseActivityDurations()) {
				/* The person leaves the activity either 'actDur' later or
				 * when the end is defined of the activity, whatever comes first. */
				if (act.getDuration() == Time.UNDEFINED_TIME) {
					departure = act.getEndTime();
				} else if (act.getEndTime() == Time.UNDEFINED_TIME) {
					departure = now + act.getDuration();
				} else {
					departure = Math.min(act.getEndTime(), now + act.getDuration());
				}
			}
			else {
				if (act.getEndTime() != Time.UNDEFINED_TIME) {
					departure = act.getEndTime() ;
				}
				else {
					throw new IllegalStateException("Can not use activity end time as new departure time as it is not set for person: " + this.getPerson().getId());
				}
			}



			if (departure < now) {
				// we cannot depart before we arrived, thus change the time so the timestamp in events will be right
				departure = now;
				// actually, we will depart in (now+1) because we already missed the departing in this time step
			}
			setDepartureTime(departure);
		}
	}

	public void moveOverNode() {
		this.currentLink = this.cachedNextLink;
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
		if (this.cacheRouteNodes == null) {
			this.cacheRouteNodes = ((NetworkRouteWRefs) this.currentLeg.getRoute()).getNodes();
		}

		if (this.currentNodeIndex >= this.cacheRouteNodes.size() ) {
			// we have no more information for the route, so we should have arrived at the destination link
			if (this.currentLink.getToNode().equals(this.destinationLink.getFromNode())) {
				this.cachedNextLink = this.destinationLink;
				return this.cachedNextLink;
			}
			if (this.currentLink != this.destinationLink) {
				// there must be something wrong. Maybe the route is too short, or something else, we don't know...
				log.error("The vehicle with driver " + this.getPerson().getId() + ", currently on link " + this.currentLink.getId().toString()
						+ ", is at the end of its route, but has not yet reached its destination link " + this.destinationLink.getId().toString());
			}
			return null;
		}

		Node destNode = this.cacheRouteNodes.get(this.currentNodeIndex);

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
