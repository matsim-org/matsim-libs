/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.mobsim.jdeqsim;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;

/**
 * Represents a vehicle.
 *
 * @author rashid_waraich
 */
public class Vehicle extends SimUnit {

	private static final Logger log = Logger.getLogger(Vehicle.class);
	private Person ownerPerson = null;
	private Leg currentLeg = null;
	private int legIndex;
	private Link currentLink = null;
	private int linkIndex;
	private Link[] currentLinkRoute = null;

	public Vehicle(Scheduler scheduler, Person ownerPerson) {
		super(scheduler);
		this.ownerPerson = ownerPerson;
		initialize();
	}

	// put the first start leg event into the message queue
	public void initialize() {

		/*
		 * we must start with linkIndex=-1, because the first link on which the
		 * start activity resides is not in the Leg. So, for being consistent
		 * with the rest of the simulation, we start with linkIndex=-1
		 */
		linkIndex = -1;

		/*
		 * return at this position, if we are just testing using a dummy
		 * person/plan (to avoid null pointer exception)
		 */
		if (ownerPerson.getSelectedPlan() == null) {
			return;
		}

		Plan plan = ownerPerson.getSelectedPlan();
		List<? extends BasicPlanElement> actsLegs = plan.getPlanElements();
		// actsLegs(0) is the first activity, actsLegs(1) is the first leg
		legIndex = 1;
		setCurrentLeg((Leg) actsLegs.get(legIndex));
		Activity firstAct=(Activity) actsLegs.get(0);
		// an agent starts the first leg at the end_time of the fist act
		double departureTime = firstAct.getEndTime();

		// this is the link, where the first activity took place
		setCurrentLink(firstAct.getLink());

		Road road = Road.getRoad(getCurrentLink().getId().toString());
		// schedule start leg message
		scheduleStartingLegMessage(departureTime, road);
	}

	/**
	 * based on the current Leg, the previous activity is computed; this could
	 * be implemented more efficiently in future.
	 * 
	 * @return
	 */
	public Activity getPreviousActivity() {
		Plan plan = ownerPerson.getSelectedPlan();
		List<? extends BasicPlanElement> actsLegs = plan.getPlanElements();

		for (int i = 0; i < actsLegs.size(); i++) {
			if (actsLegs.get(i) == currentLeg) {
				return ((Activity) actsLegs.get(i - 1));
			}
		}
		return null;
	}

	/**
	 * based on the current Leg, the next activity is computed; this could be
	 * implemented more efficiently in future.
	 * 
	 * @return
	 */
	public Activity getNextActivity() {
		Plan plan = ownerPerson.getSelectedPlan();
		List<? extends BasicPlanElement> actsLegs = plan.getPlanElements();

		for (int i = 0; i < actsLegs.size(); i++) {
			if (actsLegs.get(i) == currentLeg) {
				return ((Activity) actsLegs.get(i + 1));
			}
		}
		return null;
	}

	public void setCurrentLeg(Leg currentLeg) {
		this.currentLeg = currentLeg;
		List<Link> links = ((NetworkRoute) currentLeg.getRoute()).getLinks();
		currentLinkRoute = links.toArray(new Link[links.size()]);
	}

	private Link[] getCurrentLinkRoute() {
		return currentLinkRoute;
	}

	public void setLegIndex(int legIndex) {
		this.legIndex = legIndex;
	}

	public Person getOwnerPerson() {
		return ownerPerson;
	}

	public Leg getCurrentLeg() {
		return currentLeg;
	}

	public int getLegIndex() {
		return legIndex;
	}

	public Link getCurrentLink() {
		return currentLink;
	}

	public int getLinkIndex() {
		return linkIndex;
	}

	public void setCurrentLink(Link currentLink) {
		this.currentLink = currentLink;
	}

	public void setLinkIndex(int linkIndex) {
		this.linkIndex = linkIndex;
	}

	public boolean isCurrentLegFinished() {
		return getCurrentLinkRoute().length == getLinkIndex() + 1;
	}

	/**
	 * updates both the currentLink and link index variables with the next link
	 * in the link route of the current leg attention: only applicable, if
	 * isCurrentLegFinished==false
	 */
	public void moveToNextLinkInLeg() {
		setLinkIndex(getLinkIndex() + 1);
		setCurrentLink(getCurrentLinkRoute()[getLinkIndex()]);
	}

	// note: does not affect the link index
	public void moveToFirstLinkInNextLeg() {
		Plan plan = getOwnerPerson().getSelectedPlan();
		List<? extends BasicPlanElement> actsLegs = plan.getPlanElements();
		setCurrentLink(((Activity) actsLegs.get(getLegIndex() + 1)).getLink());
	}

	/**
	 * find out, if the vehicle is in endingLegMode this means, that the vehicle
	 * is just waiting until it can enter the last link (without entering it)
	 * and then ends the leg
	 * 
	 * @return
	 */
	public boolean isEndingLegMode() {
		return (getCurrentLinkRoute().length == getLinkIndex());
	}

	// invoking this method causes the "isEndingLegMode" method to return true
	public void initiateEndingLegMode() {
		linkIndex = getCurrentLinkRoute().length;
	}

	public void scheduleEnterRoadMessage(double scheduleTime, Road road) {
		/*
		 * before entering the new road, we must leave the previous road (if
		 * there is a previous road) the first link does not need to be left
		 * (which has index -1)
		 */
		if (this.getLinkIndex() >= 0) {
			scheduleLeavePreviousRoadMessage(scheduleTime);
		}

		if (isEndingLegMode()) {
			/*
			 * attention: as we are not actually entering the road, we need to
			 * give back the promised space to the road else a precondition of
			 * the enterRequest would not be correct any more (which involves
			 * the noOfCarsPromisedToEnterRoad variable)
			 */
			road.giveBackPromisedSpaceToRoad(); // next road
			scheduleEndLegMessage(scheduleTime, road);
		} else {
			_scheduleEnterRoadMessage(scheduleTime, road);
		}
	}

	public void scheduleLeavePreviousRoadMessage(double scheduleTime) {
		Road previousRoad = null;
		Link previousLink = null;
		/*
		 * we need to handle the first road in a leg specially, because the load
		 * to be left is accessed over the last act performed instead of the leg
		 */
		if (this.getLinkIndex() == 0) {
			Plan plan = ownerPerson.getSelectedPlan();
			List<? extends BasicPlanElement> actsLegs = plan.getPlanElements();
			previousLink = ((Activity) actsLegs.get(legIndex - 1)).getLink();
			previousRoad = Road.getRoad(previousLink.getId().toString());
		} else if (this.getLinkIndex() >= 1) {
			previousLink = this.getCurrentLinkRoute()[this.getLinkIndex() - 1];
			previousRoad = Road.getRoad(previousLink.getId().toString());
		} else {
			log.error("Some thing is wrong with the simulation: Why is this.getLinkIndex() negative");
		}

		scheduleLeaveRoadMessage(scheduleTime, previousRoad);
	}

	protected void _scheduleEnterRoadMessage(double scheduleTime, Road road) {
		sendMessage(MessageFactory.getEnterRoadMessage(road.scheduler, this), road, scheduleTime);
	}

	public void scheduleEndRoadMessage(double scheduleTime, Road road) {
		sendMessage(MessageFactory.getEndRoadMessage(road.scheduler, this), road, scheduleTime);
	}

	public void scheduleLeaveRoadMessage(double scheduleTime, Road road) {
		sendMessage(MessageFactory.getLeaveRoadMessage(road.scheduler, this), road, scheduleTime);
	}

	public void scheduleEndLegMessage(double scheduleTime, Road road) {
		sendMessage(MessageFactory.getEndLegMessage(road.scheduler, this), road, scheduleTime);
	}

	public void scheduleStartingLegMessage(double scheduleTime, Road road) {
		sendMessage(MessageFactory.getStartingLegMessage(road.scheduler, this), road, scheduleTime);
	}

	public DeadlockPreventionMessage scheduleDeadlockPreventionMessage(double scheduleTime, Road road) {
		DeadlockPreventionMessage dpMessage = MessageFactory.getDeadlockPreventionMessage(road.scheduler,
				this);
		sendMessage(dpMessage, road, scheduleTime);
		return dpMessage;
	}

}
