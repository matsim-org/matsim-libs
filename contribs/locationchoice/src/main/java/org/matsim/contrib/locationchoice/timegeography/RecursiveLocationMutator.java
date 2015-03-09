/* *********************************************************************** *
 * project: org.matsim.*
 * RecursiveLocationMutator.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.LocationMutator;
import org.matsim.contrib.locationchoice.utils.QuadTreeRing;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.old.PlanRouterAdapter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

import java.util.*;

public class RecursiveLocationMutator extends LocationMutator {

//	private static final Logger log = Logger.getLogger(LocationMutatorwChoiceSet.class);
	protected int unsuccessfullLC = 0;
	private double recursionTravelSpeedChange = 0.1;
	private double recursionTravelSpeed = 30.0;
	protected int maxRecursions = 10;
	private TripRouter router;

	public RecursiveLocationMutator(final Scenario scenario, TripRouter router,
			TreeMap<String, QuadTreeRing<ActivityFacility>> quad_trees,
			TreeMap<String, ActivityFacilityImpl []> facilities_of_type, Random random) {
		super(scenario, quad_trees, facilities_of_type, random);
		this.recursionTravelSpeedChange = Double.parseDouble(scenario.getConfig().findParam("locationchoice", "recursionTravelSpeedChange"));
		this.maxRecursions = Integer.parseInt(scenario.getConfig().findParam("locationchoice", "maxRecursions"));
		this.recursionTravelSpeed = Double.parseDouble(scenario.getConfig().findParam("locationchoice", "travelSpeed_car"));
		this.router = router;
	}

	public RecursiveLocationMutator(final Scenario scenario, TripRouter router, Random random) {
		super(scenario, random);
		this.recursionTravelSpeedChange = Double.parseDouble(scenario.getConfig().findParam("locationchoice", "recursionTravelSpeedChange"));
		this.maxRecursions = Integer.parseInt(scenario.getConfig().findParam("locationchoice", "maxRecursions"));
		this.recursionTravelSpeed = Double.parseDouble(scenario.getConfig().findParam("locationchoice", "travelSpeed_car"));
		this.router = router;
	}

	@Override
	public void run(final Plan plan){
		List<SubChain> subChains = this.calcActChains(plan);
		this.handleSubChains(plan, subChains);
		super.resetRoutes(plan);
	}

	protected TripRouter getTripRouter() {
		return this.router;
	}
	
	public int getNumberOfUnsuccessfull() {
		return this.unsuccessfullLC;
	}

	public void resetUnsuccsessfull() {
		this.unsuccessfullLC = 0;
	}

	public void handleSubChains(final Plan plan, List<SubChain> subChains) {
		Iterator<SubChain> sc_it = subChains.iterator();
		while (sc_it.hasNext()) {
			SubChain sc = sc_it.next();

			//initially using 25.3 km/h + 20%
			// micro census 2005
			//double speed = 30.36/3.6;
			double speed = this.recursionTravelSpeed;

			if (sc.getTtBudget() < 1.0) {
				continue;
			}

			int nrOfTrials = 0;
			int change = -2;
			boolean shrinked = false;
			while (change != 0) {
				// shrinking only every second time
				if ((change == -1) && shrinked) {
					speed *= (1.0 - this.recursionTravelSpeedChange);
					shrinked = true;
				}
				else if (change == 1) {
					speed *= (1.0 + this.recursionTravelSpeedChange);
					shrinked = false;
				}
				change = this.handleSubChain(plan.getPerson(), sc, speed, nrOfTrials);
				nrOfTrials++;
			}
		}
	}

	protected int handleSubChain(Person person, SubChain subChain, double speed, int trialNr){
		if (trialNr > this.maxRecursions) {
			this.unsuccessfullLC += 1;

			Iterator<Activity> act_it = subChain.getSlActs().iterator();
			while (act_it.hasNext()) {
				Activity act = act_it.next();
				this.modifyLocation((ActivityImpl) act, subChain.getStartCoord(), subChain.getEndCoord(), Double.MAX_VALUE, 0);
			}
			return 0;
		}

		Coord startCoord = subChain.getStartCoord();
		Coord endCoord = subChain.getEndCoord();
		double ttBudget = subChain.getTtBudget();

		Activity prevAct = subChain.getFirstPrimAct();

		Iterator<Activity> act_it = subChain.getSlActs().iterator();
		while (act_it.hasNext()) {
			Activity act = act_it.next();
			double radius = (ttBudget * speed) / 2.0;
			if (!this.modifyLocation((ActivityImpl) act, startCoord, endCoord, radius, 0)) {
				return 1;
			}

			startCoord = act.getCoord();
			ttBudget -= this.computeTravelTime(person, prevAct, act);

			if (!act_it.hasNext()) {
				double tt2Anchor = this.computeTravelTime(person, act, subChain.getLastPrimAct());
				ttBudget -= tt2Anchor;
			}

			if (ttBudget < 0.0) {
				return -1;
			}
			prevAct = act;
		}
		return 0;
	}

	protected boolean modifyLocation(ActivityImpl act, Coord startCoord, Coord endCoord, double radius, int trialNr) {

		ArrayList<ActivityFacility> choiceSet = this.computeChoiceSetCircle(startCoord, endCoord, radius, act.getType());

		if (choiceSet.size()>1) {
			//final Facility facility=(Facility)choiceSet.toArray()[
           	//		           MatsimRandom.random.nextInt(choiceSet.size())];
			final ActivityFacility facility = choiceSet.get(super.random.nextInt(choiceSet.size()));

			act.setFacilityId(facility.getId());
       		act.setLinkId(NetworkUtils.getNearestLink(((NetworkImpl) this.scenario.getNetwork()), facility.getCoord()).getId());
       		act.setCoord(facility.getCoord());
       		return true;
		}
		// else ...
		return false;
	}

	protected double computeTravelTime(Person person, Activity fromAct, Activity toAct) {
		LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(0.0);
		leg.setArrivalTime(0.0);

		PlanRouterAdapter.handleLeg(router, person, leg, fromAct, toAct, fromAct.getEndTime());
		return leg.getTravelTime();
	}

	private List<SubChain> calcActChainsDefinedFixedTypes(final Plan plan) {
		ManageSubchains manager = new ManageSubchains();

		final List<?> actslegs = plan.getPlanElements();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final ActivityImpl act = (ActivityImpl)actslegs.get(j);

			if (super.defineFlexibleActivities.getFlexibleTypes().contains(this.defineFlexibleActivities.getConverter().convertType(act.getType()))) { // found secondary activity
				manager.secondaryActivityFound(act, (LegImpl)actslegs.get(j+1));
			}
			else {		// found primary activity
				if (j == (actslegs.size()-1)) {
					manager.primaryActivityFound(act, null);
				}
				else {
					manager.primaryActivityFound(act, (LegImpl)actslegs.get(j+1));
				}
			}
		}
		return manager.getSubChains();
	}

	public List<SubChain> calcActChains(final Plan plan) {
		return this.calcActChainsDefinedFixedTypes(plan);
	}

	public ArrayList<ActivityFacility>  computeChoiceSetCircle(Coord coordStart, Coord coordEnd,
			double radius, String type) {
		double midPointX = (coordStart.getX()+coordEnd.getX())/2.0;
		double midPointY = (coordStart.getY()+coordEnd.getY())/2.0;
		return (ArrayList<ActivityFacility>) this.quadTreesOfType.get(this.defineFlexibleActivities.getConverter().convertType(type)).
				get(midPointX, midPointY, radius);
	}

	// for test cases:
	public double getRecursionTravelSpeedChange() {
		return recursionTravelSpeedChange;
	}

	public void setRecursionTravelSpeedChange(double recursionTravelSpeedChange) {
		this.recursionTravelSpeedChange = recursionTravelSpeedChange;
	}

	public int getMaxRecursions() {
		return maxRecursions;
	}

	public void setMaxRecursions(int maxRecursions) {
		this.maxRecursions = maxRecursions;
	}
}
