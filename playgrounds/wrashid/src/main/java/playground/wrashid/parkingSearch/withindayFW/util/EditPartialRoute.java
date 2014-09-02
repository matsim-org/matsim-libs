/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.parkingSearch.withindayFW.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.withinday.utils.EditRoutes;


public class EditPartialRoute {

	private final Scenario sc;
	private final PlanAlgorithm planAlgorithm;
	private final EditRoutes er = new EditRoutes();

	public EditPartialRoute(Scenario sc, PlanAlgorithm planAlgorithm) {
		this.sc = sc;
		this.planAlgorithm = planAlgorithm;
	}

	/**
	 * for walk leg: just adapt, if needed.
	 * 
	 * 
	 * 
	 * 
	 * @param plan
	 * @param legPlanElementIndex
	 * @param planAlgorithm
	 * @return
	 */
	public boolean replanFutureCarLegRoute(Plan plan, int legPlanElementIndex) {
		// er.replanFutureLegRoute(plan, legPlanElementIndex, planAlgorithm);

		// if (true){
		// return true;
		// }

		if (plan == null)
			return false;
		if (planAlgorithm == null)
			return false;

		DebugLib.traceAgent(plan.getPerson().getId(), 2);

		Leg leg;
		PlanElement planElement = plan.getPlanElements().get(legPlanElementIndex);
		if (planElement instanceof Leg) {
			leg = (Leg) planElement;
		} else
			return false;

		// yy This will (obviously) fail if the plan does not have alternating
		// acts and legs. kai, nov'10
		Activity fromActivity = (Activity) plan.getPlanElements().get(legPlanElementIndex - 1);
		Activity toActivity = (Activity) plan.getPlanElements().get(legPlanElementIndex + 1);

		NetworkRoute oldRoute = (NetworkRoute) leg.getRoute();

		/*
		 * We create a new Plan which contains only the Leg that should be
		 * replanned and its previous and next Activities. By doing so the
		 * PlanAlgorithm will only change the Route of that Leg.
		 */
		/*
		 * Create a new Plan that contains only the Leg which should be
		 * replanned and run the PlanAlgorithm.
		 */

		Route newRoute = null;

		double distanceOldNewDestination = 0;

		if (oldRoute != null && getRouteSize(oldRoute) != 0) {
			Link oldOriginLink = getLink(oldRoute.getStartLinkId());
			Link newOriginLink = getLink(fromActivity.getLinkId());
			oldRoute.getLinkIds();

			if (!oldOriginLink.getId().equals(newOriginLink.getId())) {
				double distanceOldNewOriginal = GeneralLib.getDistance(oldOriginLink.getCoord(), newOriginLink.getCoord());

				truncateRouteStart(oldRoute, distanceOldNewOriginal * 2);
			}

			if (getRouteSize(oldRoute) == 0) {
				PlanImpl newPlan = new PlanImpl(plan.getPerson());
				newPlan.addActivity(fromActivity);
				newPlan.addLeg(leg);
				newPlan.addActivity(toActivity);
				planAlgorithm.run(newPlan);
				return true;
			}

			Link oldDestinationLink = getLink(oldRoute.getEndLinkId());
			Link newDestinationLink = getLink(toActivity.getLinkId());

			if (!oldDestinationLink.getId().equals(newDestinationLink.getId())) {
				double distanceOldNewOriginal = GeneralLib.getDistance(oldDestinationLink.getCoord(),
						newDestinationLink.getCoord());

				truncateRouteEnd(oldRoute, distanceOldNewOriginal * 2);
			}

			if (getRouteSize(oldRoute) == 0) {
				PlanImpl newPlan = new PlanImpl(plan.getPerson());
				newPlan.addActivity(fromActivity);
				newPlan.addLeg(leg);
				newPlan.addActivity(toActivity);
				planAlgorithm.run(newPlan);
			} else {
				if (!oldRoute.getStartLinkId().equals(newOriginLink.getId())) {
					NetworkRoute startRoute = getRoute(newOriginLink, getLink(oldRoute.getStartLinkId()));
					joinRoutes(startRoute, oldRoute, oldRoute);
				}

				if (!oldRoute.getEndLinkId().equals(newDestinationLink.getId())) {
					NetworkRoute endRoute = getRoute(getLink(oldRoute.getEndLinkId()), newDestinationLink);
					joinRoutes(oldRoute, endRoute, oldRoute);
				}
			}

		} else {
			PlanImpl newPlan = new PlanImpl(plan.getPerson());
			newPlan.addActivity(fromActivity);
			newPlan.addLeg(leg);
			newPlan.addActivity(toActivity);
			planAlgorithm.run(newPlan);
		}

		/*
		 * If possible, reuse existing route objects. Someone might already be
		 * using a reference to that object.
		 */
		newRoute = leg.getRoute();
		if (oldRoute != null && oldRoute != newRoute) {
			if (oldRoute instanceof NetworkRoute && newRoute instanceof NetworkRoute) {
				List<Id<Link>> linkIds = ((NetworkRoute) newRoute).getLinkIds();
				oldRoute.setLinkIds(newRoute.getStartLinkId(), linkIds, newRoute.getEndLinkId());
				leg.setRoute(oldRoute);
			}
		}

		return true;
	}

	private void truncateRouteStart(NetworkRoute networkRoute, double distance) {
		int cutIndex = -1;

		double sumSize = 0;
		for (int i = 0; i < networkRoute.getLinkIds().size(); i++) {
			Id linkId = networkRoute.getLinkIds().get(i);
			Link link = getLink(linkId);
			sumSize += link.getLength();

			if (sumSize > distance) {
				cutIndex = i;
				break;
			}
		}

		if (cutIndex == -1) {
			networkRoute.setLinkIds(null, null, null);
		} else {
			NetworkRoute subRoute = networkRoute
					.getSubRoute(networkRoute.getLinkIds().get(cutIndex), networkRoute.getEndLinkId());

			if (getRouteSize(subRoute) == 0) {
				networkRoute.setLinkIds(null, null, null);
			} else {
				networkRoute.setLinkIds(subRoute.getStartLinkId(), subRoute.getLinkIds(), subRoute.getEndLinkId());

				assert !networkRoute.getStartLinkId().equals(subRoute.getLinkIds().get(0));
				assert !networkRoute.getEndLinkId().equals(subRoute.getLinkIds().get(subRoute.getLinkIds().size() - 1));
			}

		}
	}

	private Link getLink(Id linkId) {
		Link link = this.sc.getNetwork().getLinks().get(linkId);
		return link;
	}

	private void truncateRouteEnd(NetworkRoute networkRoute, double distance) {
		int cutIndex = -1;

		double sumSize = 0;
		for (int i = networkRoute.getLinkIds().size() - 1; i >= 0; i--) {
			Id linkId = networkRoute.getLinkIds().get(i);
			Link link = this.sc.getNetwork().getLinks().get(linkId);
			sumSize += link.getLength();

			if (sumSize > distance) {
				cutIndex = i;
				break;
			}
		}

		if (cutIndex == -1) {
			networkRoute.setLinkIds(null, null, null);
		} else {
			NetworkRoute subRoute = networkRoute.getSubRoute(networkRoute.getStartLinkId(),
					networkRoute.getLinkIds().get(cutIndex));

			if (getRouteSize(subRoute) == 0) {
				networkRoute.setLinkIds(null, null, null);
			} else {
				networkRoute.setLinkIds(subRoute.getStartLinkId(), subRoute.getLinkIds(), subRoute.getEndLinkId());

				assert !networkRoute.getStartLinkId().equals(subRoute.getLinkIds().get(0));
				assert !networkRoute.getEndLinkId().equals(subRoute.getLinkIds().get(subRoute.getLinkIds().size() - 1));
			}

		}
	}

	private void joinRoutes(NetworkRoute routeStart, NetworkRoute routeEnd, NetworkRoute targetRoute) {
		if (routeStart.getEndLinkId().equals(routeEnd.getStartLinkId())) {

			LinkedList<Id<Link>> linkIds = new LinkedList<Id<Link>>();

			linkIds.addAll(routeStart.getLinkIds());

			linkIds.add(routeStart.getEndLinkId());

			if (routeEnd.getLinkIds().size() > 0) {
				for (int i = 0; i < routeEnd.getLinkIds().size(); i++) {
					linkIds.add(routeEnd.getLinkIds().get(i));
				}
			}

			targetRoute.setLinkIds(routeStart.getStartLinkId(), linkIds, routeEnd.getEndLinkId());

		} else {
			DebugLib.stopSystemAndReportInconsistency();
		}
	}

	// TODO: perhaps reuse same dummy leg, etc. to make things more efficient?
	private NetworkRoute getRoute(Link fromLink, Link toLink) {
		PlanImpl newPlan = new PlanImpl();
		ActivityImpl fromActivity = new ActivityImpl("", fromLink.getId());
		ActivityImpl toActivity = new ActivityImpl("", toLink.getId());
		LegImpl leg = new LegImpl(TransportMode.car);
		fromActivity.setEndTime(0);
		toActivity.setEndTime(0);

		newPlan.addActivity(fromActivity);
		newPlan.addLeg(leg);
		newPlan.addActivity(toActivity);
		
		
		planAlgorithm.run(newPlan);

		return (NetworkRoute) leg.getRoute();
	}

	private int getRouteSize(NetworkRoute route) {
		if (route == null) {
			DebugLib.emptyFunctionForSettingBreakPoint();
		}

		return route.getLinkIds().size();
	}

	public boolean replanCurrentCarLegRoute(Plan plan, int legPlanElementIndex, int currentLinkIndex, double time) {
		if (legPlanElementIndex==3){
		//	DebugLib.traceAgent(plan.getPerson().getId(), 9);
		}

		if (plan == null)
			return false;
		if (planAlgorithm == null)
			return false;

		Leg leg;
		PlanElement planElement = plan.getPlanElements().get(legPlanElementIndex);
		if (planElement instanceof Leg) {
			leg = (Leg) planElement;
		} else
			return false;

		Activity fromActivity = (Activity) plan.getPlanElements().get(legPlanElementIndex - 1);
		Activity toActivity = (Activity) plan.getPlanElements().get(legPlanElementIndex + 1);

		Route route = leg.getRoute();

		// if the route type is not supported (e.g. because it is a walking
		// agent)
		if (!(route instanceof NetworkRoute))
			return false;

		NetworkRoute oldRoute = (NetworkRoute) route;

		Link oldDestinationLink = getLink(oldRoute.getEndLinkId());
		Link newDestinationLink = getLink(toActivity.getLinkId());

		List<Id<Link>> routeLinkIds = getRouteLinkIds(oldRoute);
		
		//routeShouldEndAtCurrentLink
		if (routeLinkIds.get(currentLinkIndex).equals(newDestinationLink.getId())){
			List<Id<Link>> subList = routeLinkIds.subList(1, currentLinkIndex);
			
			oldRoute.setLinkIds(routeLinkIds.get(0), subList, routeLinkIds.get(currentLinkIndex));
			
			return true;
		}
		
		
		
		if (!oldDestinationLink.getId().equals(newDestinationLink.getId())) {
			double distanceOldNewOriginal = GeneralLib.getDistance(oldDestinationLink.getCoord(), newDestinationLink.getCoord());

			truncateRouteEndCurrentRoute(oldRoute, distanceOldNewOriginal * 2, currentLinkIndex);
			
			NetworkRoute endRoute = getRoute(getLink(oldRoute.getEndLinkId()), newDestinationLink);
			joinRoutes(oldRoute, endRoute, oldRoute);
		} 
		

		
		

		//er.replanCurrentLegRoute(plan, legPlanElementIndex, currentLinkIndex, planAlgorithm, time);

		return true;
	}

	private void truncateRouteEndCurrentRoute(NetworkRoute networkRoute, double distance, int currentLinkIndex) {
		int cutIndex = -1;

		double sumSize = 0;
		List<Id<Link>> linkIds = getRouteLinkIds(networkRoute);
		for (int i = linkIds.size() - 1; i >= currentLinkIndex; i--) {
			Id linkId = linkIds.get(i);
			Link link = this.sc.getNetwork().getLinks().get(linkId);
			sumSize += link.getLength();

			if (sumSize > distance) {
				cutIndex = i;
				break;
			}
		}

		//NetworkRoute subRoute = null;
		List<Id<Link>> subRouteList=null;
		
		if (cutIndex == -1 || cutIndex<currentLinkIndex) {
			subRouteList = getSubRoute(linkIds,0,currentLinkIndex+1);
			//subRoute = networkRoute.getSubRoute(networkRoute.getStartLinkId(), linkIds.get(currentLinkIndex-1));
		} else {
			subRouteList = getSubRoute(linkIds,0,cutIndex+1);
			//subRoute = networkRoute.getSubRoute(networkRoute.getStartLinkId(), linkIds.get(cutIndex));

		}
		
		//networkRoute.setLinkIds(subRouteList.getStartLinkId(), subRoute.getLinkIds(), subRoute.getEndLinkId());
		
		if (subRouteList.size()==1){
			networkRoute.setLinkIds(subRouteList.get(0), null , subRouteList.get(0));
		} else if (subRouteList.size()==2){
			networkRoute.setLinkIds(subRouteList.get(0), null , subRouteList.get(1));
		} else {
			networkRoute.setLinkIds(subRouteList.get(0), subRouteList.subList(1, subRouteList.size()-1) , subRouteList.get(subRouteList.size()-1));
		}
		
	}
	
	public List<Id<Link>> getSubRoute(List<Id<Link>> linkIds, int startIndex, int endIndex){
		return linkIds.subList(startIndex, endIndex);
	}
	
	private List<Id<Link>> getRouteLinkIds(Route route) {
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();

		if (route instanceof NetworkRoute) {
			NetworkRoute networkRoute = (NetworkRoute) route;
			linkIds.add(networkRoute.getStartLinkId());
			linkIds.addAll(networkRoute.getLinkIds());
			linkIds.add(networkRoute.getEndLinkId());
		} else {
			DebugLib.stopSystemAndReportInconsistency("Currently only NetworkRoutes are supported for Within-Day Replanning!");
		}

		return linkIds;
	}

}
