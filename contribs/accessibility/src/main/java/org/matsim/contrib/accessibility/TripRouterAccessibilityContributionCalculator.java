/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.contrib.accessibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;

/**
 * @author nagel, dziemke
 */
public class TripRouterAccessibilityContributionCalculator implements AccessibilityContributionCalculator {

	private TripRouter tripRouter ;
	private String mode;
	private PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
	private Scenario scenario;

    Map<Id<Node>, ArrayList<ActivityFacility>> aggregatedMeasurePoints;
    Map<Id<Node>, AggregationObject> aggregatedOpportunities;


	public TripRouterAccessibilityContributionCalculator(String mode, TripRouter tripRouter, PlanCalcScoreConfigGroup planCalcScoreConfigGroup, Scenario scenario) {
		this.mode = mode;
		this.tripRouter = tripRouter;
		this.planCalcScoreConfigGroup = planCalcScoreConfigGroup;
		this.scenario = scenario;
	}


    @Override
    public void initialize(ActivityFacilities measuringPoints, ActivityFacilities opportunities) {
        this.aggregatedMeasurePoints = AccessibilityUtils.aggregateMeasurePointsWithSameNearestNode(measuringPoints, scenario.getNetwork());
        this.aggregatedOpportunities = AccessibilityUtils.aggregateOpportunitiesWithSameNearestNode(opportunities, scenario.getNetwork(), scenario.getConfig());
    }


	@Override
	public void notifyNewOriginNode(Node fromNode, Double departureTime) {
		// at this point, do nothing (inefficient)
	}


	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin, final AggregationObject destination, Double departureTime) {
		Person person = null ; // I think that this is ok

		ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();
		ActivityFacility destinationFacility = activityFacilitiesFactory.createActivityFacility(null, destination.getNearestNode().getCoord());

		Gbl.assertNotNull(tripRouter);
		List<? extends PlanElement> plan = tripRouter.calcRoute(mode, origin, destinationFacility, departureTime, person);

//		Vehicle vehicle = null ; // I think that this is ok
		double utility = 0.;
		List<Leg> legs = TripStructureUtils.getLegs(plan);
		// TODO Doing it like this, the pt interaction (e.g. waiting) times will be omitted!
		Gbl.assertIf(!legs.isEmpty());

		for (Leg leg : legs) {
			// Add up all utility components of leg
			utility += leg.getRoute().getDistance() * this.planCalcScoreConfigGroup.getModes().get(leg.getMode()).getMarginalUtilityOfDistance();
			utility += leg.getRoute().getTravelTime() * this.planCalcScoreConfigGroup.getModes().get(leg.getMode()).getMarginalUtilityOfTraveling() / 3600.;
			utility += -leg.getRoute().getTravelTime() * this.planCalcScoreConfigGroup.getPerforming_utils_hr() / 3600.;
		}

		// Utility based on opportunities that are attached to destination node
		double sumExpVjkWalk = destination.getSum();

		// exp(beta * a) * exp(beta * b) = exp(beta * (a+b))
		return Math.exp(this.planCalcScoreConfigGroup.getBrainExpBeta() * utility) * sumExpVjkWalk;
	}


	@Override
	public TripRouterAccessibilityContributionCalculator duplicate() {
		TripRouterAccessibilityContributionCalculator tripRouterAccessibilityContributionCalculator =
				new TripRouterAccessibilityContributionCalculator(this.mode, this.tripRouter, this.planCalcScoreConfigGroup, this.scenario);
		return tripRouterAccessibilityContributionCalculator;
	}


    @Override
    public Map<Id<Node>, ArrayList<ActivityFacility>> getAggregatedMeasurePoints() {
        return aggregatedMeasurePoints;
    }


    @Override
    public Map<Id<Node>, AggregationObject> getAgregatedOpportunities() {
        return aggregatedOpportunities;
    }
}