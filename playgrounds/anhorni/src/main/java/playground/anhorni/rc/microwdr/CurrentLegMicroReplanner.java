/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.anhorni.rc.microwdr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplanner;


public class CurrentLegMicroReplanner extends WithinDayDuringLegReplanner {

	private final TripRouter tripRouter;
	private static final Logger logger = Logger.getLogger(CurrentLegMicroReplanner.class);
	private Random random = new Random();
	private Controler controler;
	
	
	/*package*/ CurrentLegMicroReplanner(Id<WithinDayReplanner> id, Scenario scenario, InternalInterface internalInterface, TripRouter tripRouter, Controler controler) {
		super(id, scenario, internalInterface);
		this.tripRouter = tripRouter;
		this.controler = controler;
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {
		
		random.setSeed(Integer.parseInt(withinDayAgent.getId().toString()) * this.controler.getIterationNumber());

		Plan executedPlan = WithinDayAgentUtils.getModifiablePlan(withinDayAgent);

		// If we don't have an executed plan
		if (executedPlan == null) return false;
		
		PlanElement currentPlanElement = WithinDayAgentUtils.getCurrentPlanElement(withinDayAgent);
		if (!(currentPlanElement instanceof Leg)) return false;
		Leg currentLeg = (Leg) currentPlanElement;
		int currentLinkIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(withinDayAgent);

		// new Route for current Leg
		this.microRouteCurrentLegRoute(currentLeg, executedPlan.getPerson(), currentLinkIndex, this.time, 
				scenario.getNetwork(), tripRouter, random, executedPlan); 
		

		// Finally reset the cached Values of the PersonAgent - they may have changed!
		WithinDayAgentUtils.resetCaches(withinDayAgent);

		return true;
	}
		
	private boolean microRouteCurrentLegRoute(Leg leg, Person person, int currentLinkIndex, double time, 
			Network network, TripRouter tripRouter, Random random, Plan plan) {
		
		Route route = leg.getRoute();

		// if the route type is not supported (e.g., because it is a walking agent)
		if (!(route instanceof NetworkRoute)) return false;
		
		if (random.nextFloat() > 0.3) return false;  // only 30% replanners
		
		PersonImpl p = (PersonImpl)person;
		int legnr = plan.getPlanElements().indexOf(leg);
				
	//	logger.warn(legnr);
		
		if (p.getAge() == legnr) {
	//		logger.error("agent already replanned");
	//		return false; // agent has been replanned already
		} else {
	//		p.setAge(legnr);
		}
				
		NetworkRoute oldRoute = (NetworkRoute) route;

		/*
		 *  Get the Id of the current Link.
		 *  Create a List that contains all links of a route, including the Start- and EndLinks.
		 */
		List<Id<Link>> allLinkIds = getRouteLinkIds(oldRoute); // 	
		
		int links2go = allLinkIds.size() - (currentLinkIndex + 1); // 
		
		if (links2go >= 3) {			
			Id<Link> startLink = allLinkIds.get(0);
			Id<Link> endLink = allLinkIds.get(allLinkIds.size() - 1);
			
			// The linkIds of the new Route ---------------------------
			List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
			// start of the old route
			linkIds.addAll(allLinkIds.subList(1, currentLinkIndex)); // currentLinkIndex exclusive
			
			
			// micro-reroute part of route --------------------------- 
			Id<Link> currentLinkId = allLinkIds.get(currentLinkIndex); // link 1, currentLinkIndex = 1
			Link fromLink = network.getLinks().get(currentLinkId);
					
			int jump = random.nextInt(links2go - 2) + 2 ; // start with 2
			int toLinkIndex = (currentLinkIndex + jump); // jump over one link //
			Id<Link> toLinkId = allLinkIds.get(toLinkIndex); // 
			Link toLink = network.getLinks().get(toLinkId); // 
			
			Facility<ActivityFacility> fromFacility = new LinkWrapperFacility(fromLink);
			Facility<ActivityFacility> toFacility = new LinkWrapperFacility(toLink);
			
			List<? extends PlanElement> planElements = tripRouter.calcRoute(leg.getMode(), fromFacility, toFacility, time, person); // 
			
			if (planElements.size() != 1) {
				throw new RuntimeException("Expected a list of PlanElements containing exactly one element, " +
						"but the returned list contained " + planElements.size() + " elements."); 
			}		
			Leg newLeg = (Leg) planElements.get(0); 
			Route newRoute = newLeg.getRoute();
			linkIds.addAll(getRouteLinkIds(newRoute)); // currentLinkIndex => startLink
				
			// remainder of the old route --------------------------- 
			if (toLinkIndex + 1 < allLinkIds.size()-1) { // if route is not yet finished:
				linkIds.addAll(allLinkIds.subList(toLinkIndex + 1, allLinkIds.size() - 1)); 
			}
				
			// Overwrite old Route
			if (linkIds.size() > 2 && toLinkId.compareTo(fromLink.getId()) != 0) {
				List<Id<Link>> middleLinks = linkIds.subList(0, linkIds.size()); // to is exclusive
							
//				String str = oldRoute.toString();
//				int lo = oldRoute.getLinkIds().size();
								
				oldRoute.setLinkIds(startLink, middleLinks , endLink);
				
//				if (oldRoute.getLinkIds().size() != lo) {
//					logger.info(person.getId() + " :" + str + "\n" +
//							oldRoute.toString());
//				}
			} // else do not replace route
		}
		return true;
	}
		
	private List<Id<Link>> getRouteLinkIds(Route route) {
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();

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
	
	/*
	 * Wraps a Link into a Facility.
	 */
	private static class LinkWrapperFacility implements Facility<ActivityFacility> {
		
		private final Link wrapped;

		public LinkWrapperFacility(final Link toWrap) {
			wrapped = toWrap;
		}

		@Override
		public Coord getCoord() {
			return wrapped.getCoord();
		}

		@Override
		public Id<ActivityFacility> getId() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Id<Link> getLinkId() {
			return wrapped.getId();
		}

		@Override
		public String toString() {
			return "[LinkWrapperFacility: wrapped="+wrapped+"]";
		}
	}

}