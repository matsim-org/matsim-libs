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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.BasicPlan.ActIterator;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.LinkImpl;
import org.matsim.plans.Act;
import org.matsim.plans.Knowledge;
import org.matsim.plans.Plan;
import org.matsim.socialnetworks.algorithms.SortHashtableByValue;
import org.matsim.socialnetworks.interactions.SocialAct;
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
	// In a Plan, each Act takes place in one Facility.
	// One Facility can have multiple Acts, however, since Facilities can have several Activities.
	// The Act will change each iteration of the mobility simulation (start/end times).
	// Its ID will be stored so that it can be found again.

//	Map of activities and acts: this is like a memory of having been someplace
	private Hashtable<Activity,Integer> mapActivityActId = new Hashtable<Activity,Integer>();
	private Hashtable<Integer, Act> actIdAct = new Hashtable<Integer, Act>();

//	Map of act and activity ID numbers. Reverse of above. Acts change so we use Id's
	private Hashtable<Integer,IdI> mapActIdActivityId = new Hashtable<Integer,IdI>();

	// Socializing opportunities are face-to-face meetings of the agents
	private ArrayList<SocialAct> socializingOpportunities = new ArrayList<SocialAct>();

//	The activity score
	private Hashtable<Activity, Double> activityScore = new Hashtable<Activity, Double>();

//	Total maximum number of activities (locations + action) an agent can remember

	private Knowledge knowledge = null;

	public MentalMap(Knowledge knowledge){
		this.knowledge=knowledge;
	}

	public void initializeActActivityMap (Plan myPlan){
		// Associate each act in the plan with a random facility on the link
		ActIterator planActIter = myPlan.getIteratorAct();
		int actId = 0;
		while(planActIter.hasNext()){
			Act myAct = (Act) planActIter.next();

//			Tidy the acts up so they correspond to the expectations of the social net module
			// Actually to the facility types, too
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
				this.actIdAct.put(actId,myAct);
				actId++;
			}
			Activity myActivity = null;
			// If there is already knowledge in the initial plans file, use it
			if(this.knowledge.getActivities(myAct.getType()).size()>0){
				myActivity=this.knowledge.getActivities(myAct.getType()).get(Gbl.random.nextInt(this.knowledge.getActivities(myAct.getType()).size()));
				learnActsActivities(myAct.getRefId(),myActivity);
			}

			// Else the activity is null and we choose an activity to assign to the act
			LinkImpl myLink = myAct.getLink();
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
					learnActsActivities(myAct.getRefId(),myActivity);
				}
			}
		}
	}

	public void learnActsActivities (Integer myactId, Activity myactivity){

		this.mapActivityActId.put(myactivity,myactId);
		this.mapActIdActivityId.put(myactId,myactivity.getFacility().getId());

		setActivityScore(myactivity);

		this.knowledge.addActivity(myactivity);
	}

	public void forgetActsActivities (Integer myactId, Activity myactivity){

		if (this.mapActivityActId.containsKey(myactivity)){
			this.mapActivityActId.remove(myactivity);
		}

		if( this.mapActIdActivityId.containsKey(myactId)){

			this.mapActIdActivityId.remove(myactId);
		}
		if(this.activityScore.containsKey(myactivity)){
			this.activityScore.remove(myactivity);
		}
		this.knowledge.removeActivity(myactivity);
	}

	public void manageMemory(int max, Plan myPlan){

		if(myPlan.getActsLegs().size()/2 >max){
			Gbl.errorMsg(this.getClass()+" Number of activites an agent has to remember is greater than his memory! MAX = "+max+" "+myPlan.getActsLegs().size()/2+" "+this.knowledge.getActivities().size());
		}
		if(this.knowledge.getActivities().size()>max){
			// Mark the activities associated with the current plan
			// so that they won't be deleted
			ArrayList<Integer> currentActs = new ArrayList<Integer>();
			ActIterator actIter = myPlan.getIteratorAct();
			while( actIter.hasNext() ){
				Act act = (Act) actIter.next();
				currentActs.add(act.getRefId());
			}

//			Sort the activities by score so that they can be managed

			Hashtable sortedScores = SortHashtableByValue.makeSortedMap(this.activityScore);

			// Remove activities if there are too many, but keep one activity
			// for each act type in the current plan. Iterator goes by score.
			int numToForget= this.activityScore.values().size()-max;

			// Avoid concurrent modification errors
			// Add to the "forgetlist" the correct number of activities to forget
			// Since these activities are sorted by score, just take the first ones
			ArrayList<Activity> forgetList=new ArrayList<Activity>();
			int counter=0;
			for (Enumeration<Activity> e = sortedScores.keys() ; e.hasMoreElements() ;) {
				Activity myactivity=e.nextElement();
				double score=(Double) sortedScores.get(myactivity);
				// note which activity to forget
				if(counter<=numToForget){
					forgetList.add(myactivity);
					counter++;
				}
			}

			Iterator<Activity> forget=forgetList.iterator();
			while(forget.hasNext()){
				Activity myactivity=forget.next();
				Act myact= getActUsingId(myactivity);
				//If the activity is assigned to an act
				if(myact != null){
					// If the act is not in the current plan, delete the mapping
					if(!(currentActs.contains(myact.getRefId()))){
						forgetActsActivities(myact.getRefId(), myactivity);
					}else{// If the act is in the current plan, then map the act to a second activity if possible, and delete the first activity
						if(mapActToNewActivity(myact, myactivity)){
						}else{//there is no alternative activity here. leave the mapping as-is
						}
					}
				}else{//If the activity is not assigned to an act, forget it
					this.knowledge.removeActivity(myactivity);
				}
			}
//			}
		}
	}

	private boolean mapActToNewActivity(Act myAct, Activity oldActivity) {
		boolean remapped = false;
		// Associate one act with a random facility on the link
		Activity newActivity = null;
		LinkImpl myLink = myAct.getLink();
		// These Locations are facilities by the new convention
		Collection<Location> locations = myLink.getUpMapping().values();
		// These Objects are facilities by convention
		Object[] facs =  locations.toArray();
		// Assign a new random activity (a facility) on the link to the act
		if(facs.length>1){
			int k = Gbl.random.nextInt(facs.length);
			int i=0;
			while((i<facs.length) && (remapped==false)){
				Facility f = (Facility) facs[(k+i)%facs.length];
				newActivity = f.getActivity(myAct.getType());
				// If a new facility of the correct type is found, remap the act to it
				if((newActivity!=null) && (newActivity!=oldActivity)){
					forgetActsActivities(myAct.getRefId(),newActivity);
					learnActsActivities(myAct.getRefId(),newActivity);
					remapped= true;
				}
				i++;
			}
		}else remapped= false;// keep mapping of act to activity if there is only one activity
		return remapped;
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

	public Act getActUsingId (Activity myActivity){
		Integer myActId =  this.mapActivityActId.get(myActivity);
		if(myActId==null){
			return null;
		}else{
			return this.actIdAct.get(myActId);
		}

	}

	public Activity getActivity (Act myAct){

		IdI myActivityId= this.mapActIdActivityId.get(myAct.getRefId());
		TreeMap<IdI,Facility> facilities=this.knowledge.getFacilities();

		if(myActivityId == null){
			Gbl.errorMsg(this.knowledge.egoNet.getEgoLinks().get(0).person1.getId().toString());
			this.knowledge.egoNet.getEgoLinks().get(0).person1.getId();
		}
		Facility myFacility=facilities.get(myActivityId);
		Activity myActivity=myFacility.getActivity(myAct.getType());

		return myActivity;
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


	public int getNumKnownFacilities(){
		return this.knowledge.getActivities().size();
	}
}

