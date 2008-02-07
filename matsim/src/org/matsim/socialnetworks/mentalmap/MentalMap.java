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
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.basic.v01.BasicPlan.ActIterator;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Knowledge;
import org.matsim.plans.Plan;
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
	// The facility and activity type are in the PLAN
	// Activities should point to ACTs

	//private Hashtable<Act,Activity> planActivities = new Hashtable<Act,Activity>();// ?

	private Hashtable<Act,Activity> mapActActivity = new Hashtable<Act,Activity>();
	private Hashtable<Activity,Act> mapActivityAct = new Hashtable<Activity,Act>();
//	private Hashtable<IdI,Integer> mapActivityIdActId = new Hashtable<IdI,Integer>();
	private Hashtable<Integer,IdI> mapActIdActivityId = new Hashtable<Integer,IdI>();
	
	private ArrayList<SocializingOpportunity> dates = new ArrayList<SocializingOpportunity>();

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
			
// Tidy the acts up so they correspond to the expectations of the social net module
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
		mapActActivity.put(myact, myactivity);
		mapActIdActivityId.put(myact.getRefId(),myactivity.getFacility().getId());

		knowledge.addActivity(myactivity);
	}

	public void forgetActsActivities (Act myact, Activity myactivity){
		if (mapActivityAct.containsKey(myactivity)){
//			System.out.println("Update Act:       "+myact);
			mapActivityAct.remove(myactivity);
		}
		if( mapActActivity.containsKey(myact)){

//			System.out.println("Update Activity:  "+myactivity);
			mapActActivity.remove(myact);
			// NOTE could crash here if myact is in mapActActivity but was overwritten in mapActIdActivityId,
			// which is less specific about the details of the Act saved in it
			mapActIdActivityId.remove(myact.getRefId());
		}
		knowledge.removeActivity(myactivity);
	}

	public void manageMemory(int max){
		//First sort the activities
		
		// Then remove the least useful ones
		for(int i=0;i<this.knowledge.getActivities().size()-max;i++){
//		if(this.knowledge.getActivities().size()>max){
			// Remove one act at a time until size < max
			
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
		dates.add(date);
	}

	public void dropDate(SocializingOpportunity date){
		dates.remove(date);
	}

	public void clearDates(){
		dates.clear();
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

