/* *********************************************************************** *
 * project: org.matsim.*
 * MentalMap2.java
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

package org.matsim.socialnetworks.mentalmap;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Knowledge;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.socialnetworks.algorithms.SortHashtableByValue;
import org.matsim.socialnetworks.interactions.SocialAct;
import org.matsim.socialnetworks.io.ActivityActReader;
import org.matsim.world.Location;

/**
 *
 * @author fmarchal, jhackney
 *
 */
public class MentalMap {

	// Class to manage the knowledge.
	// Activities and Acts need to point to each other consistently
	//  across iterations of replanning. An agent should not forget in
	//  which facility he performed his Act. Indeed at the moment, which
	//  facility is chosen does not affect the mobility calculation or
	//  the standard MATSim utility, but it will be important for social
	//  networks.
	//
	// In a Plan, each Act takes place in one Facility.
	// One Facility can have multiple Acts, however, since Facilities can have several Activities.
	// The Act will change each iteration of the mobility simulation (start/end times).
	// Its mapping to the Activity is stored so it can be found again

//	Map of activities and acts: this is like a memory of having been someplace
//	private Hashtable<Activity,Act> mapActivityAct = new Hashtable<Activity,Act>();
//	private Hashtable<Integer, Act> actIdAct = new Hashtable<Integer, Act>();

//	Map of act and activity ID numbers. Reverse of above. Acts change so we use Id's
//	private Hashtable<Act,Id> mapActActivityId = new Hashtable<Act,Id>();

	// Socializing opportunities are face-to-face meetings of the agents
	private ArrayList<SocialAct> socializingOpportunities = new ArrayList<SocialAct>();

//	The activity score
	private Hashtable<Activity, Double> activityScore = new Hashtable<Activity, Double>();

//	Total maximum number of activities (locations + action) an agent can remember

	private Knowledge knowledge = null;
	private Logger log= Logger.getLogger(MentalMap.class);

	public MentalMap(Knowledge knowledge){
		this.knowledge=knowledge;
	}

	public void initializeActActivityMapRandom (Plan myPlan){

//		Associate each act in the plan with a random facility on the link

		ActIterator planActIter = myPlan.getIteratorAct();

		while(planActIter.hasNext()){
			Act myAct = (Act) planActIter.next();

			Activity myActivity = null;
			// If there is already knowledge in the initial plans file, use it
			if(this.knowledge.getActivities(myAct.getType()).size()>0){
				myActivity=this.knowledge.getActivities(myAct.getType()).get(Gbl.random.nextInt(this.knowledge.getActivities(myAct.getType()).size()));
				myAct.setFacility(myActivity.getFacility());
				this.knowledge.addActivity(myActivity);
//				learnActsActivities(myAct,myActivity);
			}

			// Else the activity is null and we choose an activity to assign to the act
			Link myLink = myAct.getLink();
			// These Locations are facilities by the new convention
			Collection<Location> locations = myLink.getUpMapping().values();
			// These Objects are facilities by convention
			Object[] facs =  locations.toArray();
			// Assign a random activity (a facility) on the link to the act
			// thus giving it in effect a street address
			while(myActivity==null){
				int k = Gbl.random.nextInt(facs.length);
				Facility f = (Facility) facs[k];
				myActivity = f.getActivity(myAct.getType());
				if(myActivity!=null){
					myAct.setFacility(myActivity.getFacility());
					this.knowledge.addActivity(myActivity);
//					learnActsActivities(myAct,myActivity);
				}
			}
//			System.out.println("## DEBUG MentalMap2 "+myPlan.getPerson().getId()+" "+myActivity.toString()+" "+myAct.toString());
		}
	}

	public void initializeActActivityMapFromFile(Plan myPlan, ActivityActReader aar){


		if(aar==null) return;
//		this.log.info("READING ACT-ACTIVITY MAP FROM FILE PERSON "+myPlan.getPerson().getId());
		ActIterator planActIter = myPlan.getIteratorAct();
		while(planActIter.hasNext()){
			Act myAct = (Act) planActIter.next();
			Id myActivityId = aar.getNextActivityId();
			Activity myActivity = knowledge.getActivity(myActivityId);
			if(myActivity!=null){
				myAct.setFacility(myActivity.getFacility());
				this.knowledge.addActivity(myActivity);
//				learnActsActivities(myAct,myActivity);
			}	
		}

	}

	public void prepareActs(Plan myPlan){

//		Tidy the acts up so they correspond to the expectations of the social net module.
//		First, change the types to be the same as the facility types.
//		Should not have to be done: adapt SocialNetworks code to use only the first letter
//		or else a standard MATSim "type" when this becomes established

		ActIterator planActIter = myPlan.getIteratorAct();

		while(planActIter.hasNext()){
			Act myAct = (Act) planActIter.next();

			String type="none";
			char typechar=myAct.getType().charAt(0);
			if(typechar=='h'){
				type="home";
			}else if(typechar=='w'){
				type="work";
			}else if(typechar=='l'){
				type="leisure";
			}else if(typechar=='s'){
				type="shop";
			}else if(typechar=='e'){
				type="education";
			}else
				Gbl.errorMsg("Activity type "+ typechar +" not known");
			myAct.setType(type);
		}
	}

	private void forgetActivity (Activity myactivity){

		if(this.activityScore.containsKey(myactivity)){
			this.activityScore.remove(myactivity);
		}
		//TODO JH 6.2008 if you remove an activity from Knowledge you must reset the facility of ALL
		// acts pointing to it, not just the one passed in, here. This is not checked, here. Make sure
		// you have checked your mapping before removing the Activity or you will get null pointers
		// from Act --> Facility.
		this.knowledge.removeActivity(myactivity);
	}
/**
 * ManageMemory is an algorithm that could be written to serve many purposes. Here, it tags
 * activities in excess of what is needed by an agent and deletes them from the agent's knowledge.
 *  
 * @param max
 * @param myPlans
 */
	public void manageMemory(int max, List<Plan> myPlans){

		if(myPlans.get(0).getActsLegs().size()*myPlans.size()/2 >max){
			Gbl.errorMsg(this.getClass()+
					" Number of activites an agent needs to remember for his plans is greater than his memory! MAX = "+max+" "+myPlans.get(0).getActsLegs().size()*myPlans.size()/2+" "+this.knowledge.getActivities().size());
		}

		if(this.knowledge.getActivities().size()>max){
			// Mark the activities associated with all plans in memory
			// so that they won't be deleted

			ArrayList<Id> currentActivities = new ArrayList<Id>();
			Iterator<Plan> planIter = myPlans.iterator();
			while(planIter.hasNext()){
				Plan myPlan = (Plan) planIter.next();
				ActIterator actIter = myPlan.getIteratorAct();
				while( actIter.hasNext() ){
					Act act = (Act) actIter.next();
					currentActivities.add(act.getFacility().getId());
				}
			}

//			Sort the activities by score so that they can be managed

			Hashtable sortedScores = SortHashtableByValue.makeSortedMap(this.activityScore);

			// Remove activities if there are too many, but keep one activity
			// for each act in the current plan. Iterator goes by score.

			int numToForget = this.knowledge.getActivities().size()-max-currentActivities.size();
			// Avoid concurrent modification errors
			// Add to the "forgetlist" the correct number of activities to forget
			// Since these activities are sorted by score, just take the first ones
			ArrayList<Activity> forgetList=new ArrayList<Activity>();
			int counter=0;

			// If there are too many activities 
			for (Enumeration<Activity> e = sortedScores.keys() ; e.hasMoreElements() ;) {
				Activity myactivity=e.nextElement();
//				double score=(Double) sortedScores.get(myactivity);
				// note which activity to forget

				if(counter<=numToForget && !currentActivities.contains(myactivity)){
					forgetList.add(myactivity);
					counter++;
				}
			}

			Iterator<Activity> forget=forgetList.iterator();
			while(forget.hasNext()){
				Activity myactivity=forget.next();
				forgetActivity(myactivity);
			}
		}
//		System.out.println(this.getClass()+" NumActivities2 "+this.knowledge.getActivities().size()+" "+this.activityScore.values().size());
	}

	public void addActivity(Activity myActivity){
		// Adds unmapped activity to mental map without associating it with an act
		this.knowledge.addActivity(myActivity);
		setActivityScore(myActivity);
	}

	public void setActivityScore(Activity myActivity){
//		Initializes the score of a newly learned or discovered activity to 0
//		Adds one to the score of the activity each time it is accessed.
//		Supposed to simulate how frequently it is thought of by agent.

		double x=0;
		if(!(this.activityScore.containsKey(myActivity))){
			this.activityScore.put(myActivity,x);
		}else
			if(this.activityScore.containsKey(myActivity)){
				x=this.activityScore.get(myActivity)+1;
				this.activityScore.remove(myActivity);
				this.activityScore.put(myActivity,x);
			}
	}

	public Act getActFromActivity (Person person, Activity myActivity){
		Act myAct=null;
		Iterator<Plan> planIter = person.getPlans().iterator();
		while(planIter.hasNext()){
			Plan myPlan = (Plan) planIter.next();
			ActIterator actIter = myPlan.getIteratorAct();
			while( actIter.hasNext() ){
				Act act = (Act) actIter.next();
				if(act.getFacility()==myActivity.getFacility()){
					myAct=act;
				}
			}
		}
		return myAct;
	}

	public void addDate(SocialAct date){
		this.socializingOpportunities.add(date);
	}

	public void dropDate(SocialAct date){
		this.socializingOpportunities.remove(date);
	}

	public void clearDates(){
		this.socializingOpportunities.clear();
	}

}


