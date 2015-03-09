/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingSearchReplanner.java
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

package playground.christoph.parking.withinday.replanner;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;

import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.christoph.parking.withinday.replanner.strategy.NearestAvailableParkingSearch;
import playground.christoph.parking.withinday.replanner.strategy.ParkingSearchStrategy;
import playground.christoph.parking.withinday.replanner.strategy.RandomParkingSearch;
import playground.christoph.parking.withinday.utils.ParkingAgentsTracker;
import playground.christoph.parking.withinday.utils.ParkingRouter;

/**
 * Applies a parking search strategy to an agent.
 * The strategy itself is implemented in an external class that implements
 * the ParkingSearchStrategy interface.
 * 
 * This class ensures that the agent's plan is valid after applying the
 * parking search (e.g. relocates activities and updates routes).
 * 
 * @author cdobler
 */
public class ParkingSearchReplanner extends WithinDayDuringLegReplanner {

	private static final Logger log = Logger.getLogger(ParkingSearchReplanner.class);
	
	protected final ParkingAgentsTracker parkingAgentsTracker;
	private final ParkingInfrastructure parkingInfrastructure;
	private final ParkingRouter parkingRouter;
	protected final ParkingSearchStrategy randomParkingSearch;
	protected final ParkingSearchStrategy nearestAvailableParkingSearch;
	
	protected ParkingSearchReplanner(Id id, Scenario scenario, InternalInterface internalInterface, 
			ParkingAgentsTracker parkingAgentsTracker, ParkingInfrastructure parkingInfrastructure, ParkingRouter parkingRouter) {
		super(id, scenario, internalInterface);
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.parkingInfrastructure = parkingInfrastructure;
		this.parkingRouter = parkingRouter;

		this.randomParkingSearch = new RandomParkingSearch(scenario.getNetwork());
		this.nearestAvailableParkingSearch = new NearestAvailableParkingSearch(scenario.getNetwork(), parkingRouter, parkingInfrastructure);
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {

		/*
		 * If a parking has been selected, set it as destination and adapt the
		 * current leg, the next activity and the next leg. Otherwise just add
		 * another link to the current leg.
		 */
		Id parkingFacilityId = this.parkingAgentsTracker.getSelectedParking(withinDayAgent.getId());
		
		// if no parking facility has been set, the agent has to continue its search
		if (parkingFacilityId == null) {
//			this.randomParkingSearch.applySearchStrategy(withinDayAgent, this.time);
			this.nearestAvailableParkingSearch.applySearchStrategy(withinDayAgent, time);
			
//			if (withinDayAgent.getId().hashCode() % 2 == 0) {
//				this.randomParkingSearch.applySearchStrategy(withinDayAgent, this.time);
//			} else {
//				this.nearestAvailableParkingSearch.applySearchStrategy(withinDayAgent, time);
//			}
		}

		/*
		 * The agent is going to stop its trip on the current link since a parking is available
		 * and has been accepted. Now ensure that the agent's plan is still valid.
		 */
		else {
			Leg leg = WithinDayAgentUtils.getModifiableCurrentLeg(withinDayAgent);

			int routeIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(withinDayAgent);

			Route route = leg.getRoute();
			
			updateAgentsPlan(withinDayAgent, parkingFacilityId, route, routeIndex);
		}

		WithinDayAgentUtils.resetCaches(withinDayAgent);
		return true;
	}
	
	protected void updateAgentsPlan(MobsimAgent withinDayAgent, Id parkingFacilityId, Route route, int routeIndex) {
		
		Plan plan = ((PlanAgent) withinDayAgent).getCurrentPlan();
		Person person = plan.getPerson();
		Id currentLinkId = withinDayAgent.getCurrentLinkId();
		
		ActivityImpl parkingActivity = (ActivityImpl) ((PlanAgent) withinDayAgent).getNextPlanElement();
		ActivityFacility parkingFacility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(parkingFacilityId);
		
		int currentPlanElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(withinDayAgent);
		
		// check whether the walk leg from the parking to the actual facility has to be updated
		boolean parkingFacilityWasChanged = false;
		boolean parkingLinkWasChanged = false;

		if (currentLinkId != parkingActivity.getLinkId()) {
			parkingLinkWasChanged = true;
			parkingFacilityWasChanged = true;
		} else if (parkingFacilityId != parkingActivity.getFacilityId()) parkingFacilityWasChanged = true;
		
		/*
		 * If the parking was relocated to another link
		 */
		if (parkingLinkWasChanged) {
			
			// relocate parking activity to its new location
			relocateParkingActivity(parkingActivity, parkingFacility);
			
			// remove parts of the route that have not been passed by the agent yet
			cutRoute((NetworkRoute) route, routeIndex);
			
			// extend route if it does not end at the agent's selected parking facility
			if (!withinDayAgent.getCurrentLinkId().equals(parkingActivity.getLinkId())) {
				Vehicle vehicle = null;
				this.parkingRouter.extendCarRoute((NetworkRoute) route, parkingActivity.getLinkId(), time, person, vehicle);				
			}
			
			Leg walkLegToNextActivity = (Leg) plan.getPlanElements().get(currentPlanElementIndex + 2);
			Activity activityAfterWalkLeg = (Activity) plan.getPlanElements().get(currentPlanElementIndex + 3);
			
			Vehicle vehicle = null;
			this.parkingRouter.adaptStartOfWalkRoute(parkingActivity, walkLegToNextActivity, activityAfterWalkLeg, 
					walkLegToNextActivity.getDepartureTime(), person, vehicle);
		}
		// if only the parking facility changed but not the link where it is located
		else if (parkingFacilityWasChanged) {
//			relocateParkingActivity(parkingActivity, parkingFacility);
			
			// relocate parking activity to its new location
			relocateParkingActivity(parkingActivity, parkingFacility);
			
			// remove parts of the route that have not been passed by the agent yet
			cutRoute((NetworkRoute) route, routeIndex);
		}
		// parking was not change, therefore we do not have to adapt anything in the agent's plan
		else {
//			cutRoute(route, routeIndex);
			return;
		}
		
		/*
		 * Check whether there are further car legs. If yes, the location of the
		 * next parking activity is updated.
		 * Then it is checked whether the next car leg ends on the same link as it starts.
		 * If it does, it is removed.
		 * When a car leg is removed, the check has to be performed again.
		 */
		boolean removed = false;
		do {
			removed = checkNextCarLeg(parkingFacility, plan, currentPlanElementIndex, parkingFacilityWasChanged, parkingLinkWasChanged);
			if (removed) {
				log.info("Agent " + withinDayAgent.getId() + ": Removed a car leg that started and ended on the same link.");
			}
		} while (removed == true);
	}
	
	private void relocateParkingActivity(ActivityImpl parkingActivity, ActivityFacility facility) {
		
		parkingActivity.setCoord(facility.getCoord());
		parkingActivity.setLinkId(facility.getLinkId());
		parkingActivity.setFacilityId(facility.getId());
	}

	/*
	 * If the agent is not at the end of its route, remove not passed parts.
	 */
	private void cutRoute(NetworkRoute route, int routeIndex) {

		int length = route.getLinkIds().size();
		if (length >= routeIndex) {
			List<Id<Link>> linkIds = new ArrayList<Id<Link>>(route.getLinkIds().subList(0, routeIndex - 1));
			route.setLinkIds(route.getStartLinkId(), linkIds, route.getLinkIds().get(routeIndex - 1));
		}
	}
	
	// returns true, if the next car leg has been removed
	private boolean checkNextCarLeg(ActivityFacility parkingFacility, Plan plan, int currentPlanElementIndex, 
			boolean parkingFacilityWasChanged, boolean parkingLinkWasChanged) {

		// check whether there are further car legs after the current one
		int nextCarLegIndex = -1;
		for (int i = currentPlanElementIndex + 1; i < plan.getPlanElements().size(); i++) {
			
			PlanElement planElement = plan.getPlanElements().get(i);
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (leg.getMode().equals(TransportMode.car)) {
					nextCarLegIndex = i;
					break;
				}
			}
		}
		
		if (nextCarLegIndex > 0) {
			Leg walkLegToNextParkingActivity = (Leg) plan.getPlanElements().get(nextCarLegIndex - 2);
			ActivityImpl nextParkingActivity = (ActivityImpl) plan.getPlanElements().get(nextCarLegIndex - 1);
			Leg nextCarLeg = (Leg) plan.getPlanElements().get(nextCarLegIndex);
			
			/*
			 * If the parking was relocated to another link
			 */
			if (parkingLinkWasChanged) {
				relocateParkingActivity(nextParkingActivity, parkingFacility);

				Vehicle vehicle;
				
				vehicle = null;
				Activity fromActivity = (Activity) plan.getPlanElements().get(nextCarLegIndex - 3);
				this.parkingRouter.adaptEndOfWalkRoute(fromActivity, walkLegToNextParkingActivity, nextParkingActivity,
						walkLegToNextParkingActivity.getDepartureTime(), plan.getPerson(), vehicle);
				
				vehicle = null;
				this.parkingRouter.adaptStartOfCarRoute((NetworkRoute) nextCarLeg.getRoute(), parkingFacility.getLinkId(), 
						nextCarLeg.getDepartureTime(), plan.getPerson(), vehicle);
			}
			// if only the parking facility changed but not the link where it is located
			else if (parkingFacilityWasChanged) {
				relocateParkingActivity(nextParkingActivity, parkingFacility);
			} 
			// If the parking facility was not relocated, we do not have to adapt anything here.
			else return false;
			
			return false;
			
		} else return false;
	}
}