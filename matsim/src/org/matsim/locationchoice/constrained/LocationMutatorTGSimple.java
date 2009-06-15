/* *********************************************************************** *
 * project: org.matsim.*
 * LocationMutatorwChoiceSet.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.locationchoice.constrained;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.locationchoice.LocationMutator;
import org.matsim.locationchoice.utils.DefineFlexibleActivities;
import org.matsim.locationchoice.utils.QuadTreeRing;

public class LocationMutatorTGSimple extends LocationMutator {
	
	protected int unsuccessfullLC = 0;
	DefineFlexibleActivities defineFlexibleActivities = new DefineFlexibleActivities(this.knowledges);
	
	public LocationMutatorTGSimple(final NetworkLayer network, Controler controler, Knowledges knowledges,
			TreeMap<String, QuadTreeRing<ActivityFacility>> quad_trees,
			TreeMap<String, ActivityFacility []> facilities_of_type) {
		
		super(network, controler, knowledges, quad_trees, facilities_of_type);
	}
		
	@Override
	public void handlePlan(final Plan plan){

		List<Activity> flexibleActivities = this.getFlexibleActivities(plan);
		
		if (flexibleActivities.size() == 0) {
			this.unsuccessfullLC++;
			return;
		}	
		Collections.shuffle(flexibleActivities);
		Activity actToMove = flexibleActivities.get(0);
		List<?> actslegs = plan.getPlanElements();
		int indexOfActToMove = actslegs.indexOf(actToMove);
		
		// starting home and ending home are never flexible
		final Leg legPre = (Leg)actslegs.get(indexOfActToMove -1);
		final Leg legPost = (Leg)actslegs.get(indexOfActToMove + 1);
		final Activity actPre = (Activity)actslegs.get(indexOfActToMove - 2);
		final Activity actPost = (Activity)actslegs.get(indexOfActToMove + 2);
		
		double travelDistancePre = 0.0;
		double travelDistancePost = 0.0;
		
		if (legPre.getMode().compareTo(TransportMode.car) == 0) {
			travelDistancePre = legPre.getRoute().getDistance();
		}
		else {
			travelDistancePre = ((CoordImpl)actPre.getCoord()).calcDistance(actToMove.getCoord());
		}
		if (legPost.getMode().compareTo(TransportMode.car) == 0) {
			travelDistancePost = legPost.getRoute().getDistance();
		}
		else {
			travelDistancePost = ((CoordImpl)actToMove.getCoord()).calcDistance(actPost.getCoord());
		}
		double radius =  0.5 * (travelDistancePre + travelDistancePost);
				
		if (Double.isNaN(radius)) {
			this.unsuccessfullLC++;
			return;
		}
		
		if (!this.modifyLocation(actToMove, actPre.getCoord(), actPost.getCoord(), radius)) {
			this.unsuccessfullLC++;
			return;
		}
		super.resetRoutes(plan);
	}
	
	private List<Activity> getFlexibleActivities(final Plan plan) {
		List<Activity> flexibleActivities = new Vector<Activity>();
		if (!super.locationChoiceBasedOnKnowledge) {
			flexibleActivities = this.defineFlexibleActivities.getFlexibleActivities(plan);
		}
		else {
			flexibleActivities = defineMovablePrimaryActivities(plan);			
			List<?> actslegs = plan.getPlanElements();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				final Activity act = (Activity)actslegs.get(j);		
				if (!this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).isPrimary(act.getType(), act.getFacilityId()) && 
						!(act.getType().startsWith("h") || act.getType().startsWith("tta"))) {
					flexibleActivities.add(act);
				}
			}
		}
		return flexibleActivities;
	}
			
	protected boolean modifyLocation(Activity act, Coord startCoord, Coord endCoord, double radius) {		
		double midPointX = (startCoord.getX() + endCoord.getX()) / 2.0;
		double midPointY = (startCoord.getY() + endCoord.getY()) / 2.0;	
		ArrayList<ActivityFacility> facilitySet = 
			(ArrayList<ActivityFacility>) this.quadTreesOfType.get(act.getType()).get(midPointX, midPointY, radius);
		
		ActivityFacility facility = null;
		if (facilitySet.size() > 1) {
			facility = facilitySet.get(MatsimRandom.getRandom().nextInt(facilitySet.size()));
		}
		else {
			return false;
		}
		act.setFacility(facility);
   		act.setLink(this.network.getNearestLink(facility.getCoord()));
   		act.setCoord(facility.getCoord());
   		
   		return true;
	}
		
	public int getNumberOfUnsuccessfull() {
		return this.unsuccessfullLC;		
	}
	
	public void resetUnsuccsessfull() {
		this.unsuccessfullLC = 0;
	}	
}

