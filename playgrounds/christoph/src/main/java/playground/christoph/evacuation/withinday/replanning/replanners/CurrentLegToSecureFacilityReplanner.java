/* *********************************************************************** *
 * project: org.matsim.*
 * CurrentLegToSecureFacilityReplanner.java
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

package playground.christoph.evacuation.withinday.replanning.replanners;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.withinday.replanning.replanners.CurrentLegToSecureFacilityReplanner;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import playground.christoph.withinday.utils.EditRoutes;
import playground.christoph.withinday.utils.ReplacePlanElements;

/*
 * If an Agent performs a Leg in the secure area (or at least the
 * link where the Agent is at the moment) we adapt the Route as following:
 * - If the Agents plans to stop at the current Link anyway we only
 * 	 replace the planned Activity.
 * 
 * - Otherwise: We cannot force the Agent to stop at the current Link. The QSim
 *   may already have moved him to the buffer of the QLink/QLane which means the
 *   Agent is ready to be moved over the OutNode - no further check whether
 *   an Activity should be performed at the current Link will be done.
 *   Therefore we try to find the counter Link to the current Link - that Link
 *   should be also secure. If also no counter Link is available, we check whether
 *   one of the OutLinks of the next Node is secure and use that one.
 * 
 */
public class CurrentLegToSecureFacilityReplanner extends WithinDayDuringLegReplanner {

	/*package*/ CurrentLegToSecureFacilityReplanner(Id id, Scenario scenario) {
		super(id, scenario);
	}

	@Override
	public boolean doReplanning(PersonAgent personAgent) {
		
		// do Replanning only in the timestep where the Evacuation has started.
		if (this.time > EvacuationConfig.evacuationTime) return true;
		
		// If we don't have a valid Replanner.
		if (this.routeAlgo == null) return false;

		// If we don't have a valid WithinDayPersonAgent
		if (personAgent == null) return false;

		WithinDayPersonAgent withinDayPersonAgent = null;
		if (!(personAgent instanceof WithinDayPersonAgent)) return false;
		else {
			withinDayPersonAgent = (WithinDayPersonAgent) personAgent;
		}

		PersonImpl person = (PersonImpl)withinDayPersonAgent.getPerson();
		PlanImpl selectedPlan = (PlanImpl)person.getSelectedPlan();

		// If we don't have a selected plan
		if (selectedPlan == null) return false;

		Leg currentLeg = personAgent.getCurrentLeg();
		Activity nextActivity = selectedPlan.getNextActivity(currentLeg);
		
		// If it is not a car Leg we don't replan it.
//		if (!currentLeg.getMode().equals(TransportMode.car)) return false;

		// Get the current Link
		Link currentLink = scenario.getNetwork().getLinks().get(getCurrentLinkId(withinDayPersonAgent));
		
		Activity rescueActivity = null;
		/*
		 * If the Agents has planned an Activity at the current Link anyway
		 * we only have to replace that Activity.
		 */
		if (currentLeg.getRoute().getEndLinkId().equals(currentLink.getId())) {
			rescueActivity = nextActivityAtCurrentLink(currentLink, selectedPlan, nextActivity);
		}
		
		else {
			/*
			 * Get the CounterLink. It should also be in the secure area.
			 */
			Link nextLink = getNextLink(currentLink);
			
			/*
			 * Now we add a new Activity at the rescue facility.
			 */
			rescueActivity = scenario.getPopulation().getFactory().createActivityFromLinkId("secure", nextLink.getId());
			String idString = "secureFacility" + nextLink.getId();
			((ActivityImpl)rescueActivity).setFacilityId(scenario.createId(idString));
			rescueActivity.setEndTime(Time.UNDEFINED_TIME);
			
			Coord rescueCoord = ((ScenarioImpl)scenario).getActivityFacilities().getFacilities().get(scenario.createId(idString)).getCoord();
			((ActivityImpl)rescueActivity).setCoord(rescueCoord);
			
			new ReplacePlanElements().replaceActivity(selectedPlan, nextActivity, rescueActivity);
			
			// new Route for current Leg
			new EditRoutes().replanCurrentLegRoute(selectedPlan, currentLeg, withinDayPersonAgent.getCurrentNodeIndex(), routeAlgo, scenario.getNetwork(), time);
		}
		
		// Remove all legs and activities after the next activity.
		int nextActivityIndex = selectedPlan.getActLegIndex(rescueActivity);
		
		while (selectedPlan.getPlanElements().size() - 1 > nextActivityIndex) {
			selectedPlan.removeActivity(selectedPlan.getPlanElements().size() - 1);
		}
		
		// Finally reset the cached Values of the PersonAgent - they may have changed!
		withinDayPersonAgent.resetCaches();
		
		return true;
	}
	
	/*
	 * We try to get a CounterLink. If none is available we choose one of
	 * the OutLinks of the next Node.
	 */
	private Link getNextLink(Link currentLink) {
		/*
		 * Get the CounterLink. It should also be in the secure area.
		 */
		for (Link link : currentLink.getToNode().getOutLinks().values()) {
			if (link.getToNode().equals(currentLink.getFromNode())) {
				return link;
			}
		}
		
		/*
		 * We found no CounterLink - therefore we use one of the OutLinks, if
		 * it is also in the secure Area.
		 */
		Coord center = EvacuationConfig.centerCoord;
		for (Link outLink : currentLink.getToNode().getOutLinks().values()) {
			double distance = CoordUtils.calcDistance(center, outLink.getToNode().getCoord());
			
			if (distance >= EvacuationConfig.innerRadius) return outLink;
		}
		
		return null;
	}
	
	private Activity nextActivityAtCurrentLink(Link currentLink, Plan selectedPlan, Activity nextActivity) {
		Activity rescueActivity = scenario.getPopulation().getFactory().createActivityFromLinkId("secure", currentLink.getId());
		String idString = "secureFacility" + currentLink.getId();
		((ActivityImpl)rescueActivity).setFacilityId(scenario.createId(idString));
		rescueActivity.setEndTime(Time.UNDEFINED_TIME);
		
		Coord rescueCoord = ((ScenarioImpl)scenario).getActivityFacilities().getFacilities().get(scenario.createId(idString)).getCoord();
		((ActivityImpl)rescueActivity).setCoord(rescueCoord);

		new ReplacePlanElements().replaceActivity(selectedPlan, nextActivity, rescueActivity);
		
		return rescueActivity;
	}
	
	private Id getCurrentLinkId(WithinDayPersonAgent withinDayPersonAgent) {
		int currentNodeIndex = withinDayPersonAgent.getCurrentNodeIndex();
		
		Route route = withinDayPersonAgent.getCurrentLeg().getRoute();
		
		if (currentNodeIndex == 1) {
			return route.getStartLinkId();
		}
		else {
			if (route instanceof NetworkRoute) {
				List<Id> ids = ((NetworkRoute) route).getLinkIds();

				// If the current Link is the last Link we don't have to replan
				// our Route.
				if (ids.size() <= currentNodeIndex - 2) {
					return route.getEndLinkId();
				}
				else return ids.get(currentNodeIndex - 2);
			}
			else {
				return null;
			}
		}
	}
}