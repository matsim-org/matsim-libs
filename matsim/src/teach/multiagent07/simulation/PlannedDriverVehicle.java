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
import org.matsim.basic.v01.BasicLink;
import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.BasicRoute;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;

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
	private BasicPlan.ActLegIterator iter = null;
	private double departuretime = 0;

	public PlannedDriverVehicle(Person agent, CAMobSim sim) {
		this.id = agent.getId();

		this.plan = agent.getSelectedPlan();

		this.iter = plan.getIterator();
		lastAct = (Activity)iter.nextAct();
		currentlink = lastAct.getLink();
		departuretime = lastAct.getEndTime();

		currentLeg = iter.nextLeg();
		currentRoute = currentLeg.getRoute();

		nextAct = (Activity)iter.nextAct();
	}

	@Override
	public CALink getDestinationLink() {
		return (CALink)nextAct.getLink();
	}

	@Override
	public BasicLink getDepartureLink() {
		return currentlink;
	}

	@Override
	public double getDepartureTime() {
		return departuretime;
	}

	@Override
	public void setCurrentLink(BasicLink link)  {
		currentlink = link;
		routeidx++;
	}

	@Override
	public CALink getNextLink(Collection<? extends BasicLinkI> nextLinks) {

		if (routeidx >= currentRoute.getRoute().size() ) return getDestinationLink();

		CANode destNode = (CANode)currentRoute.getRoute().get(routeidx);

		for (BasicLinkI link : nextLinks) {
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
				                currentlink, this.getId(), legnumber);
		CAMobSim.getEventManager().addEvent(event);
	}

	@Override
	public void reachActivity(){
		if (initNextLeg()) {
			// re-put myself into simulation
			((CALink)currentlink).addParking(this);

			Event event = new Event(CAMobSim.getCurrentTime(), Event.ACT_ARRIVAL,
	                currentlink, this.getId(), legnumber);
			CAMobSim.getEventManager().addEvent(event);
		}
	}

	public boolean initNextLeg() {
		// now point to next activity
		if ( iter.hasNextLeg()) {
			// actlegidx points to current act == departurelink
			lastAct = nextAct;
			currentLeg = iter.nextLeg();
			legnumber++;

			nextAct = (Activity)iter.nextAct();

			departuretime = lastAct.getEndTime();

			routeidx = 0;
			currentRoute = currentLeg.getRoute();
			return true;
		}
		return false;
	}
}

