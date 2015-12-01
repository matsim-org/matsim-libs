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

	public LinkPersonInfoContainer(final Id<Link> linkId){
		this.linkId = linkId;
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

	public PersonPositionChecker getOrCreatePersonPositionChecker(Id<Person> personId){
		if(! person2PersonPositionChecker.containsKey(personId) ) person2PersonPositionChecker.put(personId, new PersonPositionChecker(personId));
		return this.person2PersonPositionChecker.get(personId);
	}
	
	public class PersonPositionChecker {
		private final EnteringPersonInfo enteredPerson;
		private LeavingPersonInfo leftPerson;
		private final double linkEnterTime;
		private final String legMode;
		private final Link link;
		private double availableLinkSpace ;
		private double queuingTime;
		private boolean isPersonQueued = false;

		PersonPositionChecker(final Id<Person> personId){
			this.enteredPerson = person2EnteringPersonInfo.get(personId);
			this.leftPerson = person2LeavingPersonInfo.get(personId);
			this.legMode = enteredPerson.getLegMode();
			this.link = enteredPerson.getLink();
			this.linkEnterTime = enteredPerson.getLinkEnterTime();
			this.availableLinkSpace = this.link.getLength();
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

		public boolean isAddingVehicleInQueue(final double currentTimeStep) {
			double travelTimeSincePersonHasEntered = currentTimeStep - this.linkEnterTime;
			if(leftPerson!=null && currentTimeStep == this.leftPerson.getLinkLeaveTime()) this.isPersonQueued = false; 
			else {
				this.isPersonQueued = travelTimeSincePersonHasEntered >= Math.floor(getFreeSpeedLinkTravelTime()) + 1;
			}
			return this.isPersonQueued;
		}
		
		public boolean isPersonAlreadyQueued(){
			return this.isPersonQueued;
		}

		public void updateAvailableLinkSpace(final double availableLinkSpace) {
			if(availableLinkSpace < 0) this.availableLinkSpace = 0.;
			else this.availableLinkSpace = availableLinkSpace;
		}

		/**
		 * This sets the actual queuing time in fraction without any rounding and 
		 * it depends on the number of vehicles queued at the end of the link.
		 */
		public double getQueuingTime(){
			this.queuingTime = this.linkEnterTime + this.availableLinkSpace / Math.min(this.link.getFreespeed(), MixedTrafficVehiclesUtils.getSpeed(this.legMode));
			return this.queuingTime;
		}

		/**
		 * @return the availableLinkSpace
		 */
		public double getAvailableLinkSpace() {
			return availableLinkSpace;
		}
		
		public void updatePresonLeavingInfo(){
			// while updating position of entered persons, leaving info is not available thus this is required at the later stage.
		if(this.leftPerson == null)	this.leftPerson = person2LeavingPersonInfo.get(this.enteredPerson.getPersonId());
		}
	}
}