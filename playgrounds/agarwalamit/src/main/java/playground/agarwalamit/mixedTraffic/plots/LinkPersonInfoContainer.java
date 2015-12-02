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
	private double availableLinkSpace ;

	public LinkPersonInfoContainer(final Id<Link> linkId, final double linkLength){
		this.linkId = linkId;
		this.availableLinkSpace = linkLength;
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

	private PersonPositionChecker createAndReturnPersonPositionChecker(Id<Person> personId){
		PersonPositionChecker checker = new PersonPositionChecker(personId);
		this.person2PersonPositionChecker.put(personId, checker);
		return checker;
	}

	public Map<Id<Person>, PersonPositionChecker> getPerson2PersonPositionChecker() {
		return person2PersonPositionChecker;
	}
	
	public PersonPositionChecker getOrCreatePersonPositionChecker(Id<Person> personId){
		PersonPositionChecker checker = this.person2PersonPositionChecker.get(personId);
		if(checker == null) return createAndReturnPersonPositionChecker(personId);
		else {
			checker.updatePresonInfo();
		}
		return checker;
	}
	
	public double getAvailableLinkSpace() {
		return availableLinkSpace;
	}
	
	public void setAvailableLinkSpace(final double availableLinkSpace) {
		if(availableLinkSpace < 0) this.availableLinkSpace = 0.;
		else this.availableLinkSpace = availableLinkSpace;
	}

	public class PersonPositionChecker {
		private EnteringPersonInfo enteredPerson;
		private LeavingPersonInfo leftPerson;
		private final Id<Person> personId;
		private final double linkEnterTime;
		private final String legMode;
		private final Link link;
		private double queuingTime;
		private boolean isPersonQueued = false;
		private int cycleNumer = 1;

		private PersonPositionChecker(final Id<Person> personId){
			this.personId = personId;
			this.enteredPerson = person2EnteringPersonInfo.get(personId);
			this.leftPerson = person2LeavingPersonInfo.get(personId);
			this.legMode = enteredPerson.getLegMode();
			this.link = enteredPerson.getLink();
			this.linkEnterTime = enteredPerson.getLinkEnterTime();
		}

		private double getFreeSpeedLinkTravelTime() {
			return availableLinkSpace / Math.min(this.link.getFreespeed(), MixedTrafficVehiclesUtils.getSpeed(this.legMode));
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
			else this.isPersonQueued = travelTimeSincePersonHasEntered >= Math.floor(getFreeSpeedLinkTravelTime()) + 1;
			return this.isPersonQueued;
		}

		public boolean isPersonAlreadyQueued(){
			return this.isPersonQueued;
		}
		
		public double getQueuingTime(){
			return this.queuingTime;
		}
		
		/**
		 * This sets the actual queuing time in fraction without any rounding and 
		 * it depends on the number of vehicles queued at the end of the link.
		 */
		public void updateQueuingTime(){
			this.queuingTime = this.linkEnterTime + availableLinkSpace / Math.min(this.link.getFreespeed(), MixedTrafficVehiclesUtils.getSpeed(this.legMode));
		}

		/**
		 * A person may re-appear on the same link.
		 */
		public void updatePresonInfo(){
			this.leftPerson = person2LeavingPersonInfo.get(this.personId);
			this.enteredPerson = person2EnteringPersonInfo.get(this.personId);
		}
		
		public void updateCycleNumberOfPerson(){
			this.cycleNumer++;
		}
		
		public int getCycleNumber(){
			return this.cycleNumer;
		}
		
		public Id<Person> getPersonId(){
			return this.personId;
		}
		
		public Link getLink(){
			return this.link;
		}
	}
}