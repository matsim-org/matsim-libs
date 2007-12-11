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

package playground.jhackney.knowrefac;


import java.util.Collection;
import java.util.Hashtable;

import org.matsim.basic.v01.BasicPlan.ActIterator;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Knowledge;
import org.matsim.plans.Plan;
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

	private Knowledge knowledge = null;

	public MentalMap(Knowledge knowledge){
		this.knowledge=knowledge;
	}

	public void matchActsToActivities (Plan myPlan){
		// Associate each act in the plan with a random facility on the link
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
			}else
				type="education";
			
			Link myLink = myAct.getLink();
			Activity myActivity = null;
			Collection<Location> locations = myLink.getUpMapping().values();
			Object[] facs =  locations.toArray();
			for (int i = 0; i< facs.length;i++){
				Facility f = (Facility) facs[i];
				myActivity = f.getActivity(type);
				if(myActivity!=null){
					learnActsActivities(myAct,myActivity);
					break;// only assign the first matching Activity/Facility to the Act
				}
			}
			if(myActivity==null){
				Gbl.errorMsg("stop, no activity found for act "+myAct.getType()+" at "+myAct.getLink().getId());
			}
		}
	}

	public void learnActsActivities (Act myact, Activity myactivity){
		if (!mapActivityAct.contains(myactivity)){
			System.out.println("Update Act:       "+myact);
			mapActivityAct.put(myactivity,myact);
		}
		if( !mapActActivity.contains(myact)){
			System.out.println("Update Activity:  "+myactivity);
			mapActActivity.put(myact, myactivity);
		}

		knowledge.addActivity(myactivity);

	}

	public void forgetActsActivities (Act myact, Activity myactivity){
		if (mapActivityAct.contains(myactivity)){
//			System.out.println("Update Act:       "+myact);
			mapActivityAct.remove(myactivity);
		}
		if( mapActActivity.contains(myact)){

//			System.out.println("Update Activity:  "+myactivity);
			mapActActivity.remove(myact);
		}
		knowledge.removeActivity(myactivity);
	}

	public Act getAct (Activity myActivity){
		return this.mapActivityAct.get(myActivity);
	}
	public Activity getActivity (Act myAct){

//		System.out.println("Map Act:       "+myAct);
//		System.out.println("Map Activity:  "+this.mapActActivity.get(myAct));
		return this.mapActActivity.get(myAct);
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

