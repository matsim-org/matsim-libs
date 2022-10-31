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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;

class RecursiveLocationMutator extends AbstractLocationMutator{

	//	private static final Logger log = LogManager.getLogger(LocationMutatorwChoiceSet.class);
	private int unsuccessfullLC = 0;
	private double recursionTravelSpeedChange = 0.1;
	private double recursionTravelSpeed = 30.0;
	private int maxRecursions = 10;
	private TripRouter router;
	private final TimeInterpretation timeInterpretation;

	public RecursiveLocationMutator(final Scenario scenario, TripRouter router, TimeInterpretation timeInterpretation,
			TreeMap<String, QuadTree<ActivityFacility>> quad_trees,
			TreeMap<String, ActivityFacilityImpl []> facilities_of_type, Random random) {
		super(scenario, quad_trees, facilities_of_type, random);
		this.recursionTravelSpeedChange = this.getDccg().getRecursionTravelSpeedChange();
		this.maxRecursions = this.getDccg().getMaxRecursions();
		this.recursionTravelSpeed = this.getDccg().getTravelSpeed_car();
		this.router = router;
		this.timeInterpretation = timeInterpretation;
	}

	public RecursiveLocationMutator(final Scenario scenario, TripRouter router, TimeInterpretation timeInterpretation, Random random) {
		super(scenario, random);
		this.recursionTravelSpeedChange = this.getDccg().getRecursionTravelSpeedChange();
		this.maxRecursions = this.getDccg().getMaxRecursions();
		this.recursionTravelSpeed = this.getDccg().getTravelSpeed_car();
		this.router = router;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public void run(final Plan plan){
		List<SubChain> subChains = this.calcActChains(plan);
		this.handleSubChains(plan, subChains);
		PopulationUtils.resetRoutes(plan );
	}

	public final int getNumberOfUnsuccessfull() {
		return this.unsuccessfullLC;
	}

	public final void resetUnsuccsessfull() {
		this.unsuccessfullLC = 0;
	}

	private void handleSubChains(final Plan plan, List<SubChain> subChains) {
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

	private int handleSubChain(Person person, SubChain subChain, double speed, int trialNr){
		if (trialNr > this.maxRecursions) {
			this.unsuccessfullLC += 1;

			Iterator<Activity> act_it = subChain.getSlActs().iterator();
			while (act_it.hasNext()) {
				Activity act = act_it.next();
				this.modifyLocation((Activity) act, subChain.getStartCoord(), subChain.getEndCoord(), Double.MAX_VALUE, 0);
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
			if (!this.modifyLocation((Activity) act, startCoord, endCoord, radius, 0)) {
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

	private boolean modifyLocation(Activity act, Coord startCoord, Coord endCoord, double radius, int trialNr) {

		ArrayList<ActivityFacility> choiceSet = this.computeChoiceSetCircle(startCoord, endCoord, radius, act.getType());

		if (choiceSet.size()>1) {
			//final Facility facility=(Facility)choiceSet.toArray()[
           	//		           MatsimRandom.random.nextInt(choiceSet.size())];
			final ActivityFacility facility = choiceSet.get( super.getRandom().nextInt(choiceSet.size() ) );

			act.setFacilityId(facility.getId());
       		act.setLinkId(NetworkUtils.getNearestLink(((Network) this.getScenario().getNetwork()), facility.getCoord() ).getId() );
       		act.setCoord(facility.getCoord());
       		return true;
		}
		// else ...
		return false;
	}

	private double computeTravelTime(Person person, Activity fromAct, Activity toAct) {
		Leg leg = PopulationUtils.createLeg(TransportMode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(0.0);
		leg.setTravelTime( 0.0 - leg.getDepartureTime().seconds());

		List<? extends PlanElement> trip = router.calcRoute(
					leg.getMode(),
			  FacilitiesUtils.toFacility( fromAct, null ),
			  FacilitiesUtils.toFacility( toAct, null ),
				fromAct.getEndTime().seconds(),
				person, new Attributes() );

		if ( trip.size() != 1 ) {
			throw new IllegalStateException( "This method can only be used with "+
					"routing modules returning single legs. Got the following trip "+
					"for mode "+ leg.getMode()+": "+trip );
		}

		Leg tripLeg = (Leg) trip.get( 0 );
		leg.setRoute( tripLeg.getRoute() );
		leg.setTravelTime(tripLeg.getTravelTime().seconds() );
		leg.setDepartureTime(tripLeg.getDepartureTime().seconds() );

		timeInterpretation.decideOnLegTravelTime( tripLeg );
		return leg.getTravelTime().seconds();
	}

	private List<SubChain> calcActChainsDefinedFixedTypes(final Plan plan) {
		ManageSubchains manager = new ManageSubchains();

		final List<?> actslegs = plan.getPlanElements();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Activity act = (Activity)actslegs.get(j);

			if ( super.getDefineFlexibleActivities().getFlexibleTypes().contains( act.getType() )) { // found secondary activity
				manager.secondaryActivityFound(act, (Leg)actslegs.get(j+1));
			}
			else {		// found primary activity
				if (j == (actslegs.size()-1)) {
					manager.primaryActivityFound(act, null);
				}
				else {
					manager.primaryActivityFound(act, (Leg)actslegs.get(j+1));
				}
			}
		}
		return manager.getSubChains();
	}

	final List<SubChain> calcActChains(final Plan plan) {
		return this.calcActChainsDefinedFixedTypes(plan);
	}

	private ArrayList<ActivityFacility>  computeChoiceSetCircle(Coord coordStart, Coord coordEnd,
			double radius, String type) {
		double midPointX = (coordStart.getX()+coordEnd.getX())/2.0;
		double midPointY = (coordStart.getY()+coordEnd.getY())/2.0;
		return (ArrayList<ActivityFacility>) this.getQuadTreesOfType().get( type ).
				getDisk(midPointX, midPointY, radius);
	}

	double getRecursionTravelSpeedChange() {
		return recursionTravelSpeedChange;
	}

	final int getMaxRecursions() {
		// (public for test cases)
		return maxRecursions;
	}

}
