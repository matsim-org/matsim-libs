/* *********************************************************************** *
 * project: org.matsim.*
 * PersonContainer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.survey.mz2005;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author illenberger
 *
 */
public class PersonContainer {

	public String id;
	
	public int age;
	
	public int referenceDay;
	
	public List<TripContainer> trips = new LinkedList<TripContainer>();
	
	public PseudoPlan plan;
	
	public void createPseudoPlan() {
		/*
		 * Sort trips to chronological order.
		 */
		SortedSet<TripContainer> sortedTrips = new TreeSet<TripContainer>(new Comparator<TripContainer>() {
			public int compare(TripContainer o1, TripContainer o2) {
				
				int result = o1.startTime - o2.endTime;
				if(result == 0) {
					if(o1 == o2)
						return 0;
					else
						return o1.hashCode() - o2.hashCode();
				} else
					return result;
			}
		});
		
		sortedTrips.addAll(this.trips);
		
		plan = new PseudoPlan();
		/*
		 * First activity is a home activity.
		 */
		PseudoActivity act = new PseudoActivity();
		act.type = ActivityType.home;
		plan.activities.add(act);
		/*
		 * Go through all trips.
		 */
		for(TripContainer trip : sortedTrips) {
			/*
			 * Set end time of previous activity.
			 */
			PseudoActivity previous = plan.activities.get(plan.activities.size()-1);
			previous.endTime = trip.startTime;
			/*
			 * Add trip to plan.
			 */
			plan.trips.add(trip);
			/*
			 * Create the succeeding activity.
			 */
			act = new PseudoActivity();
			act.startTime = trip.endTime;
			if (trip.outwardTrip) {
				/*
				 * It is the outward trip, infer the type of the activity.
				 */
				if (trip.type == 8) {
					/*
					 * It is a leisure activity, infer the type of leisure.
					 */
					if(trip.leisureType == 1)
						act.type = ActivityType.visit;
					else if(trip.leisureType == 2)
						act.type = ActivityType.bar;
					else if(trip.leisureType == 3 || trip.leisureType == 9)
						act.type = ActivityType.loutdoor;
					else if(trip.leisureType < 1) {
						//throw new IllegalArgumentException("Type not specified! Dropping complete plan.");
						act.type = ActivityType.nonleisure;
						System.err.println("Leisure type not specified. Treating as nonleisure!");
					} else
						act.type = ActivityType.lindoor;
				} else {
					/*
					 * It is not a leisure activity.
					 */
					act.type = ActivityType.nonleisure;
				}
			} else {
				/*
				 * It is a home trip or a round trip.
				 */
				act.type = ActivityType.home;
			}
			/*
			 * Add activity to plan.
			 */
			plan.activities.add(act);
		}
		/*
		 * Check if the plan closes with a home activity.
		 */
		PseudoActivity last = plan.activities.get(plan.activities.size() - 1);
		if (last.type != ActivityType.home) {
			TripContainer trip = new TripContainer();
			trip.startTime = last.endTime;
			trip.accompanists = -99;
			trip.distance = -99;
			trip.duration = -99;
			trip.leisureType = -99;
			trip.mode = -99;
			trip.endTime = Integer.MAX_VALUE;
			plan.trips.add(trip);

			act = new PseudoActivity();
			act.type = ActivityType.home;
			act.startTime = trip.endTime;
			plan.activities.add(act);
		}
	}
	
	public void removeDoubleHomes() {
		if(plan.activities.size() > 1) {
			PseudoActivity previous = plan.activities.get(0);
			for(int i = 1; i < plan.activities.size(); i++) {
				PseudoActivity current = plan.activities.get(i);
				if(current.type.equals(ActivityType.home) && previous.type.equals(ActivityType.home)) {
					plan.activities.remove(i);
					plan.trips.remove(i-1);
					i--;
					
					previous.endTime = current.endTime;
				}
				previous = current;
			}
		}
	}
}
