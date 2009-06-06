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
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.locationchoice.LocationMutator;
import org.matsim.locationchoice.utils.DefineFlexibleActivities;

public class LocationMutatorTGSimple extends LocationMutator {
	
	protected int unsuccessfullLC = 0;
	DefineFlexibleActivities defineFlexibleActivities = new DefineFlexibleActivities();
	
	public LocationMutatorTGSimple(final NetworkLayer network, Controler controler,
			TreeMap<String, QuadTree<ActivityFacility>> quad_trees,
			TreeMap<String, ActivityFacility []> facilities_of_type) {
		
		super(network, controler, quad_trees, facilities_of_type);
	}
		
	public void handlePlan(final Plan plan){

		List<Activity> flexibleActivities = new Vector<Activity>();
		if (this.defineFlexibleActivities.getFlexibleTypes().size() > 0) {
			flexibleActivities = this.defineFlexibleActivities.getFlexibleActivities(plan);
		}
		else {
			flexibleActivities = defineMovablePrimaryActivities(plan);			
			List<?> actslegs = plan.getPlanElements();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				final Activity act = (Activity)actslegs.get(j);		
				if (!plan.getPerson().getKnowledge().isPrimary(act.getType(), act.getFacilityId()) && 
						!(act.getType().startsWith("h") || act.getType().startsWith("tta"))) {
					flexibleActivities.add(act);
				}
			}
		}
		
		if (flexibleActivities.size() == 0) {
			this.unsuccessfullLC++;
			return;
		}
		
		Collections.shuffle(flexibleActivities);
		Activity actToMove = flexibleActivities.get(0);
		List<?> actslegs = plan.getPlanElements();
		int index = actslegs.indexOf(actToMove);
		
		// starting home and ending home are never flexible
		final Leg legPre = (Leg)actslegs.get(index -1);
		final Leg legPost = (Leg)actslegs.get(index + 1);
		final Activity actPre = (Activity)actslegs.get(index - 2);
		final Activity actPost = (Activity)actslegs.get(index + 2);
		
		double travelDistancePre = 0.0;
		double travelDistancePost = 0.0;
		
		if (legPre.getMode().compareTo(TransportMode.car) == 0) {
			travelDistancePre = legPre.getRoute().getDistance() + legPost.getRoute().getDistance();
		}
		else {
			travelDistancePre = Math.sqrt(Math.pow(actPre.getCoord().getX() - actToMove.getCoord().getX(), 2.0) +
				Math.pow(actPre.getCoord().getY() - actToMove.getCoord().getY(), 2.0));
		}
		if (legPost.getMode().compareTo(TransportMode.car) == 0) {
			travelDistancePost = legPre.getRoute().getDistance() + legPost.getRoute().getDistance();
		}
		else {
			travelDistancePost = Math.sqrt(Math.pow(actPost.getCoord().getX() - actToMove.getCoord().getX(), 2.0) +
				Math.pow(actPost.getCoord().getY() - actToMove.getCoord().getY(), 2.0));
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
		
		actslegs = plan.getPlanElements();
		// loop over all <leg>s, remove route-information
		// routing is done after location choice
		for (int j = 1; j < actslegs.size(); j=j+2) {
			final Leg leg = (Leg)actslegs.get(j);
			leg.setRoute(null);
		}
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


/*
log.info("--------------------------------------------");
log.info("Person " + plan.getPerson().getId());
log.info("facility " + actToMove.getFacilityId());
log.info("tdPre " + travelDistancePre);
log.info("tdPost" + travelDistancePost);
log.info(actPre);
log.info(actPost);
log.info(legPre.getRoute());
log.info(legPost.getRoute());
log.info(legPre.getMode());
log.info(legPost.getMode());
*/

