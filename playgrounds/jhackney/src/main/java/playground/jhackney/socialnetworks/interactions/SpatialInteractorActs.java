/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialInteractorActsFast.java
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

package playground.jhackney.socialnetworks.interactions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationImpl;

import playground.jhackney.socialnetworks.algorithms.CompareActs;
import playground.jhackney.socialnetworks.socialnet.EgoNet;
import playground.jhackney.socialnetworks.socialnet.SocialNetwork;


public class SpatialInteractorActs {

	SocialNetwork net;

	double pBecomeFriends = Double.parseDouble(Gbl.getConfig().socnetmodule().getPBefriend());// [0.0,1.0]

//	double pct_interacting = Double.parseDouble(Gbl.getConfig().socnetmodule().getFractSInteract());// [0.0,1.0]

	String interaction_type = Gbl.getConfig().socnetmodule().getSocNetInteractor2();

	LinkedHashMap<ActivityOptionImpl,ArrayList<Person>> activityMap;
//	LinkedHashMap<Activity,ArrayList<Person>> actMap=new LinkedHashMap<Activity,ArrayList<Person>>();
	ActivityFacilities facilities;

	public SpatialInteractorActs(SocialNetwork snet, ActivityFacilities facilities) {
		this.net = snet;
		this.facilities = facilities;
	}
	/**
	 * The Plans contain all agents who are chosen to participate in social interactions.
	 * Generally all agents in the population participate, each replanning_iter number of iterations.
	 * <br><br>
	 * <code>interact</code> looks through each selected Plan and Act. For each Act it compares the
	 * Facility Id to that of all other Acts in the other Plans. If the Facility Ids are the same between
	 * the two Acts, then the two Persons passed through the same Facility at some point.
	 * The first Act and all the Persons having visited that Facility Id are recorded in activityMap
	 * The Plan/Person must be recorded rather than the Act because there is no pointer from Act to Plan/Person.
	 * Also, the Facility Id is recorded separately in an ArrayList.
	 * 
	 * To use this information, iterate through the ArrayList of Facilities and the table of Acts to
	 * find the agents who passed through the Facility. Call up the list of agents and process their
	 * Plans according to the spatial interaction desired.
	 * 
	 * Note that one list records Facilities and the other records Acts. There could be several Acts
	 * matching a given Facility, thus if it is desired to ascertain all spatial encounters at a
	 * Facilit, it will be necessary to test all Acts. 
	 * 
	 * @param plans
	 * @param rndEncounterProb
	 * @param iteration
	 */
	public void interact(PopulationImpl plans, LinkedHashMap<String, Double> rndEncounterProb, int iteration) {

		System.out.println(" "+ this.getClass()+" Looking through plans and tracking which Persons could interact "+iteration);

			activityMap = new LinkedHashMap<ActivityOptionImpl,ArrayList<Person>>(); 
			activityMap= makeActivityMap(plans);

		// Activity-(facility)-based interactions

		if (interaction_type.equals("random")) {
			encounterOnePersonRandomlyPerActivity(rndEncounterProb, iteration);
		}else if (interaction_type.equals("meetall")){
			makeSocialLinkToAll(rndEncounterProb,iteration);

//			Act-based interactions

		}else if(interaction_type.equals("timewindowrandom")){
			encounterOnePersonRandomlyFaceToFaceInTimeWindow(rndEncounterProb,iteration);
		}else if(interaction_type.equals("timewindowall")){
			encounterAllPersonsFaceToFaceInTimeWindow(rndEncounterProb,iteration);
		}else if(interaction_type.equals("MarchalNagelChain")){
			Gbl.errorMsg("Spatial interactions "+interaction_type+" Not supported in new Act search algorithm "+ this.getClass());
//			makeSocialLinkBetweenLastTwo(rndEncounterProb,iteration);
		} else {
			Gbl.errorMsg(" Spatial interaction type is \"" + interaction_type
					+ "\". Only \"random\", \"meetall\", \"timewindowrandom\", and \"timewindowall\" are supported at this time.");
		}
		System.out.println("...finished");
	}
	/**
	 * Makes a list of Activity (== Facility + activity type) and the Persons (Plans --> Acts) carrying
	 * out an Act there. Only Acts need be stored, but since there is no way to link to a Person from
	 * an Act, we store the Plan.
	 * 
	 * Note that a single Person may have multiple Acts which take place at a Facility (e.g. Morning Home,
	 * Evening Home). Thus storing the Person is a way to store all of these Acts.
	 * 
	 *  We search through the Acts at a later stage when using this Map.
	 *  
	 * @param plans
	 * @return
	 */
	private LinkedHashMap<ActivityOptionImpl,ArrayList<Person>> makeActivityMap(Population plans){
		System.out.println("Making a new activity map for spatial interactions");
		LinkedHashMap<ActivityOptionImpl,ArrayList<Person>> activityMap=new LinkedHashMap<ActivityOptionImpl,ArrayList<Person>>();
		for (Person p1 : plans.getPersons().values()) {
			Plan plan1 = p1.getSelectedPlan();
			for (PlanElement pe : plan1.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act1 = (ActivityImpl) pe;
					ActivityOptionImpl activity1=((ActivityFacilityImpl) this.facilities.getFacilities().get(act1.getFacilityId())).getActivityOptions().get(act1.getType());
					ArrayList<Person> actList=new ArrayList<Person>();
					
					if(!activityMap.keySet().contains(activity1)){
						activityMap.put(activity1,actList);	
					}
					if(activityMap.keySet().contains(activity1)){
						ArrayList<Person> myList=activityMap.get(activity1);
						myList.add(p1);
					}	
				}
			}
		}
		return activityMap;
	}
	/**
	 * Based on Marchal and Nagel 2007, TRR 1935
	 * Person p1 meets and befriends the person who arrived just before him, if
	 * this person is still at the SocialAct.
	 * 
	 * Cycle through all the agents who were co-present with p1 at the SocialAct
	 * and find the agent whose arrival time is closest to and less than that of p1
	 * Subject to the likelihood of a meeting taking place in a given facility type
	 */
	private void makeSocialLinkBetweenLastTwo(LinkedHashMap<String, Double> rndEncounterProbability, int iteration) {


//		Person nextInQueue=null;
//		if(Gbl.random.nextDouble() <rndEncounterProbability.get(act1.activity.getType())){

//		Vector<Person> persons = event.getAttendeesInTimeWindow(p1,startTime,endTime);
//		int size = persons.size();
//		nextInQueue = event.getAttendeeInTimeWindow(p1,startTime,endTime,0);
//		for(int i=0; i<size;i++){
//		Person p2 = event.getAttendeeInTimeWindow(p1, startTime, endTime, i);
//		if(event.getArrivalTime(p2)<=event.getArrivalTime(p1)){
//		if(event.getArrivalTime(p2)<=event.getArrivalTime(nextInQueue)){
//		nextInQueue=p2;
//		}
//		}
//		}
//		if(p1.getKnowledge().getEgoNet().knows(nextInQueue)){
//		} else {
//		// If the two do not already know each other,
//		net.makeSocialContact(p1,nextInQueue,iteration,"new"+event.activity.getType());
//		}
//		}
	}
	/**
	 * Time-independent chance of spatial encounter during each activity:
	 * 
	 * This is not actually a "spatial" interaction but a test of shared
	 * knowledge and could be accomplished via a search of Knowledge.Activities().
	 * 
	 * Each person visiting a Facility to perform an Activity has a chance
	 * to meet every other person who was at that Facility doing the same thing.
	 * <br><br>
	 * This models the chance that two people who do the same thing at the same
	 * place, but who may not have been present in the same time window because
	 * of bad luck or bad planning, might still know each other.
	 * <br><br>
	 * For every two people, person1 and person2, who visited the same facility
	 * and performed the same activity there, regardless of when, there is a probability
	 * that they know each other.
	 * <br><br>
	 * This probability depends only on the activity type, "rndEncounterProbability(activity type)",
	 * and not on any additional "friendliness" parameter.
	 * <br><br>
	 * The conditions of adding network links apply:
	 * {@link org.matsim.socialnetworks.socialnet.SocialNetwork.makeSocialContact}
	 * 
	 * @param plans
	 * @param rndEncounterProbability
	 * @param iteration
	 */
	private void makeSocialLinkToAll(LinkedHashMap<String, Double> rndEncounterProbability, int iteration) {
//		for (Enumeration<Activity> myActivities=activityMap.keys(); myActivities.hasMoreElements() ;) {
//			Activity myActivity=myActivities.nextElement();
			Set<ActivityOptionImpl> myActivities=activityMap.keySet();
			Iterator<ActivityOptionImpl> ait= myActivities.iterator();
			while(ait.hasNext()) {
				ActivityOptionImpl myActivity=ait.next();
			ArrayList<Person> visitors=activityMap.get(myActivity);
			// Go through the list of Persons and for each one pick one friend randomly
			// Must be double loop
			Iterator<Person> vIt1=visitors.iterator();
			while(vIt1.hasNext()){
				Person p1= vIt1.next();
				Iterator<Person> vIt2=visitors.iterator();
				while(vIt2.hasNext()){
					Person p2= vIt2.next();

					if(MatsimRandom.getRandom().nextDouble() <rndEncounterProbability.get(myActivity.getType())){

						// If they know each other, probability is 1.0 that the relationship is reinforced
						if (((EgoNet)p1.getCustomAttributes().get(EgoNet.NAME)).knows(p2)) {
							net.makeSocialContact(p1,p2,iteration,"renew_"+myActivity.getType());
//							System.out.println("Person "+p1.getId()+" renews with Person "+ p2.getId());
						} else {
							// If the two do not already know each other,
							net.makeSocialContact(p1,p2,iteration,"new_"+myActivity.getType());
//							System.out.println("Person "+p1.getId()+" and Person "+ p2.getId()+" meet at "+myActivity.getFacility().getId()+" for activity "+myActivity.getType());
						}
					}
				}
			}
		}
	}

	/**
	 * Time-independent chance of spatial encounter at each activity.
	 * 
	 * This is not actually a "spatial" interaction but a test of shared
	 * knowledge and could be accomplished via a search of Knowledge.Activities().
	 * 
	 * Each person visiting a Facility to perform an Activity has a chance
	 * to meet ONE other person who was at that Facility doing the same thing.
	 * <br><br>
	 * If person1 and person2 visited the same facility and performed the same
	 * activity there, regardless of when, then there is a probability
	 * "rndEncounterProbability(activity type)" that they encounter one another.
	 * If they know each other, their friendship is reinforced.
	 * <br><br>
	 * If they do not, they befriend with probability <code>pBecomeFriends</code>.
	 * 
	 * The conditions of "becoming friends" apply:
	 * {@link org.matsim.socialnetworks.socialnet.SocialNetwork.makeSocialContact}
	 * 
	 * 
	 * @param p1
	 * @param p2
	 * @param rndEncounterProbability
	 * @param iteration
	 */
	private void encounterOnePersonRandomlyPerActivity(LinkedHashMap<String, Double> rndEncounterProbability, int iteration) {

//		Enumeration<Activity> myActivities=pList.keys();

//		for (Enumeration<Activity> myActivities=activityMap.keys(); myActivities.hasMoreElements() ;) {
//			Activity myActivity=myActivities.nextElement();
			Set<ActivityOptionImpl> myActivities=activityMap.keySet();
			Iterator<ActivityOptionImpl> ait= myActivities.iterator();
			while(ait.hasNext()) {
				ActivityOptionImpl myActivity=ait.next();
			ArrayList<Person> visitors=activityMap.get(myActivity);
			
			// Go through the list of Persons and for each one pick one friend randomly
			// Must be double loop
			Iterator<Person> vIt1=visitors.iterator();
			while(vIt1.hasNext()){
				Person p1= vIt1.next();

				Person p2 = visitors.get(MatsimRandom.getRandom().nextInt(visitors.size()));
				if(MatsimRandom.getRandom().nextDouble() <rndEncounterProbability.get(myActivity.getType())){

					// If they know each other, probability is 1.0 that the relationship is reinforced
					if (((EgoNet)p1.getCustomAttributes().get(EgoNet.NAME)).knows(p2)) {
						net.makeSocialContact(p1,p2,iteration,"renew_"+myActivity.getType());
//						System.out.println("Person "+p1.getId()+" renews with Person "+ p2.getId());
					} else {
						// If the two do not already know each other,

						if(MatsimRandom.getRandom().nextDouble() < pBecomeFriends){
							net.makeSocialContact(p1,p2,iteration,"new_"+myActivity.getType());
//							System.out.println("Person "+p1.getId()+" and Person "+ p2.getId()+" meet at "+myActivity.getFacility().getId()+" for activity "+myActivity.getType());
						}
					}
				}
			}
		}
	}

	/**
	 * This interaction is based on Acts and as such counts things like the Morning Home and
	 * Evening Home activity as separate social phenomena. To avoid this, use the interactions
	 * based on Activity.
	 * 
	 * Each agent may randomly encounter (and have the chance to befriend) ONE other agent during
	 * an Act in which they are both present. Uses a time window. The duration of the time
	 * window is not relevant in this method.
	 * 
	 * @param plans
	 * @param rndEncounterProbability
	 * @param iteration
	 */
	private void encounterOnePersonRandomlyFaceToFaceInTimeWindow(LinkedHashMap<String, Double> rndEncounterProbability, int iteration) {

		//		LinkedHashMap<Person,Act> personList=new LinkedHashMap<Person,Act>();
		//		LinkedHashMap<Person,ArrayList<Person>> othersList = new LinkedHashMap<Person,ArrayList<Person>>();

		// First identify the overlapping Acts and the Persons involved

		//		for (Enumeration<Activity> myActivities=activityMap.keys(); myActivities.hasMoreElements() ;) {
		//			Activity myActivity=myActivities.nextElement();
		Set<ActivityOptionImpl> myActivities=activityMap.keySet();
		for (ActivityOptionImpl myActivity : myActivities) {
			ArrayList<Person> visitors=activityMap.get(myActivity);
			for (Person p1 : visitors) {
				Plan plan1=p1.getSelectedPlan();
				ArrayList<Person> others = new ArrayList<Person>();
				//				othersList.put(p1,others);
				for (PlanElement pe1 : plan1.getPlanElements()) {
					if (pe1 instanceof ActivityImpl) {
						ActivityImpl act1 = (ActivityImpl) pe1;
						//					personList.put(p1,act1);
						for (Person p2 : visitors) {
							Plan plan2=p2.getSelectedPlan();
							for (PlanElement pe2 : plan2.getPlanElements()) {
								if (pe2 instanceof ActivityImpl)	{
									ActivityImpl act2 = (ActivityImpl) pe2;
									if(CompareActs.overlapTimePlaceType(act1,act2, this.facilities)&& !p1.equals(p2)){
										//agents encounter and may befriend
										others.add(p2);
									}
								}
							}
						}
					}
				}
				if(others.size()>0){
					Person p2=others.get(MatsimRandom.getRandom().nextInt(others.size()));
					if(MatsimRandom.getRandom().nextDouble() <rndEncounterProbability.get(myActivity.getType())){

						// If they know each other, probability is 1.0 that the relationship is reinforced
						if (((EgoNet)p1.getCustomAttributes().get(EgoNet.NAME)).knows(p2)) {
							net.makeSocialContact(p1,p2,iteration,"renew_"+myActivity.getType());
							//							System.out.println("Person "+p1.getId()+" renews with Person "+ p2.getId());
						} else {
							// If the two do not already know each other,

							if(MatsimRandom.getRandom().nextDouble() < pBecomeFriends){
								net.makeSocialContact(p1,p2,iteration,"new_"+myActivity.getType());
								//								System.out.println("Person "+p1.getId()+" and Person "+ p2.getId()+" meet at "+myActivity.getFacility().getId()+" for activity "+myActivity.getType());
							}
						}
					}
				}
			}
		}
//
//		// Using the two TreeMaps of Who did What and Who Else Did It, for each Person, randomly pick 
//		// ONE other Person at each act and make them have an encounter.
//		
//		Iterator<Person> pIt=personList.keySet().iterator();
//		
//		while(pIt.hasNext()){
//			Person p1=pIt.next();
//			if(othersList.get(p1).size()>0){
//			Person p2=othersList.get(p1).get(Gbl.random.nextInt(othersList.get(p1).size()));
//			Activity myActivity=personList.get(p1).getFacility().getActivity(personList.get(p1).getType());
//			if(Gbl.random.nextDouble() <rndEncounterProbability.get(myActivity.getType())){
//
//				// If they know each other, probability is 1.0 that the relationship is reinforced
//				if (p1.getKnowledge().getEgoNet().knows(p2)) {
//					net.makeSocialContact(p1,p2,iteration,"renew"+myActivity.getType());
////					System.out.println("Person "+p1.getId()+" renews with Person "+ p2.getId());
//				} else {
//					// If the two do not already know each other,
//
//					if(Gbl.random.nextDouble() < pBecomeFriends){
//						net.makeSocialContact(p1,p2,iteration,"new"+myActivity.getType());
////						System.out.println("Person "+p1.getId()+" and Person "+ p2.getId()+" meet at "+myActivity.getFacility().getId()+" for activity "+myActivity.getType());
//					}
//				}
//			}
//			}
//		}
	}


	/**
	 * For each Act of each SelectedPlan() of each Person, this method tests all other Acts
	 * at the Activity to see if they are in the same place at the same time (Act overlap). If so,
	 * the agents are linked in a social network with Activity-dependent probability,
	 * <code>rndEncounterProbability(activity type)</code>. If the agents were already linked,
	 * their link is reinforced.
	 * <br><br>
	 * 
	 * There is no other probability adjustment in this method.
	 * 
	 * @param plans
	 * @param rndEncounterProbability
	 * @param iteration
	 */
	private void encounterAllPersonsFaceToFaceInTimeWindow(LinkedHashMap<String, Double> rndEncounterProbability,
			int iteration) {

//		for (Enumeration<Activity> myActivities=activityMap.keys(); myActivities.hasMoreElements() ;) {
		Set<ActivityOptionImpl> myActivities=activityMap.keySet();
		Iterator<ActivityOptionImpl> ait= myActivities.iterator();
		while(ait.hasNext()) {
			ActivityOptionImpl myActivity=ait.next();
//			Activity myActivity=myActivities.nextElement();
			ArrayList<Person> visitors=activityMap.get(myActivity);
			Iterator<Person> vIt1=visitors.iterator();
			while(vIt1.hasNext()){
				Person p1= vIt1.next();
				for (PlanElement pe1 : p1.getSelectedPlan().getPlanElements()) {
					if (pe1 instanceof ActivityImpl) {
						ActivityImpl act1 = (ActivityImpl) pe1;
						for (Person p2 : visitors) {
							for (PlanElement pe2 : p2.getSelectedPlan().getPlanElements()) {
								if (pe2 instanceof ActivityImpl) {
									ActivityImpl act2 = (ActivityImpl) pe2;
									if(CompareActs.overlapTimePlaceType(act1,act2, this.facilities)&& !p1.equals(p2)){
										//agents encoutner and may befriend
										if(MatsimRandom.getRandom().nextDouble() <rndEncounterProbability.get(myActivity.getType())){
											
											// If they know each other, probability is 1.0 that the relationship is reinforced
											if (((EgoNet)p1.getCustomAttributes().get(EgoNet.NAME)).knows(p2)) {
												net.makeSocialContact(p1,p2,iteration,"renew_"+myActivity.getType());
//										System.out.println("Person "+p1.getId()+" renews with Person "+ p2.getId());
											} else {
												// If the two do not already know each other,
												
												net.makeSocialContact(p1,p2,iteration,"new_"+myActivity.getType());
//										System.out.println("Person "+p1.getId()+" and Person "+ p2.getId()+" meet at "+myActivity.getFacility().getId()+" for activity "+myActivity.getType());
												
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
