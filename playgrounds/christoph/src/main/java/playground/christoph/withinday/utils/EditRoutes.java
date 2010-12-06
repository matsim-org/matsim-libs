/* *********************************************************************** *
 * project: org.matsim.*
 * EditRoutes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.population.algorithms.PlanAlgorithm;

public class EditRoutes {

	private static final Logger logger = Logger.getLogger(EditRoutes.class);
	
	/**Re-plans a future route between two activities.  The route is given by its leg, which is given by the planElementIndex.
	 * <p/>
	 * The leg needs to be preceded and followed by activities in order for this method to work.  This is not as strong a 
	 * requirement as one may think, since pt plans also need to be stripped down to the "real" activities before routing starts.
	 * <p/> 
	 * @param plan the plan containing the leg/route to be re-planned
	 * @param legPlanElementIndex the index for the leg containing the route to be re-planned
	 * @param planAlgorithm an algorithm that fulfills the PlanAlgorithm interface, but needs to be a router for this method to make sense
	 * @return true when replacing the route worked, false when something went wrong
	 */
	public boolean replanFutureLegRoute(Plan plan, int legPlanElementIndex, PlanAlgorithm planAlgorithm) {
		
		if (plan == null) return false;
		if (planAlgorithm == null) return false; 
		
		Leg leg;
		PlanElement planElement = plan.getPlanElements().get(legPlanElementIndex);
		if (planElement instanceof Leg) {
			leg = (Leg) planElement;
		} else return false;

		// yy This will (obviously) fail if the plan does not have alternating acts and legs.  kai, nov'10
		Activity fromActivity = (Activity) plan.getPlanElements().get(legPlanElementIndex - 1);
		Activity toActivity = (Activity) plan.getPlanElements().get(legPlanElementIndex + 1);
		
		Route oldRoute = leg.getRoute();
		
		// if the route type is not supported (e.g. because it is a walking agent)
		if (!(oldRoute instanceof NetworkRoute)) return false;
		
		if (oldRoute != null) {
			// Update the startLinkId if it has changed.
			if (!fromActivity.getLinkId().equals(oldRoute.getStartLinkId())) {
				if (oldRoute instanceof RouteWRefs) {
					((RouteWRefs) oldRoute).setStartLinkId(fromActivity.getLinkId());
				}
				else {
					logger.warn("Could not update the StartLinkId of the Route! Route was not replanned!");
					return false;
				}
			}
			
			// Update the endLinkId if it has changed.
			if (!toActivity.getLinkId().equals(oldRoute.getEndLinkId())) {
				if (oldRoute instanceof RouteWRefs) {
					((RouteWRefs) oldRoute).setEndLinkId(toActivity.getLinkId());
				}
				else {
					logger.warn("Could not update the EndLinkId of the Route! Route was not replanned!");
					return false;
				}
			}		
		}
		
		/*
		 * We create a new Plan which contains only the Leg
		 * that should be replanned and its previous and next
		 * Activities. By doing so the PlanAlgorithm will only
		 * change the Route of that Leg.
		 */
		/*
		 *  Create a new Plan that contains only the Leg
		 *  which should be replanned and run the PlanAlgorithm.
		 */
		PlanImpl newPlan = new PlanImpl(plan.getPerson());
		newPlan.addActivity(fromActivity);
		newPlan.addLeg(leg);
		newPlan.addActivity(toActivity);
		planAlgorithm.run(newPlan);
		
		Route newRoute = leg.getRoute();

		if (oldRoute != null) {
			// If the Route Object was replaced...
			if (oldRoute != newRoute) {
				if (oldRoute instanceof NetworkRoute) {
					List<Id> linkIds = ((NetworkRoute) newRoute).getLinkIds();
					((NetworkRoute) oldRoute).setLinkIds(newRoute.getStartLinkId(), linkIds, newRoute.getEndLinkId());
					leg.setRoute(oldRoute);
				}
				else {
					logger.warn("A new Route Object was created. The Route data could not be copied to the old Route. Cached Referenced to the old Route may cause Problems!");
				}			
			}		
		}
		
		return true;
	}
	
	/*
	 * We create a new Plan which contains only the Leg
	 * that should be replanned and its previous and next
	 * Activities. By doing so the PlanAlgorithm will only
	 * change the Route of that Leg.
	 * 
	 * Use currentNodeIndex from a DriverAgent if possible!
	 * 
	 * Otherwise code it as following:
	 * startLink - Node1 - routeLink1 - Node2 - routeLink2 - Node3 - endLink
	 * The currentNodeIndex has to Point to the next Node
	 * (which is the endNode of the current Link)
	 */
	public boolean replanCurrentLegRoute(Plan plan, int legPlanElementIndex, int currentLinkIndex, PlanAlgorithm planAlgorithm, Network network, double time) {
		if (plan == null) return false;
		if (planAlgorithm == null) return false; 

		Leg leg;
		PlanElement planElement = plan.getPlanElements().get(legPlanElementIndex);
		if (planElement instanceof Leg) {
			leg = (Leg) planElement;
		} else return false;

		
		// yyyy I can't say how safe this is.  There is no guarantee that the same entry is not used twice in the plan.  This will in
		// particular be a problem if we override the "equals" contract, in the sense that two activities are equal if
		// certain (or all) elements are equal.  kai, oct'10
		// using index now - should be save... cdobler, nov'10
		
		Activity fromActivity = (Activity) plan.getPlanElements().get(legPlanElementIndex - 1);
		Activity toActivity = (Activity) plan.getPlanElements().get(legPlanElementIndex + 1);

		Route oldRoute = leg.getRoute();
		
		// if the route type is not supported (e.g. because it is a walking agent)
		if (!(oldRoute instanceof NetworkRoute)) return false;
		
		/*
		 *  Get the Id of the current Link.
		 *  Create a List that contains all links of a route, including the Start- and EndLinks.
		 */
		List<Id> allLinkIds = getRouteLinkIds(oldRoute);
		Id currentLinkId = allLinkIds.get(currentLinkIndex);

		/*
		 *  Create a new Plan with one Leg that leeds from the
		 *  current Position to the destination Activity.
		 */
		Activity newFromActivity = new ActivityImpl(fromActivity.getType(), currentLinkId);
		newFromActivity.setStartTime(time);
		newFromActivity.setEndTime(time);
		
		// The linkIds of the new Route
		List<Id> linkIds = new ArrayList<Id>();
		
		/*
		 * Get those Links which have already been passed.
		 * allLinkIds contains also the startLinkId, which should not
		 * be part of the List - it is set separately. Therefore we start
		 * at index 1.
		 */
		if (currentLinkIndex > 0) {
			linkIds.addAll(allLinkIds.subList(1, currentLinkIndex + 1));
		}
		
		// Create a new leg and use the subRoute.
		Leg newLeg = new LegImpl((LegImpl) leg);
		newLeg.setDepartureTime(time);
//		newLeg.setRoute(subRoute);
		
		/*
		 *  Create a new Plan that contains only the Leg
		 *  which should be replanned and run the PlanAlgorithm.
		 */
		PlanImpl newPlan = new PlanImpl(plan.getPerson());
		newPlan.addActivity(newFromActivity);
		newPlan.addLeg(newLeg);
		newPlan.addActivity(toActivity);
		planAlgorithm.run(newPlan);
		
		Route newRoute = newLeg.getRoute();
		
		// Merge old and new Route.
		if (newRoute instanceof NetworkRoute) {
			/*
			 * Edit cdobler 25.5.2010
			 * If the new leg ends at the current Link, we have to
			 * remove that linkId from the linkIds List - it is stored
			 * in the endLinkId field of the route.
			 */
			if (linkIds.size() > 0 && linkIds.get(linkIds.size() - 1).equals(newRoute.getEndLinkId())) {
				linkIds.remove(linkIds.size() - 1);
			}
			
			linkIds.addAll(((NetworkRoute) newRoute).getLinkIds());
		}
		else {
			logger.warn("The Route data could not be copied to the old Route. Old Route will be used!");
			return false;
		}
		
		// Overwrite old Route
		if (oldRoute instanceof NetworkRoute) {
			((NetworkRoute) oldRoute).setLinkIds(oldRoute.getStartLinkId(), linkIds, toActivity.getLinkId());
		}
		else {
			logger.warn("The new Route data could not be copied to the old Route. Old Route will be used!");
			return false;
		}	
		
		return true;
	}
	
	private List<Id> getRouteLinkIds(Route route) {
		List<Id> linkIds = new ArrayList<Id>();
		
		if (route instanceof NetworkRoute) {
			NetworkRoute networkRoute = (NetworkRoute) route;
			linkIds.add(networkRoute.getStartLinkId());
			linkIds.addAll(networkRoute.getLinkIds());
			linkIds.add(networkRoute.getEndLinkId());
		} else {
			throw new RuntimeException("Currently only NetworkRoutes are supported for Within-Day Replanning!");
		}
		
		return linkIds;
	}
}
