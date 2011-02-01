/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityChoice.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.locationChoice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;

/**
 * @author illenberger
 *
 */
public class ActivityChoice implements PlanStrategyModule {

	private static final String ACT_TYPE = "leisure";
	
	private final Map<Person, ChoiceSet> choiceSets;
	
	private final Random random;
	
	private final Network network;
	
	private final LeastCostPathCalculator router;
	
	private final PopulationFactory factory;
	
	public ActivityChoice(Random random, Network network, LeastCostPathCalculator router, Map<Person, ChoiceSet> choiceSets, PopulationFactory factory) {
		this.random = random;
		this.network = network;
		this.router = router;
		this.choiceSets = choiceSets;
		this.factory = factory;
	}
	
	@Override
	public void prepareReplanning() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.replanning.PlanStrategyModule#handlePlan(org.matsim.api.core.v01.population.Plan)
	 */
	@Override
	public void handlePlan(Plan plan) {
		List<Activity> activities = new ArrayList<Activity>(3);
		for(int i = 0; i < plan.getPlanElements().size(); i+=2) {
			Activity act = (Activity) plan.getPlanElements().get(i);
//			if(act.getType().equalsIgnoreCase(ACT_TYPE)) {
			if(act.getType().startsWith("l")) {
				activities.add(act);
			}
		}

		if(activities.size() > 0) {
			changeAct(plan.getPerson(), plan, plan.getPlanElements().indexOf(activities.get(random.nextInt(activities.size())))); // fix this
		}
	}

	private void changeAct(Person person, Plan plan, int actIdx) {
		Activity act = (Activity) plan.getPlanElements().get(actIdx);
		ChoiceSet set = choiceSets.get(person);
		
		Person dest = set.getOpportunities().get(random.nextInt(set.getOpportunities().size()));
		
		Id destLinkId = ((Activity)dest.getSelectedPlan().getPlanElements().get(0)).getLinkId();
		Coord c = ((Activity)dest.getSelectedPlan().getPlanElements().get(0)).getCoord();
		
//		Activity newAct = factory.createActivityFromLinkId(act.getType(), destLinkId);
		Activity newAct = factory.createActivityFromCoord(act.getType(), c);
		((ActivityImpl)newAct).setLinkId(destLinkId);
		
		if(Double.isInfinite(act.getEndTime())) {
			act.setEndTime(act.getStartTime() + ((ActivityImpl) act).getDuration());
		}
		newAct.setEndTime(act.getEndTime());
		
		Activity prev = (Activity) plan.getPlanElements().get(actIdx - 2);
		Leg toLeg = (Leg)plan.getPlanElements().get(actIdx - 1);
		Leg fromLeg = (Leg)plan.getPlanElements().get(actIdx + 1);
		Activity next = (Activity) plan.getPlanElements().get(actIdx + 2);
		
		
		calcRoute(prev, newAct, toLeg);
		calcRoute(newAct, next, fromLeg);

		plan.getPlanElements().set(actIdx, newAct);
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.replanning.PlanStrategyModule#finishReplanning()
	 */
	@Override
	public void finishReplanning() {
		// TODO Auto-generated method stub

	}
	
	private double calcRoute(Activity prev, Activity next, Leg leg) {
		Id link1 = prev.getLinkId();
		Id link2 = next.getLinkId();

		Node node1 = network.getLinks().get(link1).getToNode();
		Node node2 = network.getLinks().get(link2).getToNode();
		
		Path path = router.calcLeastCostPath(node1, node2, prev.getEndTime());
		
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(link1, link2);
		route.setLinkIds(link1, NetworkUtils.getLinkIds(path.links), link2);
		leg.setRoute(route);
		leg.setTravelTime(path.travelTime);
		
		return path.travelTime;
	}

}
