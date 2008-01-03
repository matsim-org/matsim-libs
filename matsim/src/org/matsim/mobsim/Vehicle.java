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

package org.matsim.mobsim;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.events.EventActivityEnd;
import org.matsim.events.EventActivityStart;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Route;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.netvis.DrawableAgentI;

public class Vehicle implements Serializable, DrawableAgentI {

	private static final long serialVersionUID = 1L;

	static private int globalID = 0;
	public double lastMoveTime = 0;
	protected String driverId;
	private int currentNode;
	private int nextActivity = 0;
	private double speed = 0.0;
	private double currentDepartureTime = 0;
	private double lastMovedTime = 0;

	protected List<Object> actslegs = new ArrayList<Object>();
	protected transient QueueLink cachedNextLink = null;
	private transient QueueLink destinationLink = null;
	private transient Person driver = null;
	protected transient QueueLink currentLink = null;
	protected transient BasicLeg currentLeg = null;

	private final int id = globalID++; // TODO change to IdI instead of int

	// return zero based leg number
	public int getCurrentLegNumber() {
		return ((this.nextActivity - 2)/ 2);
	};

	public double getDepartureTime_s() {
		return this.currentDepartureTime;
	}
	public void setDepartureTime_s(final double i) {
		this.currentDepartureTime = i;
	}

	/**
	 * @return Returns the currentLink.
	 */
	public QueueLink getCurrentLink() {
		return this.currentLink;
	}

	/**
	 * @param currentLink The currentLink to set.
	 */
	public void setCurrentLink(final QueueLink currentLink) {
		this.currentLink = currentLink;
	}

	public void incCurrentNode() {
		this.currentNode++;
		this.cachedNextLink = null; //reset cached nextLink
	}

	// has the main functionality of chooseNextLink, but other vehicles might
	// not want an error issued, when no link is found, but do something more elaborate!
	// so they can now easily override chooseNextLink...
	protected QueueLink findNextLink() {
		if (this.cachedNextLink != null) {
			return this.cachedNextLink;
		}
		ArrayList<?> route = this.currentLeg.getRoute().getRoute();

		if (this.currentNode >= route.size() ) {
			return this.destinationLink;
		}

		Node destNode = (Node)route.get(this.currentNode);

		for (Link link :  this.currentLink.getToNode().getOutLinks().values()) {
			if (link.getToNode() == destNode) {
				this.cachedNextLink = (QueueLink)link; //save time in later calls, if link is congested
				return this.cachedNextLink;
			}
		}
		return null;
	}

	public QueueLink chooseNextLink() {
		QueueLink result = findNextLink();
		if (result == null) {
			Gbl.warningMsg(this.getClass(), "chooseNextLink(...)", this
					+ " [no link to next routenode found: routeeindex= " + this.currentNode + " ]");
		}

		return result;
	}

	public Route getCurrentRoute() {
		return (Route)this.currentLeg.getRoute();
	}

	//public MobsimLinkI getDestinationLink() {
	public QueueLink getDestinationLink() {
		return this.destinationLink;
	}

	private boolean initNextLeg() {

		double now = SimulationTimer.getTime();
		Act act = (Act)this.actslegs.get(this.nextActivity);

		this.currentLink = (QueueLink) act.getLink();

		if ( this.nextActivity > 0 ) {
			// no actStartEvent for first act.
			QueueSimulation.getEvents().processEvent( new EventActivityStart(now, this.driverId, this.driver, this.currentLink, act));
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

		this.destinationLink = (QueueLink)((Act)this.actslegs.get(this.nextActivity +2)).getLink();

		// set the route according to the next leg
		Leg leg = (Leg) this.actslegs.get(this.nextActivity+1);
		this.currentLeg = leg;
		this.currentNode = 1;
		this.cachedNextLink = null;
		this.nextActivity += 2;

		// this is the starting point for our vehicle, so put it in the queue
		transferToMobsim();

		QueueSimulation.getEvents().processEvent( new EventActivityEnd(departure, this.driverId, this.driver, this.currentLink, act));

		return true;
	}

	public void initVeh() {
		this.nextActivity = 0;
		SimulationTimer.updateSimStartTime(((Act)this.actslegs.get(0)).getEndTime());

		if (initNextLeg()) {
			Simulation.incLiving();
		}
	}

	public void rebuildVeh(final QueueLink link) {
		this.currentLink = link;
		this.destinationLink = (QueueLink)((Act)this.actslegs.get(this.nextActivity)).getLink();
		Leg actleg = (Leg) this.actslegs.get(this.nextActivity-1);
		this.currentLeg = actleg;
		this.cachedNextLink = null;
	}

	// this second variant is only used for "teleportation" aka QueueLink,line206
	// because otherwise ActivityStartEvent would be before ArrivalEvent in timeline
	private void reinitVeh() {
		if (!initNextLeg()) {
			Simulation.decLiving();
		}
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
	 * @param driverId The driverId to set.
	 */
	public void setDriverID(final String driverId) {
		this.driverId = driverId;
	}

	/**
	 * @return Returns the iD.
	 */
	public int getID() {
		return this.id;
	}

	@Override
	public String toString() {
		return "Vehicle Id " + getID() + ", driven by (personId) " + this.driverId
				+ ", on link " + this.currentLink.getId() + ", routeindex: " + this.currentNode
				+ ", next activity#: " + this.nextActivity;
	}

	/**
	 * @return Returns the speed.
	 */
	public double getSpeed() {
		return this.speed;
	}

	/**
	 * @param speed The speed to set.
	 */
	public void setSpeed(final double speed) {
		this.speed = speed;
	}

	/**
	 * @return Returns the driverID.
	 */
	public String getDriverID() {
		return this.driverId;
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
		double dur = this.currentLink.getFreeTravelDuration();
		double mytime = getDepartureTime_s() - SimulationTimer.getTime();
		if (mytime<0) {
			mytime = 0.;
		}
		mytime/= dur;
		mytime = (1.-mytime)*this.currentLink.getLength();
		return mytime;
	}

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

	// The next two methods were taken from MobsimAgentI that
	// I cannot fully implement right now, but will maybe later on

	/**
	 * Notifies the agent that it leaves its current activity location (and
	 * accordingly starts moving on its current route).
	 */
	public void leaveActivity() {
	}

	/**
	 * Notifies the agent that it reaches its aspired activity location.
	 */
	public void reachActivity() {
		// 	 this is the starting point for our vehicle, so put it in the queue
		reinitVeh();
	}

	protected void transferToMobsim() {
		this.currentLink.addParking(this);
	}


}
