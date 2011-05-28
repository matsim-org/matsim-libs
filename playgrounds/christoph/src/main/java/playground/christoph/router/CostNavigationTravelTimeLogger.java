/* *********************************************************************** *
 * project: org.matsim.*
 * CostNavigationTravelTimeLogger.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.router;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.util.TravelTime;

public class CostNavigationTravelTimeLogger implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler {

	private Scenario scenario;
	private TravelTime travelTime;
	
	private Map<Id, PersonInfo> personInfos;
	private Map<Id, Boolean> followed;
	private Map<Id, Double> enterTimes;
	private Map<Id, Double> expectedTravelTimes;
	
	protected double toleranceSlower = 1.2;
	protected double toleranceFaster = 0.9;
	
//	protected double initialGamma = 0.5;
	
	public CostNavigationTravelTimeLogger(Scenario scenario, TravelTime travelTime) {
		this.scenario = scenario;
		this.travelTime = travelTime;
		
		personInfos = new HashMap<Id, PersonInfo>();
		followed = new HashMap<Id, Boolean>();
		enterTimes = new HashMap<Id, Double>();
		expectedTravelTimes = new HashMap<Id, Double>();
		
		init();
	}
	
	private void init() {
		
		// set initial gammas for the whole population
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			personInfos.put(person.getId(), new PersonInfo());
		}
		
		followed.clear();
		enterTimes.clear();
		expectedTravelTimes.clear();
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id personId = event.getPersonId();
		Id linkId = event.getLinkId();
		double time = event.getTime();

		enterTimes.put(personId, time);
		expectedTravelTimes.put(personId, travelTime.getLinkTravelTime(scenario.getNetwork().getLinks().get(linkId), time));
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id personId = event.getPersonId();
		Id linkId = event.getLinkId();
		double time = event.getTime();
		
		/*
		 * If the agent has just ended an activity, there is no
		 * entry in the enterTimes map. Therefore, we have nothing
		 * else to do.
		 */
		if (!enterTimes.containsKey(personId)) return;
		
		double enterTime = enterTimes.get(personId);
		double expectedTravelTime = expectedTravelTimes.get(personId);
		double travelTime = time - enterTime;
		boolean hasFollowed = followed.get(personId);
		PersonInfo personInfo = personInfos.get(personId);
		
		/*
		 * estimation > 1: slower than expected
		 * estimation < 1: faster than expected 
		 */
		double estimation = travelTime / expectedTravelTime;
		
		if (hasFollowed) {
			if (estimation > toleranceSlower) {
				personInfo.followedAndNotAccepted++;
			} else if (estimation < toleranceFaster) {
				personInfo.followedAndNotAccepted++;
			} else {
				personInfo.followedAndAccepted++;
			}
		} else {
			if (estimation > toleranceSlower) {
				personInfo.notFollowedAndNotAccepted++;
			} else if (estimation < toleranceFaster) {
				personInfo.notFollowedAndNotAccepted++;
			} else {
				personInfo.notFollowedAndAccepted++;
			}			
		}		
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Id personId = event.getPersonId();
		
		enterTimes.remove(personId);
		expectedTravelTimes.remove(personId);
		followed.remove(personId);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		// nothing to do here...
	}

	@Override
	public void reset(int iteration) {
		init();
	}

	public double getTrust(Id personId) {
		return personInfos.get(personId).getTrust();
	}
	
//	public double getTrustFollowedAndAccepted(Id personId) {
//		return personInfos.get(personId).getTrustFollowedAndAccepted();
//	}
	
//	public double getTrustNotFollowedAndNotAccepted(Id personId) {
//		return personInfos.get(personId).getTrustNotFollowedAndNotAccepted();
//	}
	
	public double getRandomNumber(Id personId) {
		return personInfos.get(personId).getRandomNumber();
	}
		
	private class PersonInfo {
		/*
		 * Initialize counters with 1 - as a result, the first returned
		 * trust value will be 0.5
		 */
		int followedAndAccepted = 1;
		int followedAndNotAccepted = 1;
		int notFollowedAndAccepted = 1;
		int notFollowedAndNotAccepted = 1;
		
		Random random = MatsimRandom.getLocalInstance();
		
		public double getTrust() {
			int observations = (followedAndAccepted + followedAndNotAccepted + notFollowedAndAccepted + notFollowedAndNotAccepted);
			return (followedAndAccepted + notFollowedAndNotAccepted) / observations;
		}
		
//		public double getTrustFollowedAndAccepted() {
//			return followedAndAccepted / (followedAndAccepted + followedAndNotAccepted);
//		}
		
//		public double getTrustNotFollowedAndNotAccepted() {
//			return notFollowedAndNotAccepted / (notFollowedAndAccepted + notFollowedAndNotAccepted);
//		}
		
		public double getRandomNumber() {
			return random.nextDouble();
		}
	}
}
