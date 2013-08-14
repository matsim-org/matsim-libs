/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.fullParkingStrategies;

import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.SortableMapObject;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;

import playground.christoph.parking.withinday.utils.ParkingAgentsTracker;
import playground.christoph.parking.withinday.utils.ParkingRouter;
import playground.wrashid.parkingSearch.withinDay_v_STRC.core.mobsim.ParkingInfrastructure_v2;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingActivityAttributes;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingScoreEvaluator;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingScoreManager;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.FullParkingSearchStrategy;
import playground.wrashid.parkingSearch.withinDay_v_STRC.util.ParkingAgentsTracker_v2;
import playground.wrashid.parkingSearch.withindayFW.util.ActivityDurationEstimator;

public class OptimalParkingStrategy implements FullParkingSearchStrategy {

	private ParkingRouter parkingRouter;
	private ScenarioImpl scenarioData;
	private ParkingAgentsTracker parkingAgentsTracker;
	private ParkingInfrastructure_v2 parkInfrastructure_v2;
	private WithinDayAgentUtils withinDayAgentUtils;

	public OptimalParkingStrategy(ParkingRouter parkingRouter, ScenarioImpl scenarioData,
			ParkingAgentsTracker parkingAgentsTracker, ParkingInfrastructure_v2 parkInfrastructure_v2) {
		this.parkingRouter = parkingRouter;
		this.scenarioData = scenarioData;
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.parkInfrastructure_v2 = parkInfrastructure_v2;
		this.withinDayAgentUtils = new WithinDayAgentUtils();
	}

	@Override
	public void applySearchStrategy(MobsimAgent agent, double time) {
		
		
		
		Id currentLinkId = agent.getCurrentLinkId();

		Leg leg = this.withinDayAgentUtils.getCurrentLeg(agent);

		NetworkRoute route = (NetworkRoute) leg.getRoute();

		Id parkingFacilityId = parkingAgentsTracker.getSelectedParking(agent.getId());
		Id parkingFacilityLinkId = parkInfrastructure_v2.getParkingFacilityLinkId(parkingFacilityId);

		parkingRouter.adaptEndOfRoute(route, parkingFacilityLinkId, time, this.withinDayAgentUtils.getSelectedPlan(agent).getPerson(), null,
				TransportMode.car);
	}

	@Override
	public boolean acceptParking(MobsimAgent agent, Id facilityId) {
		return false;
	}

	@Override
	public String getStrategyName() {
		// TODO Auto-generated method stub
		return "OptimalParkingStrategy";
	}

	public Id reserveBestParking(MobsimAgent agent, ParkingAgentsTracker_v2 parkingAgentsTracker, double time) {
		ParkingInfrastructure_v2 parkingInfrastructure = parkingAgentsTracker.getInfrastructure_v2();

		Id personId = agent.getId();
		Plan selectedPlan = this.withinDayAgentUtils.getSelectedPlan(agent);
		List<PlanElement> planElements = selectedPlan.getPlanElements();
		Integer currentPlanElementIndex = this.withinDayAgentUtils.getCurrentPlanElementIndex(agent);

		// get all parking within 1000m (of destination) or at least on parking,
		// if that set is empty.
		Activity nextNonParkingAct = (Activity) selectedPlan.getPlanElements().get(currentPlanElementIndex + 3);

		Collection<ActivityFacility> parkings = parkingInfrastructure.getAllFreeParkingWithinDistance(1000,
				nextNonParkingAct.getCoord());
		if (parkings.size() == 0) {
			parkings.add(parkingInfrastructure.getClosestFreeParkingFacility(nextNonParkingAct.getCoord()));
		}

		// get best parking
		PriorityQueue<SortableMapObject<ActivityFacility>> priorityQueue = new PriorityQueue<SortableMapObject<ActivityFacility>>();

		for (ActivityFacility parkingFacility : parkings) {
			double walkingDistance = GeneralLib.getDistance(parkingFacility.getCoord(), nextNonParkingAct.getCoord());

			double parkingDuration;
			if (isLastParkingOfDay(agent)) {
				parkingDuration = ActivityDurationEstimator.estimateActivityDurationLastParkingOfDay(time,
						((ParkingAgentsTracker_v2) parkingAgentsTracker).getFirstCarDepartureTimeOfDay().get(personId));
			} else {
				parkingDuration = ActivityDurationEstimator.estimateActivityDurationParkingDuringDay(time, planElements,
						currentPlanElementIndex);
			}

			double activityDuration = ActivityDurationEstimator.estimateActivityDurationParkingDuringDay(time, planElements,
					currentPlanElementIndex);
			ParkingScoreEvaluator parkingScoreEvaluator = ((ParkingScoreManager) parkingAgentsTracker).getParkingScoreEvaluator();

			ParkingActivityAttributes parkingActivityAttributes = new ParkingActivityAttributes(personId, parkingFacility.getId(),
					time, parkingDuration, activityDuration, 0, GeneralLib.getWalkingTravelDuration(walkingDistance),
					GeneralLib.getWalkingTravelDuration(walkingDistance));
			double parkingScore = parkingScoreEvaluator.getParkingScore(parkingActivityAttributes);

			priorityQueue.add(new SortableMapObject<ActivityFacility>(parkingFacility, parkingScore));
		}

		ActivityFacility bestParkingFacility = priorityQueue.poll().getKey();

		Id facilityId = bestParkingFacility.getId();

		return facilityId;
	}

	private boolean isLastParkingOfDay(MobsimAgent agent) {
		List<PlanElement> planElements = this.withinDayAgentUtils.getSelectedPlan(agent).getPlanElements();
		Integer currentPlanElementIndex = this.withinDayAgentUtils.getCurrentPlanElementIndex(agent);

		for (int i = planElements.size() - 1; i > 0; i--) {
			if (planElements.get(i) instanceof Leg) {
				Leg leg = (Leg) planElements.get(i);

				if (leg.getMode().equals(TransportMode.car) && i == currentPlanElementIndex) {
					return true;
				} else {
					return false;
				}
			}
		}

		DebugLib.stopSystemAndReportInconsistency("assumption broken");

		return false;
	}

}
