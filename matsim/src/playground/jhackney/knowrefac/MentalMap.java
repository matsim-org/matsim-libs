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
import java.util.Iterator;

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

    private Hashtable<Act,Activity> planActivities = new Hashtable<Act,Activity>();
    private Hashtable<Act,Activity> mapActActivity = new Hashtable<Act,Activity>();
    private Hashtable<Activity,Act> mapActivityAct = new Hashtable<Activity,Act>();

    private Knowledge knowledge = null;

    public MentalMap(Knowledge knowledge){
	this.knowledge=knowledge;
    }

    public void initialMatchActsActivities (Plan myPlan){
	// Associate the act in the plan with a random facility on the link
	Iterator planActIter = (Iterator) myPlan.getIteratorAct();
	while(planActIter.hasNext()){
	    Act myAct = (Act) planActIter.next();
	    Link myLink = myAct.getLink();
	    Activity myActivity = null;
	    Collection<Location> locations = myLink.getUpMapping().values();
	    Object[] facs =  locations.toArray();
	    for (int i = 0; i< facs.length;i++){
		Facility f = (Facility) facs[i];
		myActivity = f.getActivity(myAct.getType());
		if(myActivity!=null){
		    // at this point, neither myActivity nor myAct are null
		    // check to see if this pair is already in the map
		    if( this.getActivity(myAct)==null){
			mapActActivity.put(myAct, myActivity);}
		    if (this.getAct(myActivity)==null){
			mapActivityAct.put(myActivity,myAct);
		    }
		    break;
		}
	    }
	    if(myActivity==null){

		Gbl.errorMsg("stop, no activity found for act "+myAct.getType()+" at "+myAct.getLink().getId());
	    }
	}   
    }

    public void updateMatchActsActivities (Act myact, Activity myactivity){
	if (this.getAct(myactivity)==null){
//	    System.out.println("Update Act:       "+myact);
	    mapActivityAct.put(myactivity,myact);
	}
	if( this.getActivity(myact)==null){

//	    System.out.println("Update Activity:  "+myactivity);
	    mapActActivity.put(myact, myactivity);}

    }

    public Act getAct (Activity myActivity){
	return this.mapActivityAct.get(myActivity);
    }
    public Activity getActivity (Act myAct){

//	System.out.println("Map Act:       "+myAct);
//	System.out.println("Map Activity:  "+this.mapActActivity.get(myAct));
	return this.mapActActivity.get(myAct);
    }


    //---?---------//
    public void setPlanActivities (Plan myPlan){
	// Associate the act in the plan with a random facility on the link
	Iterator planActIter = (Iterator) myPlan.getIteratorAct();
	while(planActIter.hasNext()){
	    Act myAct = (Act) planActIter.next();
	    Link myLink = myAct.getLink();
	    Activity myActivity = null;
	    Collection<Location> locations = myLink.getUpMapping().values();
	    Object[] facs =  locations.toArray();
	    for (int i = 0; i< facs.length;i++){
		Facility f = (Facility) facs[i];
//		if(f!=null){
		myActivity = f.getActivity(myAct.getType());
//		}else{
//		Gbl.errorMsg("stop, no facility found for act"+myAct.getType()+" at "+myAct.getLink().getId());
//		}
		if(myActivity!=null){  break;}
	    }
	    if(myActivity!=null){
		planActivities.put(myAct, myActivity);
		break;
	    }else{
		Gbl.errorMsg("stop, no activity found for act"+myAct.getType()+" at "+myAct.getLink().getId());
	    }
	}
    }
    public Activity getPlanActivity(Act act){
	return planActivities.get(act);
    }

    public Collection getPlanActivities(){
	return planActivities.values();
    }

    public Facility getPlanFacility(Act act){
	return planActivities.get(act).getFacility();
    }

    public void changePlanActivity(Act act, Activity activity){
	// Changes to acts in plans should be associated with updates here
	// Activity should have been added to Knowledge before, but just in case
	if(!knowledge.getActivities().contains(activity)){
	    knowledge.addActivity(activity);
	}
	if(!planActivities.get(act).equals(null)){
	    planActivities.remove(act);
	    planActivities.put(act,activity);
	}
	if(!mapActActivity.get(act).equals(null)){
	    mapActActivity.remove(act);
	    mapActActivity.put(act,activity);
	}
	if(!mapActivityAct.get(activity).equals(null)){
	    mapActivityAct.remove(activity);
	    mapActivityAct.put(activity,act);
	}
    }

//  Move to Knowledge?
//  public final ArrayList<Activity> getActivitiesOnLink(final String type, final Link link) {
//  ArrayList<Activity> acts = new ArrayList<Activity>();
//  ArrayList<Activity> activities = knowledge.getActivities(type);		
//  for (int i=0; i<activities.size(); i++) {
//  Activity a = activities.get(i);
//  if (a.getType().equals(type) && a.getFacility().getLink().equals(link)) {
//  acts.add(a);
//  }
//  }
//  return acts;
//  }
    public int getNumKnownFacilities(){
	return knowledge.getActivities().size(); 
    }



    //-----------------------old and in the way------------------------//    
    // We need a private structure that manages efficiently
    // requests for activities of a given type, facilities where
    // an activity type can be performed etc.
    // It is private and can be later optimized

//  private TreeMap< String,HashSet<CoolPlace>> places = new TreeMap< String,HashSet<CoolPlace>>();

//  void learn( CoolPlace place ){
//  String type = place.activity.getType();
//  HashSet<CoolPlace> sp = places.get( type );
//  if( sp == null ){
//  sp = new HashSet<CoolPlace>();
//  places.put( type, sp );
//  }
//  sp.add( place );
//  }

//  public CoolPlace getRandomCoolPlace(){
//  Vector<CoolPlace> v = new Vector<CoolPlace>(); 
//  for( HashSet<CoolPlace> tree : places.values() )		
//  v.addAll( tree );
//  return v.get( Gbl.random.nextInt( v.size()));
//  }

//  public CoolPlace getRandomCoolPlace( String activityType ){
//  HashSet<CoolPlace> sp = places.get( activityType );
//  if( sp == null )
//  return null;
//  Vector<CoolPlace> v = new Vector<CoolPlace>();
//  v.addAll(places.get( activityType ));
//  return v.get( Gbl.random.nextInt( v.size()));
//  }

//  public Vector<CoolPlace> getCoolPlacesOfType(String activityType){
//  HashSet<CoolPlace> sp = places.get( activityType );
//  if( sp == null )
//  return null;
//  Vector<CoolPlace> v = new Vector<CoolPlace>();
//  v.addAll(places.get( activityType ));
//  return v;
//  }
//  // JH For debugging learning
//  public int getNumKnownFacilities(){
//  int num=0;
//  Vector<CoolPlace> v = new Vector<CoolPlace>();
//  for( HashSet<CoolPlace> tree : places.values() )		
//  v.addAll( tree );
//  num=v.size();
////Iterator viter = v.iterator();
////while(viter.hasNext()){
////CoolPlace place = (CoolPlace) viter.next();
////num++;
////}
//  return num;
//  }
//  public Vector<CoolPlace> getAllPlaces(){
//  Vector<CoolPlace> v = new Vector<CoolPlace>();
//  for( HashSet<CoolPlace> tree : places.values() )		
//  v.addAll( tree );
//  return v;
//  }
    // JH debugging end

    // This method is in fact the only one which is not a wrapper around
    // existing methods from Facility or Activity
    // Indeed,
    // 1) if we know a Facility, we already have access to its Activity(s)
    // 2) if we know an Activity, we already have acccess to its Facility
    //

//  public Set<CoolPlace> whereIsActivityPerformable( String activity_type ){
//  return places.get( activity_type );
//  }

//  // This below is just a convenient wrapper method
//  public TreeMap<String,Activity>  getPerformableActivities( Facility facility ){
//  return  facility.getActivities();
//  }

////This below is just a convenient wrapper method
//  public TreeSet<Opentime> whenIsAcvitityPerformableAt( Facility facility, String activity_type, String day ){
//  Activity act = facility.getActivities().get(activity_type);
//  return act.getOpentimes(day);
//  }

//  // The activity space should be computed here depending on all
//  // the information available in the Knowledge
//  ActivitySpace getActivitySpace(){
//  return null;
//  }

    // Create/Modify the private container of facilities
//  void addFacility(final Facility facility){
//  TreeMap<String, Activity> acts = facility.getActivities();
//  for( String type : acts.keySet() ){
//  TreeSet<Facility> tr = facilities.get( type );
//  if( tr == null ){
//  tr = new TreeSet<Facility>();
//  facilities.put( type, tr );
//  }
//  tr.add( facility );
//  }
//  }



}

