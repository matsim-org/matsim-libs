/* *********************************************************************** *
 * project: org.matsim.*
 * PlanAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.mobsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.misc.Time;

/**
 * An implementation of {@link MobsimAgent} that uses a {@link PersonImpl} as
 * underlying data source. A PlanAgent serves the {@link MobsimAgent} interface
 * according to the person's selected plan.
 * 
 * @author illenberger
 * 
 */
public class PlanAgent implements MobsimAgent {

	// =======================================================
	// private fields
	// =======================================================

	private final Person person;

	private int currentPlanIndex;

	private int currentRouteIndex;

	private Link currentLink;

	// =======================================================
	// constructor
	// =======================================================

	/**
	 * Creates a new PlanAgent with <tt>person</tt> as the underlying data
	 * source. Unless {@link #beforeSim()} is called, the state of the agent is
	 * undefined.
	 * 
	 * @param person
	 *            the underlying data source. <tt>person</tt> must have a
	 *            selected plan!
	 */
	public PlanAgent(Person person) {
		this.person = person;
	}

	// =======================================================
	// MobsimAgent interface implementation
	// =======================================================

	/**
	 * @see {@link MobsimAgent}
	 */
	public Link getLink() {
		return currentLink;
	}

	/**
	 * Returns the end time of the current activity, or the end time of the next
	 * activity if the agent is en-route, or {@link Double#MAX_VALUE} if
	 * {@link #isDone()} returns <tt>true</tt>.<br>
	 * Can return {@link Time#UNDEFINED_TIME} if the agent is en-route, since
	 * the end time of the next activity might be unknown at this time. This
	 * depends on what is defined in the person's selected plan.
	 * 
	 * @param time
	 *            the current simulation time.
	 * @return the next desired departure time.
	 */
	public double getDepartureTime(double time) {
		if (isDone())
			return Double.MAX_VALUE;
		else {
			int index = currentPlanIndex;
			if (index % 2 != 0) {
				index++;
			}
			return ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(index))
					.getEndTime();
		}
	}

	/**
	 * Returns the mode of the next trip, or the mode of the current trip if the
	 * agent is en-route, or <tt>null</tt> if {@link #isDone()} returns
	 * <tt>true</tt>.
	 * 
	 * @param time
	 *            the current simulation time.
	 * @return the mode of the current or next trip.
	 */
	public TransportMode getMode(double time) {
		int index = currentPlanIndex;
		if (index % 2 == 0) {
			if (isDone())
				return null;
			else {
				index++;
			}
		}
		return ((LegImpl) person.getSelectedPlan().getPlanElements().get(index))
				.getMode();
	}

	/**
	 * @see {@link MobsimAgent#getNextLink(double)}
	 */
	public Link getNextLink(double time) {
		/*
		 * TODO: We need the link-based route implementation here!
		 */
		return null;
	}

	/**
	 * Returns the link of the next activity, or {@link #getLink()} if
	 * {@link #isDone()} returns <tt>true</tt>.
	 * 
	 * @param time
	 *            the current simulation time.
	 * @return the desired destination link.
	 */
	public Link getDestinationLink(double time) {
		if (currentPlanIndex % 2 == 0)
			if (isDone())
				return getLink();
			else
				return ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(
						currentPlanIndex + 2)).getLink();
		else
			return ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(
					currentPlanIndex + 1)).getLink();
	}

	/**
	 * Initializes the agent to its initial state, which is the first activity
	 * in the selected plan.<br>
	 * The first activity's start time is set to 00:00:00. If the end time is
	 * undefined, it is set to {@link ActivityImpl#getDuration()}. If both, duration and end
	 * time are defined, the end time is set to min[end time, duration].<br>
	 * The current link is set to {@link ActivityImpl#getLinkId()}.
	 */
	public void beforeSim() {
		currentRouteIndex = -1;
		currentPlanIndex = 0;

		ActivityImpl act = (ActivityImpl) person.getSelectedPlan().getPlanElements().get(
				currentPlanIndex);
		act.setStartTime(0);
		if (act.getEndTime() != Time.UNDEFINED_TIME
				&& act.getDuration() != Time.UNDEFINED_TIME) {
			act.setEndTime(Math.min(act.getEndTime(), act.getDuration()));
		} else if (act.getEndTime() == Time.UNDEFINED_TIME) {
			act.setEndTime(act.getDuration());
		}

		currentLink = act.getLink();
	}

	/**
	 * @return <tt>true</tt> if the agent has reached its last activity,
	 *         <tt>false</tt> otherwise.
	 */
	public boolean isDone() {
		if (currentPlanIndex < person.getSelectedPlan().getPlanElements().size() - 1)
			return false;
		else
			return true;
	}

	/**
	 * Informs the agent that it arrived.<br>
	 * Sets the arrival time of the traveled leg to <tt>time</tt> and the leg
	 * travel time to <tt>time</tt> - arrival time.<br>
	 * Sets the start time of the following activity to <tt>time</tt>. If the
	 * activity's end time is undefined, it is set to <tt>time</tt> +
	 * {@link ActivityImpl#getDuration()}. If both, duration and end time are defined, the
	 * end time is set to min[end time, <tt>time</tt> + duration].
	 * 
	 * @param time
	 *            the current simulation time.
	 */
	public void arrival(double time) {
		LegImpl leg = (LegImpl) person.getSelectedPlan().getPlanElements().get(
				currentPlanIndex);
		leg.setArrivalTime(time);
		leg.setTravelTime(leg.getArrivalTime() - leg.getDepartureTime());
		currentRouteIndex = -1;
		currentPlanIndex++;

		ActivityImpl act = (ActivityImpl) person.getSelectedPlan().getPlanElements().get(
				currentPlanIndex);
		act.setStartTime(time);
		if (act.getEndTime() != Time.UNDEFINED_TIME
				&& act.getDuration() != Time.UNDEFINED_TIME) {
			act.setEndTime(Math.min(act.getEndTime(), time + act.getDuration()));
		} else if (act.getEndTime() == Time.UNDEFINED_TIME) {
			act.setEndTime(time + act.getDuration());
		}
	}

	/**
	 * Informs the agent that it departed.<br>
	 * Sets the activity's end time to <tt>time</tt> and the duration to (end
	 * time - start time). Sets the departure time of the following leg to
	 * <tt>time</tt>.
	 * 
	 * @param time
	 *            the current simulation time.
	 */
	public void departure(double time) {
		ActivityImpl act = (ActivityImpl) person.getSelectedPlan().getPlanElements().get(
				currentPlanIndex);
		act.setEndTime(time);
		act.setDuration(act.getEndTime() - act.getStartTime());
		currentPlanIndex++;

		LegImpl leg = (LegImpl) person.getSelectedPlan().getPlanElements().get(
				currentPlanIndex);
		leg.setDepartureTime(time);
		currentRouteIndex = 0;
	}

	/**
	 * Informs the agent that it has been set to a new link. <tt>link</tt>
	 * must be consistent to {@link #getNextLink(double)}, otherwise an
	 * {@link ArrayIndexOutOfBoundsException} is thrown.
	 * 
	 * @param link
	 *            the link the agent has been set to.
	 * @param time
	 *            the current simulation time.
	 * @throws {@link ArrayIndexOutOfBoundsException}
	 *             if the agent has been set to a new link, although it has
	 *             already reached its destination.
	 */
	public void enterLink(Link link, double time) {
		currentLink = link;
		currentRouteIndex++;
		Link desiredLink = ((NetworkRouteWRefs) ((LegImpl) person.getSelectedPlan().getPlanElements().get(
				currentPlanIndex)).getRoute()).getLinks().get(currentRouteIndex);
		if (currentLink != desiredLink)
			currentRouteIndex--;
	}

	/**
	 * @return the id of the underlying person object.
	 */
	public Id getId() {
		return person.getId();
	}

	// =======================================================
	// accessor methods
	// =======================================================

	/**
	 * @return the underlying person.
	 */
	public Person getPerson() {
		return person;
	}

	/**
	 * @return the (zero-based) index of the current plan entry, even for
	 *         activities and odd for legs.
	 */
	public int getCurrentPlanIndex() {
		return currentPlanIndex;
	}

	/**
	 * @return the (zero-based) index of the current link in the current route
	 *         including departure and destination link, or <tt>-1</tt> if the
	 *         agent is not en-route, i.e., {@link #getCurrentPlanIndex()} is
	 *         even.
	 */
	public int getCurrentRouteIndex() {
		return currentRouteIndex;
	}
}
