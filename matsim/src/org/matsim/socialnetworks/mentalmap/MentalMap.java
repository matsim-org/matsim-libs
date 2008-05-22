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
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Knowledge;
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
	private Hashtable<Activity,Act> mapActivityAct = new Hashtable<Activity,Act>();
//	private Hashtable<Integer, Act> actIdAct = new Hashtable<Integer, Act>();

//	Map of act and activity ID numbers. Reverse of above. Acts change so we use Id's
	private Hashtable<Act,Id> mapActActivityId = new Hashtable<Act,Id>();

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
				learnActsActivities(myAct,myActivity);
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
					learnActsActivities(myAct,myActivity);
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
			Activity myActivity = knowledge.getFacilities().get(myActivityId).getActivity(myAct.getType());
			if(myActivity!=null){
				learnActsActivities(myAct,myActivity);
			}	
		}

	}

	public void prepareActs(Plan myPlan){

//		Tidy the acts up so they correspond to the expectations of the social net module.
//		First, change the types to be the same as the facility types.
//		Should not have to be done: adapt SocialNetworks code to use only the first letter
//		or else some standard MATSim "type", if this exists
//		Next, make sure each act has a number within the plan

		ActIterator planActIter = myPlan.getIteratorAct();
		int actId = 0;
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

			if(myAct.getRefId()==Integer.MIN_VALUE){
				myAct.setRefId(actId);
//				this.actIdAct.put(actId,myAct);
				actId++;
			}else{
//				this.actIdAct.put(myAct.getRefId(),myAct);
			}
		}
	}
	public void learnActsActivities (Act myAct, Activity myactivity){

//		int myActId = myAct.getRefId();
		this.mapActivityAct.put(myactivity,myAct);
		this.mapActActivityId.put(myAct,myactivity.getFacility().getId());

		// Drop (myActId,myAct)
//		System.out.println("Removing act number: "+myActId);
//		this.actIdAct.remove(myActId);
//		System.out.println("Adding act index "+myActId+" "+myAct);
//		this.actIdAct.put(myActId,myAct);
		
		setActivityScore(myactivity);

		this.knowledge.addActivity(myactivity);
	}

	public void forgetActsActivities (Act myAct, Activity myactivity){

		if (this.mapActivityAct.containsKey(myactivity)){
			this.mapActivityAct.remove(myactivity);
		}

		if( this.mapActActivityId.containsKey(myAct)){

			this.mapActActivityId.remove(myAct);
		}
		if(this.activityScore.containsKey(myactivity)){
			this.activityScore.remove(myactivity);
		}
		this.knowledge.removeActivity(myactivity);
	}

	public void manageMemory(int max, List<Plan> myPlans){

		if(myPlans.get(0).getActsLegs().size()*myPlans.size()/2 >max){
			Gbl.errorMsg(this.getClass()+
					" Number of activites an agent needs to remember for his plans is greater than his memory! MAX = "+max+" "+myPlans.get(0).getActsLegs().size()*myPlans.size()/2+" "+this.knowledge.getActivities().size());
		}

		if(this.knowledge.getActivities().size()>max){
			// Mark the activities associated with all plans in memory
			// so that they won't be deleted
			ArrayList<Integer> currentActs = new ArrayList<Integer>();
			Iterator<Plan> planIter = myPlans.iterator();
			while(planIter.hasNext()){
				Plan myPlan = (Plan) planIter.next();
				ActIterator actIter = myPlan.getIteratorAct();
				while( actIter.hasNext() ){
					Act act = (Act) actIter.next();
					currentActs.add(act.getRefId());
				}
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
//				double score=(Double) sortedScores.get(myactivity);
				// note which activity to forget
				
				// JH 21.05.2008 I don't know what happens if a Plan is deleted, along with its
				// Acts. Make sure no null Acts are kept
				if(this.getActUsingId(myactivity)== null){
//					log.info("the Act is null");
					forgetList.add(myactivity);
					counter++;
				}
			}
			// If there are still too many activities 
			for (Enumeration<Activity> e = sortedScores.keys() ; e.hasMoreElements() ;) {
				Activity myactivity=e.nextElement();
//				double score=(Double) sortedScores.get(myactivity);
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
					// If the act is not in the current list, delete the mapping
					if(!(currentActs.contains(myact.getRefId()))){
						forgetActsActivities(myact, myactivity);
					}else{// If the act is in the current list, then map the act to a second activity if possible, and delete the first activity
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
		Random rnd = new Random(111);
		boolean remapped = false;
		// Associate one act with a random facility on the link
//		System.out.println(this.getClass()+" WARNING WARNING WARNING assignment is not the same between runs. Use a new Gbl.random(seed) here!");
		Activity newActivity = null;
		Link myLink = myAct.getLink();
		// These Locations are facilities by the new convention
		Collection<Location> locations = myLink.getUpMapping().values();
		// These Objects are facilities by convention
		Object[] facs =  locations.toArray();
		// Assign a new random activity (a facility) on the link to the act
		if(facs.length>1){
//			int k = Gbl.random.nextInt(facs.length);
			int k = rnd.nextInt(facs.length);
			int i=0;
			while((i<facs.length) && (remapped==false)){
				Facility f = (Facility) facs[(k+i)%facs.length];
				newActivity = f.getActivity(myAct.getType());
				// If a new facility of the correct type is found, remap the act to it
				if((newActivity!=null) && (newActivity!=oldActivity)){
					forgetActsActivities(myAct,newActivity);
					learnActsActivities(myAct,newActivity);
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
		Act myAct =  this.mapActivityAct.get(myActivity);
		if(myAct==null){
			return null;
		}else{
//			return this.actIdAct.get(myActId);
			return myAct;
			
		}

	}

	public Activity getActivity (Act myAct){

		// Debugging test
		
		Object mycollect[]=mapActActivityId.keySet().toArray();
		boolean matchFound=false;
		for(int jjj = 0; jjj<mycollect.length;jjj++){
			
//			System.out.println("MentalMap2 Act "+mycollect[jjj].toString());
//			System.out.println("   "+(mycollect[jjj].equals(myAct)));
			if(mycollect[jjj].equals(myAct)){
				matchFound=true;
			}
		}
		if(matchFound==false){
			System.out.println("MentalMap2 myAct "+myAct.toString());
			System.out.println("MentalMap2 Act stop");
		}
		
		
		Id myActivityId= this.mapActActivityId.get(myAct);
		TreeMap<Id,Facility> facilities=this.knowledge.getFacilities();

		if(myActivityId == null){
			System.out.println("DEbug mentalmap2");
			Gbl.errorMsg(this.knowledge.egoNet.getEgoLinks().get(0).person1.getId().toString());
		}
		Facility myFacility=facilities.get(myActivityId);
		Activity myActivity=myFacility.getActivity(myAct.getType());

		return myActivity;
	}
	
//	public Hashtable<Integer,Act> getActIdAct(){
//		return this.actIdAct;
//	}

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
	public Collection<Act> getActs(){
		return this.mapActivityAct.values();
	}
}


