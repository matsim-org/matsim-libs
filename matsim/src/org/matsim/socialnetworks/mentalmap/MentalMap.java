/* *********************************************************************** *
 * project: org.matsim.*
 * MentalMap.java
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


import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.basic.v01.BasicPlan.ActIterator;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Knowledge;
import org.matsim.plans.Plan;
import org.matsim.socialnetworks.algorithms.SortTreeMapByValue;
import org.matsim.socialnetworks.interactions.SocializingOpportunity;
import org.matsim.utils.identifiers.IdI;
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
	// In a Plan, there is exactly one Activity for each Act
	// There is exactly one Act per Activity
	// The Act will change each iteration of the mobility simulation.
	// Its ID will be stored so that it can be found again.

//	Map of activities and acts: this is like a memory of having been someplace
	private Hashtable<Activity,Act> mapActivityAct = new Hashtable<Activity,Act>();
//	Map of act and activity ID numbers. Reverse of above. Acts change so we use Id's
	private Hashtable<Integer,IdI> mapActIdActivityId = new Hashtable<Integer,IdI>();
	// Socializing opportunities are face-to-face meetings of the agents
	private ArrayList<SocializingOpportunity> socializingOpportunities = new ArrayList<SocializingOpportunity>();
//	The activity score
	private Hashtable<Activity, Double> activityScore = new Hashtable<Activity, Double>();
//	Total maximum number of activities (locations + action) an agent can remember 
	private int max_act_memory = 50;
	private Knowledge knowledge = null;

	public MentalMap(Knowledge knowledge){
		this.knowledge=knowledge;
	}

	public void matchActsToActivities (Plan myPlan){
		// Associate each act in the plan with a random facility on the link
		ActIterator planActIter = myPlan.getIteratorAct();
		int actId = 0;
		while(planActIter.hasNext()){
			Act myAct = (Act) planActIter.next();

//			Tidy the acts up so they correspond to the expectations of the social net module
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

			if(myAct.getRefId()==Integer.MIN_VALUE){
				myAct.setRefId(actId);
				actId++;
			}

			Link myLink = myAct.getLink();
			Activity myActivity = null;
			// These Locations are facilities by the new convention
			Collection<Location> locations = myLink.getUpMapping().values();
			// These Objects are facilities by convention
			Object[] facs =  locations.toArray();

			// Assign a random activity (a facility) on the link to the act
			// thus giving it in effect a street address
			while(myActivity==null){
				int k = Gbl.random.nextInt(facs.length);
				Facility f = (Facility) facs[k];
				myActivity = f.getActivity(type);
				if(myActivity!=null){
					learnActsActivities(myAct,myActivity);
				}
			}
		}
	}

	public void learnActsActivities (Act myact, Activity myactivity){

//		System.out.println("Update mapActivityAct:       "+myactivity.getFacility().getId()+" on link "+myact.getLinkId()+" with Act "+myact.getType());
		mapActivityAct.put(myactivity,myact);

//		System.out.println("Update mapActActivity:       "+myact.getType()+" with Activity key: "+myactivity.getFacility().getId()+" on link "+myact.getLinkId());
		mapActIdActivityId.put(myact.getRefId(),myactivity.getFacility().getId());

		setActivityScore(myactivity);

		knowledge.addActivity(myactivity);
	}

	public void forgetActsActivities (Act myact, Activity myactivity){
		if (mapActivityAct.containsKey(myactivity)){
//			System.out.println("Update Act:       "+myact);
			mapActivityAct.remove(myactivity);
		}

		if( mapActIdActivityId.containsKey(myact.getRefId())){

//			System.out.println("Update Activity:  "+myactivity);
//			mapActActivity.remove(myact);
			mapActIdActivityId.remove(myact.getRefId());
		}
		if(activityScore.containsKey(myactivity)){
			activityScore.remove(myactivity);
		}
		knowledge.removeActivity(myactivity);
	}

	public void manageMemory(int max, Plan myPlan){

		if(knowledge.getActivities().size()>max){
			// Mark the activities associated with the current plan
			// so that they won't be deleted
			ArrayList<Act> currentActs = new ArrayList<Act>();
			ActIterator actIter = myPlan.getIteratorAct();
			while( actIter.hasNext() ){

				Act act = (Act) actIter.next();
				if(mapActIdActivityId.containsKey(act.getRefId())){
					currentActs.add(act);
				}
			}
			// Sort the activities by score
			
//			TreeSet<Double> scores = SortTreeMapByValue.sort(activityScore);
			// Remove activities if there are too many, but keep one activity
			// for each act type in the current plan. Iterator goes by score.
			Iterator<Double> scoreIter = activityScore.values().iterator();
			int counter = 0;
			while(scoreIter.hasNext()){
				System.out.println("hi");
			}
		}
	}

	public void addActivity(Activity myActivity){
		// Adds unmapped activity to mental map without associating it with an act
		knowledge.addActivity(myActivity);
		setActivityScore(myActivity);
	}

	public void setActivityScore(Activity myActivity){
//		Initializes the score of a newly learned or discovered activity to 0
//		Adds one to the score of the activity each time it is accessed.
//		Supposed to simulate how frequently it is thought of by agent.

		double x=0;
		if(!(activityScore.containsKey(myActivity))){
			activityScore.put(myActivity,x);
		}else
			if(activityScore.containsKey(myActivity)){
				x=activityScore.get(myActivity)+1;	
			}

	}

	public Act getAct (Activity myActivity){
		return this.mapActivityAct.get(myActivity);
	}
	public Activity getActivity (Act myAct){

		IdI myActivityId= this.mapActIdActivityId.get(myAct.getRefId());
		TreeMap<IdI,Facility> facilities=this.knowledge.getFacilities();
		Facility myFacility=facilities.get(myActivityId);
		Activity myActivity=myFacility.getActivity(myAct.getType());

//		System.out.println("MM "+myAct.getType()+" on link "+myAct.getLinkId()+"\n"+myActivity);
		return myActivity;
	}

	public void addDate(SocializingOpportunity date){
		socializingOpportunities.add(date);
	}

	public void dropDate(SocializingOpportunity date){
		socializingOpportunities.remove(date);
	}

	public void clearDates(){
		socializingOpportunities.clear();
	}


	public int getNumKnownFacilities(){
		return knowledge.getActivities().size();
	}

//	//---?---------//

//	/**
//	* 
//	* @param myPlan
//	*/

//	public void setPlanActivities (Plan myPlan){
//	// Associate the act in the plan with a random facility on the link
//	ActIterator planActIter = myPlan.getIteratorAct();
//	while(planActIter.hasNext()){
//	Act myAct = (Act) planActIter.next();
//	Link myLink = myAct.getLink();
//	Activity myActivity = null;
//	Collection<Location> locations = myLink.getUpMapping().values();
//	Object[] facs =  locations.toArray();
//	for (int i = 0; i< facs.length;i++){
//	Facility f = (Facility) facs[i];
////	if(f!=null){
//	myActivity = f.getActivity(myAct.getType());
////	}else{
////	Gbl.errorMsg("stop, no facility found for act"+myAct.getType()+" at "+myAct.getLink().getId());
////	}
//	if(myActivity!=null){  break;}
//	}
//	if(myActivity!=null){
//	planActivities.put(myAct, myActivity);
//	break;
//	}else{
//	Gbl.errorMsg("stop, no activity found for act"+myAct.getType()+" at "+myAct.getLink().getId());
//	}
//	}
//	}
//	/**
//	* @param act
//	* @return
//	*/
//	public Activity getPlanActivity(Act act){
//	return planActivities.get(act);
//	}
//	/**
//	* @return
//	*/
//	public Collection<?> getPlanActivities(){
//	return planActivities.values();
//	}
//	/**
//	* @param act
//	* @return
//	*/
//	public Facility getPlanFacility(Act act){
//	return planActivities.get(act).getFacility();
//	}
//	/**
//	* @param act
//	* @param activity
//	*/
//	public void changePlanActivity(Act act, Activity activity){
//	// Changes to acts in plans should be associated with updates here
//	// Activity should have been added to Knowledge before, but just in case
//	if(!knowledge.getActivities().contains(activity)){
//	knowledge.addActivity(activity);
//	}
//	if(!planActivities.get(act).equals(null)){
//	planActivities.remove(act);
//	planActivities.put(act,activity);
//	}
//	if(!mapActActivity.get(act).equals(null)){
//	mapActActivity.remove(act);
//	mapActActivity.put(act,activity);
//	}
//	if(!mapActivityAct.get(activity).equals(null)){
//	mapActivityAct.remove(activity);
//	mapActivityAct.put(activity,act);
//	}
//	}

//	Move to Knowledge?
//	public final ArrayList<Activity> getActivitiesOnLink(final String type, final Link link) {
//	ArrayList<Activity> acts = new ArrayList<Activity>();
//	ArrayList<Activity> activities = knowledge.getActivities(type);
//	for (int i=0; i<activities.size(); i++) {
//	Activity a = activities.get(i);
//	if (a.getType().equals(type) && a.getFacility().getLink().equals(link)) {
//	acts.add(a);
//	}
//	}
//	return acts;
//	}

}

