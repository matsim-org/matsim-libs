package org.matsim.socialnetworks.scoring;

/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialScorer.java
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.ActivityOption;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.socialnetworks.algorithms.CompareActs;
import org.matsim.socialnetworks.socialnet.EgoNet;
import org.matsim.socialnetworks.socialnet.SocialNetwork;


public class TrackActsOverlap {

	SocialNetwork net;

	LinkedHashMap<ActivityOption,ArrayList<Person>> activityMap;
	static final private Logger log = Logger.getLogger(TrackActsOverlap.class);

	/**
	 * The Plans contain all agents who are chosen to get scores for social interactions.
	 * Generally all agents in the population get scored.
	 * <br><br>
	 * <code>summarizeActs</code> looks through each selected Plan and Act. For each Act it compares the
	 * Facility Id to that of all other Acts in the other Plans. If the Facility Ids are the same between
	 * the two Acts, then the two Persons passed through the same Facility at some point.
	 * The first Act and all the Persons having visited that Facility Id are recorded in activityMap.
	 * The Plan/Person must be recorded rather than the Act because there is no pointer from Act to Plan/Person.
	 * Also, the Facility Id is recorded separately in an ArrayList.
	 * 
	 * To use this information, iterate through the ArrayList of Facilities and the table of Acts to
	 * find the agents who passed through the Facility. Call up the list of agents and process their
	 * Plans according to the spatial interaction desired.
	 * 
	 * Note that there could be several Acts
	 * matching a given Facility, thus if it is desired to ascertain all spatial encounters at a
	 * Facility, it will be necessary to test all Acts. 
	 * 
	 * @param plans
	 * @param iteration
	 * 
	 */
	public void trackActs(Population plans, int iteration) {

		log.info(" Looking through plans and mapping social interactions for scoring "+iteration);

		activityMap = new LinkedHashMap<ActivityOption,ArrayList<Person>>(); 
		activityMap= makeActivityMap(plans);

		log.info("...finished");

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
	 * @return activityMap
	 */
	private LinkedHashMap<ActivityOption,ArrayList<Person>> makeActivityMap(Population plans){
		log.info("Making a new activity map for spatial scores");
//		LinkedHashMap<Activity,ArrayList<Person>> activityMap=new LinkedHashMap<Activity,ArrayList<Person>>();
		Iterator<Person> p1Iter=plans.iterator();
		while(p1Iter.hasNext()){
			Plan plan1= ((Person) p1Iter.next()).getSelectedPlan();
			Person p1=plan1.getPerson();
			ActIterator a1Iter =plan1.getIteratorAct();
			while(a1Iter.hasNext()){
				Act act1 = (Act) a1Iter.next();
				ActivityOption activity1=act1.getFacility().getActivity(act1.getType());
				ArrayList<Person> actList=new ArrayList<Person>();

				if(!activityMap.keySet().contains(activity1)){
					actList.add(p1);
					activityMap.put(activity1,actList);	
				}
				if(activityMap.keySet().contains(activity1)){
					ArrayList<Person> myList=activityMap.get(activity1);
					myList.add(p1);
				}	
			}
		}
		return activityMap;
	}

	/**
	 * Time-independent spatial collocation:
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
	 * and performed the same activity there, regardless of when, there is a score
	 * <br><br>
	 * 
	 * @param plans
	 * @param iteration
	 * @return score
	 */
	public double scoreAllFriendsInAct(Plan plan) {
		double score = 0;
		ActIterator ait=plan.getIteratorAct();
		while(ait.hasNext()){
			Act act = (Act) ait.next();
			ActivityOption myActivity=act.getFacility().getActivity(act.getType());
			ArrayList<Person> visitors=activityMap.get(myActivity);
			// Go through the list of Persons and for each one pick one friend randomly
			// Must be double loop
			Iterator<Person> vIt=visitors.iterator();
			while(vIt.hasNext()){
				Person p2= vIt.next();
//				TODO do something to score JH
				// Person p1=plan.getPerson(); EgoNet net = p1.getMap().getEgoNet(); if(net.getAlters().contains(p2){friend++}else{foe++}; etc.

			}
		}
		return score;
	}

	/**
	 * For the Act, this method tests all other Acts
	 * at the Activity to see if they are in the same place at the same time (Act overlap). If so,
	 * the agent is scored accordingly.
	 * <br><br>
	 * 
	 * 
	 * @param plans
	 * @param rndEncounterProbability
	 * @param iteration
	 */
	public double calculateFriendtoFoeInTimeWindow(Plan plan) {

		double friend=0.;
		double foe=0.;
		ActIterator ait=plan.getIteratorAct();
		Person p1=plan.getPerson();
		while(ait.hasNext()){
			Act act1 = (Act) ait.next();
			ActivityOption myActivity=act1.getFacility().getActivity(act1.getType());
			ArrayList<Person> visitors=activityMap.get(myActivity);
			if(!activityMap.keySet().contains(myActivity)){
				Gbl.errorMsg(this.getClass()+" activityMap does not contain myActivity");
			}
			if(!(visitors.size()>0)){
				Gbl.errorMsg(this.getClass()+" number of visitors not >0");
			}
			// Go through the list of Persons and for each one pick one friend randomly
			Iterator<Person> vIt=visitors.iterator();
			while(vIt.hasNext()){
				Person p2= vIt.next();
				Plan plan2=p2.getSelectedPlan();
				ActIterator act2It=plan2.getIteratorAct();
				while(act2It.hasNext()){
					Act act2 = (Act) act2It.next();
					if(CompareActs.overlapTimePlaceType(act1,act2)&& !p1.equals(p2)){
						EgoNet net = p1.getKnowledge().getEgoNet();
						if(net.getAlters().contains(p2)){
							friend++;
						}else{
							foe++;
						}
					}
				}
			}
		}
		if((friend+foe)==0){
			return 0;
		}else
			return friend/(foe+.1*(friend+foe));
	}
	/**
	 * Calculates a set of statistics about the face to face interactions
	 * at a social event
	 * @param plan
	 * @return
	 */
	public ArrayList<Double> calculateTimeWindowStats(Plan plan) {

		ArrayList<Double> stats=new ArrayList<Double>();
		// stats(0)=friendFoeRatio
		// stats(1)=nFriends
		// stats(2)=totalTimeWithFriends
		double friend=0.;
		double foe=0.;
		double totalTimeWithFriends=0;

		ActIterator ait=plan.getIteratorAct();
		Person p1=plan.getPerson();
		while(ait.hasNext()){
			Act act1 = (Act) ait.next();
			ActivityOption myActivity=act1.getFacility().getActivity(act1.getType());
			ArrayList<Person> visitors=activityMap.get(myActivity);
			if(!activityMap.keySet().contains(myActivity)){
				Gbl.errorMsg(this.getClass()+" activityMap does not contain myActivity");
			}
			if(!(visitors.size()>0)){
				Gbl.errorMsg(this.getClass()+" number of visitors not >0");
			}
			// Go through the list of Persons
			Iterator<Person> vIt=visitors.iterator();
			while(vIt.hasNext()){
				Person p2= vIt.next();
				Plan plan2=p2.getSelectedPlan();
				ActIterator act2It=plan2.getIteratorAct();
				while(act2It.hasNext()){
					Act act2 = (Act) act2It.next();
					if(CompareActs.overlapTimePlaceType(act1,act2)&& !p1.equals(p2)){
						EgoNet net = p1.getKnowledge().getEgoNet();
						if(net.getAlters().contains(p2)){
							friend++;
							totalTimeWithFriends+=getTimeWindowDuration(act1,act2);
						}else{
							foe++;
						}
					}
				}
			}
		}
		if((friend+foe)==0){
			stats.add((double) 0);
		}else{
			stats.add(friend/(foe+.1*(friend+foe)));
		}
		stats.add(friend);
		stats.add(totalTimeWithFriends);

		return stats;
	}
	
	/**
	 * Calculates a set of statistics about the face to face interactions
	 * at a social event
	 * @param plan
	 * @return
	 */
	public LinkedHashMap<Act,ArrayList<Double>> calculateTimeWindowActStats(Plan plan) {

		LinkedHashMap<Act,ArrayList<Double>> actStats = new LinkedHashMap<Act,ArrayList<Double>>();
		// stats(0)=friendFoeRatio
		// stats(1)=nFriends
		// stats(2)=totalTimeWithFriends
		ActIterator ait=plan.getIteratorAct();
		Person p1=plan.getPerson();
		while(ait.hasNext()){
			double friend=0.;
			double foe=0.;
			double totalTimeWithFriends=0;
			
			Act act1 = (Act) ait.next();
			ActivityOption myActivity=act1.getFacility().getActivity(act1.getType());
			ArrayList<Person> visitors=activityMap.get(myActivity);
			if(!activityMap.keySet().contains(myActivity)){
				Gbl.errorMsg(this.getClass()+" activityMap does not contain myActivity");
			}
			if(!(visitors.size()>0)){
				Gbl.errorMsg(this.getClass()+" number of visitors not >0");
			}
			// Go through the list of Persons
			Iterator<Person> vIt=visitors.iterator();
			while(vIt.hasNext()){
				Person p2= vIt.next();
				Plan plan2=p2.getSelectedPlan();
				ActIterator act2It=plan2.getIteratorAct();
				while(act2It.hasNext()){
					Act act2 = (Act) act2It.next();
					if(CompareActs.overlapTimePlaceType(act1,act2)&& !p1.equals(p2)){
						EgoNet net = p1.getKnowledge().getEgoNet();
						if(net.getAlters().contains(p2)){
							friend++;
							totalTimeWithFriends+=getTimeWindowDuration(act1,act2);
						}else{
							foe++;
						}
					}
				}
			}
			if(!actStats.keySet().contains(act1)){
				ArrayList<Double> stats=new ArrayList<Double>();
				if((friend+foe)==0){
					stats.add((double) 0);
				}else{
					stats.add(friend/(foe+.1*(friend+foe)));
				}
				stats.add(friend);
				stats.add(totalTimeWithFriends);

				actStats.put(act1,stats);	
			}
			if(actStats.keySet().contains(act1)){
				ArrayList<Double> stats=actStats.get(act1);
				if((friend+foe)==0){
					stats.add((double) 0);
				}else{
					stats.add(friend/(foe+.1*(friend+foe)));
				}
				stats.add(friend);
				stats.add(totalTimeWithFriends);
			}	
		}

		return actStats;
	}	
	
	private double getTimeWindowDuration(Act act1, Act act2) {
		double duration=0.;
		duration = Math.min(act1.getEndTime(),act2.getEndTime())-Math.max(act1.getStartTime(),act2.getStartTime());
		return duration;
	}
}

