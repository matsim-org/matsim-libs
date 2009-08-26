/* *********************************************************************** *
 * project: org.matsim.*
 * EvacDestinationAssigner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.groupedevac.controler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

public class EvacDestinationAssigner implements ScoringListener {

	private PopulationImpl population;

	private HashMap<LinkImpl,ArrayList<PlanImpl>> linkPlanMapping;
	private HashMap<LinkImpl,Id> linkColor;

	ArrayList<LinksScoreGroup> linksScoreGroups;
	
	private final NetworkLayer network;



	private final PlansCalcRoute plansCalcRoute;
	
	
	public EvacDestinationAssigner(final TravelCost travelCostCalculator,
			final TravelTimeCalculator travelTimeCalculator, final NetworkLayer network) {

		this.network = network;
		this.plansCalcRoute = new PlansCalcRoute(network,travelCostCalculator,travelTimeCalculator, new DijkstraFactory()); 

	}


	public void notifyScoring(final ScoringEvent event) {
		if (event.getIteration() > 150) {
			return;
		}
		Controler controler = event.getControler();
		this.population = controler.getPopulation();
		this.linkPlanMapping = new HashMap<LinkImpl,ArrayList<PlanImpl>>();
		this.linkColor = new HashMap<LinkImpl, Id>();
		this.linksScoreGroups = new ArrayList<LinksScoreGroup>();
		iteratePlans();
		iterateLinks();
		iterateLinksScoreGroups();
	}

	
	private void iterateLinksScoreGroups() {
		for (LinksScoreGroup group : this.linksScoreGroups) {
			LinkImpl link = group.getBestLink();
			PlanImpl plan = getBestLinkPlan(link);
			ArrayList<Node> evacRoute = new ArrayList<Node>(((NetworkRouteWRefs) ((LegImpl) plan.getPlanElements().get(1)).getRoute()).getNodes());
			if (isOutLink(link, group.getNode())){
				evacRoute.add(0, group.getNode());
			}
				
			
			LinkImpl dest = ((ActivityImpl)plan.getPlanElements().get(2)).getLink();
				
			
			
			Id color = this.linkColor.get(link);
			ArrayList<LinkImpl> links = group.getLinks();
			for (LinkImpl l : links) {

				ArrayList<PlanImpl> plans = this.linkPlanMapping.get(l);
				if (plans == null) {
					continue;
				}
				if (this.linkColor.get(l) != color) {
					ArrayList<Node> tmpEvacRoute = new ArrayList<Node>(evacRoute);
					if (isOutLink(l,group.getNode())) {
						tmpEvacRoute.add(0, l.getToNode());	
					}
					
					if (l.getToNode() != tmpEvacRoute.get(0)) {
						int i = 0;
						i++;
					}
					modifyPlans(dest, tmpEvacRoute, l,plans);
				}
				
				
			}
			
		}
		
	}


	


	private boolean isOutLink(final LinkImpl l, final NodeImpl node) {
		for (LinkImpl tmp : node.getOutLinks().values()){
			if (tmp == l) {
				return true;
			}
		}
		return false;
	}


	private void modifyPlans(final Link dest, final ArrayList<Node> evacRoute, final Link origin, final ArrayList<PlanImpl> plans) {
		
		for (PlanImpl plan : plans) {
			LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
			NetworkRouteWRefs route = new NodeNetworkRouteImpl();
			route.setNodes(evacRoute);
			leg.setRoute(route);
			ActivityImpl act = new org.matsim.core.population.ActivityImpl("h",(LinkImpl)dest);
			try {
				route.getLinks();
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

			PersonImpl pers = plan.getPerson();
			// keep best plan only
			PlanImpl bestPlan = null;
			double bestScore = Double.NEGATIVE_INFINITY;
			for (PlanImpl plan2 : pers.getPlans()) {
				if (plan2.getScore().doubleValue() > bestScore) {
					bestScore = plan2.getScore().doubleValue();
					bestPlan = plan2;
				}
			}
			pers.getPlans().clear();
			pers.getPlans().add(bestPlan);
			pers.setSelectedPlan(bestPlan);
//			((PersonImpl)pers).removeWorstPlans(0);
			plan = bestPlan;
			plan.removeLeg(1);
			plan.addLeg(leg);
			plan.addActivity(act);
//			this.plansCalcRoute.handleLeg(leg,((Act)plan.getActsLegs().get(0)), act, 3 * 3600);
			
//			pers.addPlan(plan);
//			if (plan.getFirstActivity().getLink().getToNode() == evacRoute.get(1) || plan.getFirstActivity().getLink().getToNode() != evacRoute.get(0)) {
//				int i = 0;
//				i++;
//				System.err.println("haha");
//			} else {
//				System.out.println("yea!!");
//			}
		}
	}





	private PlanImpl getBestLinkPlan(final LinkImpl link) {
		PlanImpl bestPlan = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (PlanImpl plan : this.linkPlanMapping.get(link)) {
			if (plan.getScore().doubleValue() > bestScore) {
				bestScore = plan.getScore().doubleValue();
				bestPlan = plan;
			}
		}
		return bestPlan;
	}


	private void iterateLinks() {
		for (NodeImpl node : this.network.getNodes().values()){
			boolean mixedColored = false;
			Iterator it = node.getInLinks().values().iterator();
			LinkImpl firstLink = (LinkImpl) it.next();
			Id color = this.linkColor.get(firstLink);
			if (color == null) {
				continue;
			}
			while ( it.hasNext()) {
				LinkImpl link = (LinkImpl) it.next();
				Id tmpColor = this.linkColor.get(link);
				if (tmpColor == null) {
					continue;
				}
				if (color != tmpColor) {
					mixedColored = true;
					break;
				}
			}
			
			it = node.getOutLinks().values().iterator();
			while (it.hasNext() && !mixedColored) {
				LinkImpl link = (LinkImpl) it.next();
				Id tmpColor = this.linkColor.get(link);
				if (tmpColor == null) {
					continue;
				}
				if (color != tmpColor) {
					mixedColored = true;
					break;
				}
			}
			
			if (mixedColored) {
				generateLinksScoreGroup(node);
			}
		}
		
	}


	private void generateLinksScoreGroup(final NodeImpl node) {
		LinksScoreGroup mapping = new LinksScoreGroup(node);
		Iterator it = node.getInLinks().values().iterator();
		while (it.hasNext()) {
			LinkImpl link = (LinkImpl) it.next();
			double score = calcScore(link);
			mapping.addLinkScore(link, score);
		}
		
		it = node.getOutLinks().values().iterator();
		while (it.hasNext()) {
			LinkImpl link = (LinkImpl) it.next();
			double score = calcScore(link);
			mapping.addLinkScore(link, score);
		}
		
		this.linksScoreGroups.add(mapping);
	}


	private double calcScore(final LinkImpl link) {
		ArrayList<PlanImpl> plans = this.linkPlanMapping.get(link);
		if (plans == null) {
			return Double.NEGATIVE_INFINITY;
		}
		
		
		double scoreSum = 0;
		for (PlanImpl plan : plans) {
			scoreSum += plan.getScore().doubleValue();
		}
		
		return scoreSum / plans.size();
	}


	private void iteratePlans() {
		Collection<PersonImpl> pers = this.population.getPersons().values();
		for (PersonImpl p : pers) {
			PlanImpl plan = p.getSelectedPlan();
			LinkImpl link = plan.getFirstActivity().getLink();
			ArrayList<PlanImpl> coPlans = this.linkPlanMapping.get(link);
			if (coPlans == null) {
				coPlans = new ArrayList<PlanImpl>();
				this.linkPlanMapping.put(link, coPlans);
			}
			coPlans.add(plan);
			if (this.linkColor.get(link) == null) {
				this.linkColor.put(link, ((ActivityImpl)plan.getPlanElements().get(2)).getLinkId());
			}
		}
	}


	private static class LinksScoreGroup{ 
		private final ArrayList<LinkImpl> links;
		private final ArrayList<Double> score;
		private final NodeImpl node;
		
		public LinksScoreGroup(final NodeImpl node) {
			this.node = node;
			this.links = new ArrayList<LinkImpl>();
			this.score = new ArrayList<Double>();
		}
		
		
		public void addLinkScore(final LinkImpl link, final double score) {
			this.links.add(link);
			this.score.add(score);
		}
		
		public ArrayList<LinkImpl> getLinks() {
			return this.links;
		}
		
		public LinkImpl getBestLink() {
			LinkImpl bestLink = null;
			double bestScore = Double.NEGATIVE_INFINITY;
			double worstScore = Double.POSITIVE_INFINITY;
			for (int i = 0; i < this.score.size(); i++) {
				double score = this.score.get(i);
				if (score > bestScore) {
					bestLink = this.links.get(i);
					bestScore = score;
				}
				if (score < worstScore) {
					worstScore = score;
				}
				
				
			}
			
			return bestLink;
		}
		
		public NodeImpl getNode() {
			return this.node;
		}
	}
}
