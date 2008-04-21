/* *********************************************************************** *
 * project: org.matsim.*
 * PlannedDriverVehicle.java
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

package teach.multiagent07.simulation;

import java.util.Collection;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.BasicLinkImpl;
import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.BasicPlanImpl;
import org.matsim.basic.v01.BasicRoute;
import org.matsim.interfaces.networks.basicNet.BasicLink;

import teach.multiagent07.net.CALink;
import teach.multiagent07.net.CANode;
import teach.multiagent07.population.Activity;
import teach.multiagent07.population.Person;
import teach.multiagent07.util.Event;


public class PlannedDriverVehicle extends Vehicle {

	private BasicPlan plan;

	private BasicLeg currentLeg = null;
	private BasicLink currentlink;
	private BasicRoute currentRoute;
	private Activity lastAct = null;
	private Activity nextAct = null;

	private int routeidx = 0;
	private BasicPlanImpl.ActLegIterator iter = null;
	private double departuretime = 0;

	public PlannedDriverVehicle(Person agent, CAMobSim sim) {
		this.id = agent.getId();

		this.plan = agent.getSelectedPlan();

		this.iter = this.plan.getIterator();
		this.lastAct = (Activity)this.iter.nextAct();
		this.currentlink = this.lastAct.getLink();
		this.departuretime = this.lastAct.getEndTime();

		this.currentLeg = this.iter.nextLeg();
		this.currentRoute = this.currentLeg.getRoute();

		this.nextAct = (Activity)this.iter.nextAct();
	}

	@Override
	public CALink getDestinationLink() {
		return (CALink)this.nextAct.getLink();
	}

	@Override
	public BasicLink getDepartureLink() {
		return this.currentlink;
	}

	@Override
	public double getDepartureTime() {
		return this.departuretime;
	}

	@Override
	public void setCurrentLink(BasicLinkImpl link)  {
		this.currentlink = link;
		this.routeidx++;
	}

	@Override
	public CALink getNextLink(Collection<? extends BasicLink> nextLinks) {

		if (this.routeidx >= this.currentRoute.getRoute().size() ) return getDestinationLink();

		CANode destNode = (CANode)this.currentRoute.getRoute().get(this.routeidx);

		for (BasicLink link : nextLinks) {
			if (link.getToNode() == destNode) {
				return (CALink)link;
			}
		}

		return null;
	}

	private int legnumber = 0;

	@Override
	public void leaveActivity() {
		Event event = new Event(CAMobSim.getCurrentTime(), Event.ACT_DEPARTURE,
				                this.currentlink, this.getId(), this.legnumber);
		CAMobSim.getEventManager().addEvent(event);
	}

	@Override
	public void reachActivity(){
		if (initNextLeg()) {
			// re-put myself into simulation
			((CALink)this.currentlink).addParking(this);

			Event event = new Event(CAMobSim.getCurrentTime(), Event.ACT_ARRIVAL,
	                this.currentlink, this.getId(), this.legnumber);
			CAMobSim.getEventManager().addEvent(event);
		}
	}

	public boolean initNextLeg() {
		// now point to next activity
		if ( this.iter.hasNextLeg()) {
			// actlegidx points to current act == departurelink
			this.lastAct = this.nextAct;
			this.currentLeg = this.iter.nextLeg();
			this.legnumber++;

			this.nextAct = (Activity)this.iter.nextAct();

			this.departuretime = this.lastAct.getEndTime();

			this.routeidx = 0;
			this.currentRoute = this.currentLeg.getRoute();
			return true;
		}
		return false;
	}
}

