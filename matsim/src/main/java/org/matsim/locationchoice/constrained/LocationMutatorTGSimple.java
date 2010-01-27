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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.locationchoice.LocationMutator;
import org.matsim.locationchoice.utils.DefineFlexibleActivities;
import org.matsim.locationchoice.utils.QuadTreeRing;

public class LocationMutatorTGSimple extends LocationMutator {

	protected int unsuccessfullLC = 0;
	private final DefineFlexibleActivities defineFlexibleActivities;

	public LocationMutatorTGSimple(final Network network, Controler controler, Knowledges knowledges,
			TreeMap<String, QuadTreeRing<ActivityFacility>> quad_trees,
			TreeMap<String, ActivityFacilityImpl []> facilities_of_type) {

		super(network, controler, knowledges, quad_trees, facilities_of_type);
		this.defineFlexibleActivities = new DefineFlexibleActivities(this.knowledges, controler.getConfig().locationchoice());
	}

	@Override
	public void handlePlan(final Plan plan){

		List<ActivityImpl> flexibleActivities = this.getFlexibleActivities(plan);

		if (flexibleActivities.size() == 0) {
			this.unsuccessfullLC++;
			return;
		}
		Collections.shuffle(flexibleActivities);
		ActivityImpl actToMove = flexibleActivities.get(0);
		List<?> actslegs = plan.getPlanElements();
		int indexOfActToMove = actslegs.indexOf(actToMove);

		// starting home and ending home are never flexible
		final LegImpl legPre = (LegImpl)actslegs.get(indexOfActToMove -1);
		final LegImpl legPost = (LegImpl)actslegs.get(indexOfActToMove + 1);
		final ActivityImpl actPre = (ActivityImpl)actslegs.get(indexOfActToMove - 2);
		final ActivityImpl actPost = (ActivityImpl)actslegs.get(indexOfActToMove + 2);

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

	private List<ActivityImpl> getFlexibleActivities(final Plan plan) {
		List<ActivityImpl> flexibleActivities = new Vector<ActivityImpl>();
		if (!super.locationChoiceBasedOnKnowledge) {
			flexibleActivities = this.defineFlexibleActivities.getFlexibleActivities(plan);
		}
		else {
			flexibleActivities = defineMovablePrimaryActivities(plan);
			List<?> actslegs = plan.getPlanElements();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				final ActivityImpl act = (ActivityImpl)actslegs.get(j);
				if (!this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).isPrimary(act.getType(), act.getFacilityId()) &&
						!(act.getType().startsWith("h") || act.getType().startsWith("tta"))) {
					flexibleActivities.add(act);
				}
			}
		}
		return flexibleActivities;
	}

	protected boolean modifyLocation(ActivityImpl act, Coord startCoord, Coord endCoord, double radius) {
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
		act.setFacilityId(facility.getId());
   		act.setLinkId(((NetworkImpl) this.network).getNearestLink(facility.getCoord()).getId());
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

