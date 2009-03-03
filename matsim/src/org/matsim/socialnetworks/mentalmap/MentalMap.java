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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.facilities.Facilities;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.ActivityOption;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.Knowledge;
import org.matsim.socialnetworks.algorithms.SortHashMapByValue;
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
//	private LinkedHashMap<Activity,Act> mapActivityAct = new LinkedHashMap<Activity,Act>();
//	private LinkedHashMap<Integer, Act> actIdAct = new LinkedHashMap<Integer, Act>();

//	Map of act and activity ID numbers. Reverse of above. Acts change so we use Id's
//	private LinkedHashMap<Act,Id> mapActActivityId = new LinkedHashMap<Act,Id>();


//	The activity score
	private LinkedHashMap<ActivityOption, Double> activityScore = new LinkedHashMap<ActivityOption, Double>();

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
			if(myAct.getFacility()==null){ // new Acts are assigned a facility in the Plans file
				ActivityOption myActivity = null;
				// If there is already knowledge in the initial plans file, use it
				if(this.knowledge.getActivities(myAct.getType()).size()>0){
					myActivity=this.knowledge.getActivities(myAct.getType()).get(MatsimRandom.random.nextInt(this.knowledge.getActivities(myAct.getType()).size()));
					myAct.setFacility(myActivity.getFacility());
					//TODO JH add logic to label this activity primary or secondary
					this.knowledge.addActivity(myActivity, false);
//					learnActsActivities(myAct,myActivity);
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
					int k = MatsimRandom.random.nextInt(facs.length);
					Facility f = (Facility) facs[k];
					myActivity = f.getActivity(myAct.getType());
					if(myActivity!=null){
						myAct.setFacility(myActivity.getFacility());
						//TODO JH add logic to label this activity primary or secondary
						this.knowledge.addActivity(myActivity,false);
					}
				}
			}
		}
	}

	public void initializeActActivityMapFromFile(Plan myPlan, Facilities facilities, ActivityActReader aar){


		if(aar==null) return;
//		this.log.info("READING ACT-ACTIVITY MAP FROM FILE PERSON "+myPlan.getPerson().getId());
		ActIterator planActIter = myPlan.getIteratorAct();
		while(planActIter.hasNext()){
			Act myAct = (Act) planActIter.next();

			TreeMap<Id,String> nextFac = aar.getNextPoint();
			Id myFacilityId = nextFac.firstKey();
//			String myActivityType=nextFac.get(myFacilityId);
			String myActivityType=myAct.getType();

			Facility fac = facilities.getFacilities().get(myFacilityId);
//			myAct.setFacility(fac);
//			this.knowledge.addActivity(fac.getActivity(myActivityType));
			//TODO JH apply some logic to label this a primary or secondary location
			ActivityOption myActivity=fac.getActivity(myActivityType);
			this.knowledge.addActivity(myActivity,false);
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

	private void forgetActivity (ActivityOption myactivity){

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
			this.log.info("No activities removed from memory");
			return;
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

			SortHashMapByValue shmbv = new SortHashMapByValue();
//			LinkedHashMap<Activity, ?> sortedScores = SortHashMapByValue.makeSortedMap(this.activityScore);
			LinkedHashMap<ActivityOption, ?> sortedScores = shmbv.makeSortedMap(this.activityScore);

			// Remove activities if there are too many, but keep one activity
			// for each act in the current plan. Iterator goes by score.

			int numToForget = this.knowledge.getActivities().size()-max-currentActivities.size();
			// Avoid concurrent modification errors
			// Add to the "forgetlist" the correct number of activities to forget
			// Since these activities are sorted by score, just take the first ones
			ArrayList<ActivityOption> forgetList=new ArrayList<ActivityOption>();
			int counter=0;

			// If there are too many activities 
//			for (Enumeration<Activity> e = sortedScores.keys() ; e.hasMoreElements() ;) {
//				Activity myactivity=e.nextElement();
				Set<ActivityOption> myActivities=sortedScores.keySet();
				Iterator<ActivityOption> ait= myActivities.iterator();
				while(ait.hasNext()) {
					ActivityOption myactivity=(ActivityOption) ait.next();
//				double score=(Double) sortedScores.get(myactivity);
				// note which activity to forget

				if(counter<=numToForget && !currentActivities.contains(myactivity)){
					forgetList.add(myactivity);
					counter++;
				}
			}

			Iterator<ActivityOption> forget=forgetList.iterator();
			while(forget.hasNext()){
				ActivityOption myactivity=forget.next();
				forgetActivity(myactivity);
			}
		}
//		System.out.println(this.getClass()+" NumActivities2 "+this.knowledge.getActivities().size()+" "+this.activityScore.values().size());
	}

	public void addActivity(ActivityOption myActivity){
		// Adds unmapped activity to mental map without associating it with an act
		//TODO JH add logic to label this activity primary or secondary
		this.knowledge.addActivity(myActivity,false);
		setActivityScore(myActivity);
	}

	public void setActivityScore(ActivityOption myActivity){
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

	public Act getActFromActivity (Person person, ActivityOption myActivity){
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

}


