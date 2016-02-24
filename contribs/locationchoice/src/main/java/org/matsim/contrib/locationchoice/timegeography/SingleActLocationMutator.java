/* *********************************************************************** *
 * project: org.matsim.*
 * SingleActLocationMutator.java
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

package org.matsim.contrib.locationchoice.timegeography;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.LocationMutator;
import org.matsim.contrib.locationchoice.utils.ActivitiesHandler;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

public class SingleActLocationMutator extends LocationMutator {

	protected int unsuccessfullLC = 0;
	private final ActivitiesHandler defineFlexibleActivities;

	public SingleActLocationMutator(final Scenario scenario, TreeMap<String, ? extends QuadTree<ActivityFacility>> quad_trees, 
			TreeMap<String, ActivityFacilityImpl []> facilities_of_type,
			Random random) {

		super(scenario, quad_trees, facilities_of_type, random);
		this.defineFlexibleActivities = new ActivitiesHandler((DestinationChoiceConfigGroup) scenario.getConfig().getModule("locationchoice"));
	}

	@Override
	public final void run(final Plan plan){

		List<Activity> flexibleActivities = this.getFlexibleActivities(plan);

		if (flexibleActivities.size() == 0) {
			this.unsuccessfullLC++;
			return;
		}
		Collections.shuffle(flexibleActivities);
		Activity actToMove = flexibleActivities.get(0);
		List<PlanElement> actslegs = plan.getPlanElements();
		int indexOfActToMove = actslegs.indexOf(actToMove);

		// starting home and ending home are never flexible
		final Leg legPre = (Leg)actslegs.get(indexOfActToMove -1);
		final Leg legPost = (Leg)actslegs.get(indexOfActToMove + 1);
		final Activity actPre = (Activity)actslegs.get(indexOfActToMove - 2);
		final Activity actPost = (Activity)actslegs.get(indexOfActToMove + 2);

		double travelDistancePre = 0.0;
		double travelDistancePost = 0.0;

		if (legPre.getMode().compareTo(TransportMode.car) == 0) {
			travelDistancePre = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) legPre.getRoute(), this.scenario.getNetwork());
		}
		else {
			travelDistancePre = CoordUtils.calcEuclideanDistance(actPre.getCoord(), actToMove.getCoord());
		}
		if (legPost.getMode().compareTo(TransportMode.car) == 0) {
			travelDistancePost = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) legPost.getRoute(), this.scenario.getNetwork());
		}
		else {
			travelDistancePost = CoordUtils.calcEuclideanDistance(actToMove.getCoord(), actPost.getCoord());
		}
		double radius =  0.5 * (travelDistancePre + travelDistancePost);

		if (Double.isNaN(radius)) {
			this.unsuccessfullLC++;
			return;
		}

		if (!this.modifyLocation((ActivityImpl) actToMove, actPre.getCoord(), actPost.getCoord(), radius)) {
			this.unsuccessfullLC++;
			return;
		}
		PlanUtils.resetRoutes(plan);
	}

	private List<Activity> getFlexibleActivities(final Plan plan) {
		List<Activity> flexibleActivities;
		flexibleActivities = this.defineFlexibleActivities.getFlexibleActivities(plan);
		return flexibleActivities;
	}

	protected final boolean modifyLocation(ActivityImpl act, Coord startCoord, Coord endCoord, double radius) {
		double midPointX = (startCoord.getX() + endCoord.getX()) / 2.0;
		double midPointY = (startCoord.getY() + endCoord.getY()) / 2.0;
		ArrayList<ActivityFacility> facilitySet =
				(ArrayList<ActivityFacility>) this.quadTreesOfType.get(this.defineFlexibleActivities.getConverter().convertType(act.getType())).
						getDisk(midPointX, midPointY, radius);

		ActivityFacility facility = null;
		if (facilitySet.size() > 1) {
			facility = facilitySet.get(super.random.nextInt(facilitySet.size()));
		}
		else {
			return false;
		}
		act.setFacilityId(facility.getId());
   		act.setLinkId(NetworkUtils.getNearestLink(((NetworkImpl) this.scenario.getNetwork()), facility.getCoord()).getId());
   		act.setCoord(facility.getCoord());

   		return true;
	}

	public final int getNumberOfUnsuccessfull() {
		return this.unsuccessfullLC;
	}

	public final void resetUnsuccsessfull() {
		this.unsuccessfullLC = 0;
	}
}

