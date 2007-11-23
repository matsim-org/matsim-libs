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

package playground.fabrice.secondloc;

import java.util.HashSet;
import java.util.TreeMap;
import java.util.Vector;

import org.matsim.gbl.Gbl;

public class MentalMap {
	
	
	// We need a private structure that manages efficiently
	// requests for activities of a given type, facilities where
	// an activity type can be performed etc.
	// It is private and can be later optimized

	private TreeMap< String,HashSet<CoolPlace>> places = new TreeMap< String,HashSet<CoolPlace>>();
	
	void learn( CoolPlace place ){
		String type = place.activity.getType();
		HashSet<CoolPlace> sp = places.get( type );
		if( sp == null ){
			sp = new HashSet<CoolPlace>();
			places.put( type, sp );
		}
		sp.add( place );
	}
	
	public CoolPlace getRandomCoolPlace(){
		Vector<CoolPlace> v = new Vector<CoolPlace>(); 
		for( HashSet<CoolPlace> tree : places.values() )		
			v.addAll( tree );
		return v.get( Gbl.random.nextInt( v.size()));
	}
	
	public CoolPlace getRandomCoolPlace( String activityType ){
		HashSet<CoolPlace> sp = places.get( activityType );
		if( sp == null )
			return null;
		Vector<CoolPlace> v = new Vector<CoolPlace>();
		v.addAll(places.get( activityType ));
		return v.get( Gbl.random.nextInt( v.size()));
	}
	
	// This method is in fact the only one which is not a wrapper around
	// existing methods from Facility or Activity
	// Indeed,
	// 1) if we know a Facility, we already have access to its Activity(s)
	// 2) if we know an Activity, we already have acccess to its Facility
	//
	
//	public Set<CoolPlace> whereIsActivityPerformable( String activity_type ){
//		return places.get( activity_type );
//	}
//	
//	// This below is just a convenient wrapper method
//	public TreeMap<String,Activity>  getPerformableActivities( Facility facility ){
//		return  facility.getActivities();
//	}
//	
////	 This below is just a convenient wrapper method
//	public TreeSet<Opentime> whenIsAcvitityPerformableAt( Facility facility, String activity_type, String day ){
//		Activity act = facility.getActivities().get(activity_type);
//		return act.getOpentimes(day);
//	}
//	
//	// The activity space should be computed here depending on all
//	// the information available in the Knowledge
//	ActivitySpace getActivitySpace(){
//		return null;
//	}

	// Create/Modify the private container of facilities
//	void addFacility(final Facility facility){
//		TreeMap<String, Activity> acts = facility.getActivities();
//		for( String type : acts.keySet() ){
//			TreeSet<Facility> tr = facilities.get( type );
//			if( tr == null ){
//				tr = new TreeSet<Facility>();
//				facilities.put( type, tr );
//			}
//			tr.add( facility );
//		}
//	}
	


}
