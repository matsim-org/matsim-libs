/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.plots;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;

/**
 * @author amit
 */

public class LinkPersonInfoContainer {

	private final Id<Link> linkId ;
	private final Map<Id<Person>, LeavingPersonInfo> person2LeavingPersonInfo = new HashMap<>();
	private final Map<Id<Person>, EnteringPersonInfo> person2EnteringPersonInfo = new HashMap<>();
	private final Map<Id<Person>, PersonPositionChecker> person2PersonPositionChecker = new HashMap<>();
	private final Queue<Id<Person>> agentsOnLink = new LinkedList<>();
	private final Queue<Id<Person>> agentsInQueue= new LinkedList<>();
	private double remainingLinkSpace;

	public LinkPersonInfoContainer(final Id<Link> linkId, final double linkLength){
		this.linkId = linkId;
		this.remainingLinkSpace = linkLength;
	}

	public double getRemainingLinkSpace() {
		return remainingLinkSpace;
	}
	public void updateRemainingLinkSpace(final double remainingLinkSpace) {
		this.remainingLinkSpace = remainingLinkSpace;
	}
	public Id<Link> getLinkId() {
		return linkId;
	}
	public Map<Id<Person>, LeavingPersonInfo> getPerson2LeavingPersonInfo() {
		return person2LeavingPersonInfo;
	}
	public Queue<Id<Person>> getAgentsOnLink() {
		return agentsOnLink;
	}
	public Queue<Id<Person>> getAgentsInQueue() {
		return agentsInQueue;
	}
	public Map<Id<Person>, EnteringPersonInfo> getPerson2EnteringPersonInfo() {
		return person2EnteringPersonInfo;
	}

	public PersonPositionChecker getPersonInfoChecker(final Id<Person> personId){
		if(! person2PersonPositionChecker.containsKey(personId) || person2PersonPositionChecker.get(personId).getLeftPersonInfo()==null ) person2PersonPositionChecker.put(personId, new PersonPositionChecker(personId));
		return person2PersonPositionChecker.get(personId);
	}

	public class PersonPositionChecker {
		private final EnteringPersonInfo enteredPerson;
		private final LeavingPersonInfo leftPerson;
		private final double linkEnterTime;
		private final String legMode;
		private final Link link;
		private boolean addVehicleInQ;
		private double availableLinkSpace = Double.NEGATIVE_INFINITY ;
		private double queuingTime;

		public PersonPositionChecker(final Id<Person> personId){
			this.enteredPerson = person2EnteringPersonInfo.get(personId);
			this.leftPerson = person2LeavingPersonInfo.get(personId);
			this.legMode = enteredPerson.getLegMode();
			this.link = enteredPerson.getLink();
			this.linkEnterTime = enteredPerson.getLinkEnterTime();
		}
		
		private double getFreeSpeedLinkTravelTime() {
			return this.availableLinkSpace / Math.min(this.link.getFreespeed(), MixedTrafficVehiclesUtils.getSpeed(this.legMode));
		}

		public EnteringPersonInfo getEnteredPersonInfo() {
			return this.enteredPerson;
		}

		public LeavingPersonInfo getLeftPersonInfo() {
			return this.leftPerson;
		}

		public void checkIfVehicleWillGoInQ(final double currentTimeStep){
			double travelTimeSincePersonHasEntered = currentTimeStep - this.linkEnterTime;
			if(leftPerson!=null) {
				if(currentTimeStep != this.leftPerson.getLinkLeaveTime()){
					this.addVehicleInQ= travelTimeSincePersonHasEntered > Math.floor(getFreeSpeedLinkTravelTime()) + 1;
				} else this.addVehicleInQ=false;
			} else {
				this.addVehicleInQ = travelTimeSincePersonHasEntered > Math.floor(getFreeSpeedLinkTravelTime()) + 1;
			}
		}

		public boolean isAddingVehicleInQueue() {
			return this.addVehicleInQ;
		}

		public void updateAvailableLinkSpace(final double availableLinkSpace) {
			if(availableLinkSpace < 0) this.availableLinkSpace = 0.;
			else this.availableLinkSpace = availableLinkSpace;
		}

		public double getQueuingTime(){
			return this.queuingTime;
		}

		/**
		 * @param availableSpaceSoFar
		 * This sets the actual queuing time in fraction without any rounding
		 */
		public void updateQueuingTime(final double availableSpaceSoFar){
			this.queuingTime = this.linkEnterTime + availableSpaceSoFar / Math.min(this.link.getFreespeed(), MixedTrafficVehiclesUtils.getSpeed(this.legMode));
		}
	}
}