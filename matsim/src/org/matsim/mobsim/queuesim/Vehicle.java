/* *********************************************************************** *
 * project: org.matsim.*
 * Vehicle.java
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

package org.matsim.mobsim.queuesim;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Route;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.netvis.DrawableAgentI;

public class Vehicle implements DrawableAgentI {

	static private int globalID = 0;
	public double lastMoveTime = 0;
	protected String driverId;
	private int currentNodeIndex;
	private int nextActivity = 0;
	private double currentDepartureTime = 0;
	private double lastMovedTime = 0;

	protected List<Object> actslegs = new ArrayList<Object>();
	protected transient Link cachedNextLink = null;
	private transient Link destinationLink = null;
	private transient Person driver = null;
	protected transient Link currentLink = null;
	protected transient BasicLeg currentLeg = null;

	private final static Logger log = Logger.getLogger(Vehicle.class);

	private final Id id = new IdImpl(globalID++); 

	public Vehicle() {}
	
	
	public double getDepartureTime_s() {
		return this.currentDepartureTime;
	}
	public void setDepartureTime_s(final double time) {
		this.currentDepartureTime = time;
	}

	/**
	 * @return Returns the currentLink.
	 */
	public Link getCurrentLink() {
		return this.currentLink;
	}

	/**
	 * @param currentLink The currentLink to set.
	 */
	public void setCurrentLink(final Link currentLink) {
		this.currentLink = currentLink;
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
	protected Link chooseNextLink() {
		if (this.cachedNextLink != null) {
			return this.cachedNextLink;
		}
		ArrayList<?> route = this.currentLeg.getRoute().getRoute();

		if (this.currentNodeIndex >= route.size() ) {
			// we have no more information for the route, so we should have arrived at the destination link
			if (this.currentLink.getToNode().equals(this.destinationLink.getFromNode())) {
				this.cachedNextLink = this.destinationLink;
				return this.cachedNextLink;
			}
			// there must be something wrong. Maybe the route is too short, or something else, we don't know...
			log.error("The vehicle with driver " + this.driverId + ", currently on link " + this.currentLink.getId().toString()
				+ ", is at the end of its route, but has not yet reached its destination link " + this.destinationLink.getId().toString());
			return null;
		}

		Node destNode = (Node)route.get(this.currentNodeIndex);

		for (Link link :  this.currentLink.getToNode().getOutLinks().values()) {
			if (link.getToNode() == destNode) {
				this.cachedNextLink = link; //save time in later calls, if link is congested
				return this.cachedNextLink;
			}
		}
		log.warn(this + " [no link to next routenode found: routeindex= " + this.currentNodeIndex + " ]");
		return null;
	}

	public Route getCurrentRoute() {
		return (Route)this.currentLeg.getRoute();
	}

	public Link getDestinationLink() {
		return this.destinationLink;
	}

	private boolean initNextLeg() {

		double now = SimulationTimer.getTime();
		Act act = (Act)this.actslegs.get(this.nextActivity);

		if (act.getLink() != this.currentLink) {
			log.error("The vehicle with driver " + this.driverId + " should be on link " + act.getLink().getId().toString()
					+ ", but is on link " + this.currentLink.getId().toString() + ". Removing the agent from the simulation.");
			return false;
		}

		if (this.nextActivity == this.actslegs.size()-1) {
			// if this is the last activity, then stop vehicle
			return false;
		}

		double departure = 0;

		/* WELL, THAT'S IMPORTANT:
		 * The person leaves the activity either 'actDur' later or
		 * when the end is defined of the activity, whatever comes first. */
		if (act.getDur() == Time.UNDEFINED_TIME) {
			departure = act.getEndTime();
		} else if (act.getEndTime() == Time.UNDEFINED_TIME) {
			departure = now + act.getDur();
		} else {
			departure = Math.min(act.getEndTime(), now + act.getDur());
		}
		if (departure < now) {
			// we cannot depart before we arrived, thus change the time so the timestamp in events will be right
			departure = now;
			// actually, we will depart in (now+1) because we already missed the departing in this time step
		}
		setDepartureTime_s(departure);

		this.destinationLink = ((Act)this.actslegs.get(this.nextActivity +2)).getLink();

		// set the route according to the next leg
		Leg leg = (Leg) this.actslegs.get(this.nextActivity+1);
		this.currentLeg = leg;
		this.currentNodeIndex = 1;
		this.cachedNextLink = null;
		this.nextActivity += 2;

		return true;
	}

	 protected boolean initVeh() {
		this.nextActivity = 0;
		Act firstAct = (Act) this.actslegs.get(0);

		SimulationTimer.updateSimStartTime(firstAct.getEndTime());
		setCurrentLink(firstAct.getLink());

		if (initNextLeg()) {
			Simulation.incLiving();
			// this is the starting point for our vehicle, so put it in the queue
			return true;
		}
		return false;
	}

	public Leg getCurrentLeg() {
		return (Leg) this.actslegs.get(this.nextActivity-1);
	}

	/**
	 * @param actLegs The actLegs to set.
	 */
	public void setActLegs(final List<Object> actLegs) {
		this.actslegs = actLegs;
	}

	/**
	 * @return Returns the iD.
	 */
	public Id getID() {
		return this.id;
	}

	@Override
	public String toString() {
		return "Vehicle Id " + getID() + ", driven by (personId) " + this.driverId
				+ ", on link " + this.currentLink.getId() + ", routeindex: " + this.currentNodeIndex
				+ ", next activity#: " + this.nextActivity;
	}

	/**
	 * @return Returns the driver.
	 */
	public Person getDriver() {
		return this.driver;
	}

	/**
	 * @param driver The driver to set.
	 */
	public void setDriver(final Person driver) {
		this.driver = driver;
		if (null != driver) {
			this.driverId = driver.getId().toString();
		}
	}

	public double getPosInLink_m() {

		double dur = this.currentLink.getFreespeedTravelTime(SimulationTimer.getTime());
		double mytime = getDepartureTime_s() - SimulationTimer.getTime();
		if (mytime<0) {
			mytime = 0.;
		}
		mytime/= dur;
		mytime = (1.-mytime)*this.currentLink.getLength();
		return mytime;
	}

	/** @return Always returns the value 1. */
	public int getLane() {
		return 1;
	}

	/**
	 * @return Returns the time the vehicle moved last.
	 */
	public double getLastMovedTime() {
		return this.lastMovedTime;
	}

	/**
	 * @param lastMovedTime The lastMovedTime to set.
	 */
	public void setLastMovedTime(final double lastMovedTime) {
		this.lastMovedTime = lastMovedTime;
	}

	/**
	 * Notifies the agent that it leaves its current activity location (and
	 * accordingly starts moving on its current route).
	 *
	 * @param now the current time
	 */
	protected void leaveActivity(final double now) {
		Act act = (Act)this.actslegs.get(this.nextActivity - 2);
		QueueSimulation.getEvents().processEvent(new ActEndEvent(now, this.driver, this.currentLink, act));
	}

	/**
	 * Notifies the agent that it reaches its aspired activity location.
	 *
	 * @param now the current time
	 * @param currentQueueLink
	 */
	 protected void reachActivity(final double now, QueueLink currentQueueLink) {
		Act act = (Act)this.actslegs.get(this.nextActivity);
		// no actStartEvent for first act.
		QueueSimulation.getEvents().processEvent(new ActStartEvent(now, this.driver, this.currentLink, act));
		// 	 this is the starting point for our vehicle, so put it in the queue
		if (!initNextLeg()) {
			Simulation.decLiving();
		}
		else {
			currentQueueLink.addParking(this);
		}
	}

}
