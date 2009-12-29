/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialInteractor.java
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

package org.matsim.socialnetworks.interactions;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.matsim.gbl.Gbl;
import org.matsim.population.Person;
import org.matsim.socialnetworks.socialnet.SocialNetwork;


public class SpatialInteractor {

	SocialNetwork net;

	double pBecomeFriends = Double.parseDouble(Gbl.getConfig().socnetmodule().getPBefriend());// [0.0,1.0]

//	double pct_interacting = Double.parseDouble(Gbl.getConfig().socnetmodule().getFractSInteract());// [0.0,1.0]

	String interaction_type = Gbl.getConfig().socnetmodule().getSocNetInteractor2();

	public SpatialInteractor(SocialNetwork snet) {
		this.net = snet;
	}

	public void interact(Collection<SocialAct> events, HashMap<String, Double> rndEncounterProb, int iteration) {

		System.out.println(" "+ this.getClass()+" Looking through social events and letting Persons interact "+iteration);
		for (SocialAct event : events) {

			if (event.getAttendees().size() > 1) {

//				Shuffle the list of attendees at the socializing opportunity
//				and let a percentage of them interact
				List<Person> people = java.util.Collections.list(java.util.Collections.enumeration(event.getAttendees()));
				java.util.Collections.shuffle(people, Gbl.random);
				Object personList[] = people.toArray();
				int numPersons = personList.length;

//				for (int i = 0; i < (int) (numPersons * pct_interacting); i++) {
				for (int i = 0; i < numPersons; i++) {
					Person p1 = (Person) personList[i];

					if (interaction_type.equals("random")) {
						encounterOnePersonRandomlyFaceToFace(event, rndEncounterProb, p1, iteration);
					}else if (interaction_type.equals("meetall")){
						makeSocialLinkToAll(event,rndEncounterProb,p1,iteration);
					}else if(interaction_type.equals("timewindowrandom")){
						double t1=event.getArrivalTime(p1);
						double t2=event.getDepartureTime(p1);
						encounterOnePersonRandomlyFaceToFaceInTimeWindow(event, rndEncounterProb, p1, t1, t2, iteration);
					}else if(interaction_type.equals("timewindowall")){
						double t1=event.getArrivalTime(p1);
						double t2=event.getDepartureTime(p1);
						encounterAllPersonsFaceToFaceInTimeWindow(event, rndEncounterProb, p1, t1, t2, iteration);
					}else if(interaction_type.equals("MarchalNagelChain")){
						double t1=event.getArrivalTime(p1);
						double t2=event.getDepartureTime(p1);
						makeSocialLinkBetweenLastTwo(event,rndEncounterProb,p1,t1, t2,iteration);
					} else {
						Gbl.errorMsg(" Spatial interaction type is \"" + interaction_type
								+ "\". Only \"random\" and \"timewindow\" are supported at this time.");
					}
				}
			}
		}
		System.out.println("...finished");
	}

	private void makeSocialLinkBetweenLastTwo(SocialAct event,
			HashMap<String, Double> rndEncounterProbability, Person p1, double startTime, double endTime, int iteration) {
		/**
		 * Based on Marchal and Nagel 2007, TRR 1935
		 * Person p1 meets and befriends the person who arrived just before him, if
		 * this person is still at the SocialAct.
		 * The natural order of the agents in the SocialAct HashMap is not the order of arrival,
		 * so sorting is necessary.
		 * Cycle through all the agents who were co-present with p1 at the SocialAct
		 * and find the agent whose arrival time is closest to and less than that of p1
		 * Subject to the likelihood of a meeting taking place in a given facility type
		 */

		Person nextInQueue=null;
		if(Gbl.random.nextDouble() <rndEncounterProbability.get(event.activity.getType())){

			Vector<Person> persons = event.getAttendeesInTimeWindow(p1,startTime,endTime);
			if(persons!=null && persons.size()>0){
				int size = persons.size();
				nextInQueue = event.getAttendeeInTimeWindow(p1,startTime,endTime,0);
				for(int i=0; i<size;i++){
					Person p2 = event.getAttendeeInTimeWindow(p1, startTime, endTime, i);
					if(event.getArrivalTime(p2)<=event.getArrivalTime(p1)){
						if(event.getArrivalTime(p2)<=event.getArrivalTime(nextInQueue)){
							nextInQueue=p2;
						}
					}
				}
				if(p1.getKnowledge().getEgoNet().knows(nextInQueue)){
				} else {
					// If the two do not already know each other,
					net.makeSocialContact(p1,nextInQueue,iteration,"new"+event.activity.getType());
				}
			}
		}
	}

	private void makeSocialLinkToAll(SocialAct event, HashMap<String, Double> rndEncounterProbability, Person p1, int iteration) {

		// Person p1 encounters all persons p2 at the socializing opportunity
		// the probability of encountering depends on the activity type

		if(Gbl.random.nextDouble() <rndEncounterProbability.get(event.activity.getType())){

			Vector<Person> persons = event.getAttendees();
			if(persons!=null){
				int size = persons.size();
				for(int i=0; i<size;i++){
					Person p2=persons.get(i);
					if(p1.getKnowledge().getEgoNet().knows(p2)){
					} else {
						// If the two do not already know each other,


						net.makeSocialContact(p1,p2,iteration,"new"+event.activity.getType());

					}
				}
			}
		}
	}

	private void encounterOnePersonRandomlyFaceToFace(SocialAct event, HashMap<String, Double> rndEncounterProbability,
			Person p1, int iteration) {

		// Person p1 randomly encounters a person p2 at the socializing opportunity
		// the probability of encountering depends on the activity type

		if(Gbl.random.nextDouble() <rndEncounterProbability.get(event.activity.getType())){
			Person p2 = event.getRandomAttendee(p1);

			// If they know each other, probability is 1.0 that the relationship is reinforced
			if (p1.getKnowledge().getEgoNet().knows(p2)) {
				net.makeSocialContact(p1,p2,iteration,"renew"+event.activity.getType());
				System.out.println("Person "+p1.getId()+" renews with Person "+ p2.getId());
			} else {
				// If the two do not already know each other,

				if(Gbl.random.nextDouble() < pBecomeFriends){
					net.makeSocialContact(p1,p2,iteration,"new"+event.activity.getType());
					System.out.println("Person "+p1.getId()+" and Person "+ p2.getId()+" meet at "+event.activity.getFacility().getId()+" for activity "+event.activity.getType());
				}
			}
		}
	}
	private void encounterOnePersonRandomlyFaceToFaceInTimeWindow(SocialAct event, HashMap<String, Double> rndEncounterProbability,
			Person p1, double StartTime, double EndTime, int iteration) {

		// Person p1 randomly encounters a person p2 at the socializing opportunity
		// the probability of encountering depends on the activity type

		if(Gbl.random.nextDouble() <rndEncounterProbability.get(event.activity.getType())){
			Person p2 = event.getRandomAttendeeInTimeWindow(p1, StartTime, EndTime);
			if(p2==null) return;
			// If they know each other, probability is 1.0 that the relationship is reinforced
			if (p1.getKnowledge().getEgoNet().knows(p2)) {
				net.makeSocialContact(p1,p2,iteration,"renew"+event.activity.getType());

			} else {
				// If the two do not already know each other,

				if(Gbl.random.nextDouble() < pBecomeFriends){
					net.makeSocialContact(p1,p2,iteration,"new"+event.activity.getType());
				}
			}
		}
	}
	private void encounterAllPersonsFaceToFaceInTimeWindow(SocialAct event, HashMap<String, Double> rndEncounterProbability,
			Person p1, double StartTime, double EndTime, int iteration) {

		// Person p1 encounters all persons p2 at the socializing opportunity
		// the probability of encountering depends on the activity type

		if(Gbl.random.nextDouble() <rndEncounterProbability.get(event.activity.getType())){

			Vector<Person> persons = event.getAttendeesInTimeWindow(p1, StartTime, EndTime);
			if(persons!=null){
				int size = persons.size();
				for(int i=0; i<size;i++){
					Person p2=persons.get(i);
					if(p1.getKnowledge().getEgoNet().knows(p2)){
					} else {
						// If the two do not already know each other,

						if(Gbl.random.nextDouble() < pBecomeFriends){
							net.makeSocialContact(p1,p2,iteration,"new"+event.activity.getType());
						}
					}
				}
			}
		}
	}
}
