/* *********************************************************************** *
 * project: org.matsim.*
 * EquilRandomRouter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jjoubert.coord3D;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.replanning.ReplanningContext;

import com.google.inject.Inject;

/**
 *
 * @author jwjoubert
 */
public class EquilRandomRouterModule implements PlanStrategyModule {

	private final Scenario sc;
	
	@Inject
	public EquilRandomRouterModule(Scenario sc) {
		this.sc = sc;
	}
	
	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
	}

	@Override
	public void handlePlan(Plan plan) {
		Leg leg = (Leg) plan.getPlanElements().get(1);

		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();

		/* Add the first link. */
		linkIds.add(Id.createLinkId("1"));
		
		/* Add the next two links based on some random sample. */
		int routeChoice = MatsimRandom.getLocalInstance().nextInt(9);
		switch (routeChoice) {
		case 0:
			linkIds.add(Id.createLinkId("2"));
			linkIds.add(Id.createLinkId("11"));
			break;
		case 1:
			linkIds.add(Id.createLinkId("3"));
			linkIds.add(Id.createLinkId("12"));
			break;
		case 2:
			linkIds.add(Id.createLinkId("4"));
			linkIds.add(Id.createLinkId("13"));
			break;
		case 3:
			linkIds.add(Id.createLinkId("5"));
			linkIds.add(Id.createLinkId("14"));
			break;
		case 4:
			linkIds.add(Id.createLinkId("6"));
			linkIds.add(Id.createLinkId("15"));
			break;
		case 5:
			linkIds.add(Id.createLinkId("7"));
			linkIds.add(Id.createLinkId("16"));
			break;
		case 6:
			linkIds.add(Id.createLinkId("8"));
			linkIds.add(Id.createLinkId("17"));
			break;
		case 7:
			linkIds.add(Id.createLinkId("9"));
			linkIds.add(Id.createLinkId("18"));
			break;
		case 8:
			linkIds.add(Id.createLinkId("10"));
			linkIds.add(Id.createLinkId("19"));
			break;
		default:
			break;
		}
		
		/* Add the final link */
		linkIds.add(Id.createLinkId("20"));
		
		Route route = RouteUtils.createNetworkRoute(linkIds, sc.getNetwork());
		route.setDistance(25000);
		leg.setRoute(route);
	}

	@Override
	public void finishReplanning() {
	}

}
