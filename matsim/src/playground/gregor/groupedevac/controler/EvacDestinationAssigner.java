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

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.controler.Controler;
import org.matsim.controler.events.ScoringEvent;
import org.matsim.controler.listener.ScoringListener;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.Route;
import org.matsim.population.RouteImpl;
import org.matsim.router.PlansCalcRouteDijkstra;
import org.matsim.router.util.TravelCost;
import org.matsim.trafficmonitoring.TravelTimeCalculator;

public class EvacDestinationAssigner implements ScoringListener {

	private Population population;

	private HashMap<Link,ArrayList<Plan>> linkPlanMapping;
	private HashMap<Link,Id> linkColor;

	ArrayList<LinksScoreGroup> linksScoreGroups;
	
	private final NetworkLayer network;



	private final PlansCalcRouteDijkstra plansCalcRoute;
	
	
	public EvacDestinationAssigner(final TravelCost travelCostCalculator,
			final TravelTimeCalculator travelTimeCalculator, final NetworkLayer network) {

		this.network = network;
		this.plansCalcRoute = new PlansCalcRouteDijkstra(network,travelCostCalculator,travelTimeCalculator); 

	}


	public void notifyScoring(final ScoringEvent event) {
		if (event.getIteration() > 150) {
			return;
		}
		Controler controler = event.getControler();
		this.population = controler.getPopulation();
		this.linkPlanMapping = new HashMap<Link,ArrayList<Plan>>();
		this.linkColor = new HashMap<Link, Id>();
		this.linksScoreGroups = new ArrayList<LinksScoreGroup>();
		iteratePlans();
		iterateLinks();
		iterateLinksScoreGroups();
	}

	
	private void iterateLinksScoreGroups() {
		for (LinksScoreGroup group : this.linksScoreGroups) {
			Link link = group.getBestLink();
			Plan plan = getBestLinkPlan(link);
			ArrayList<Node> evacRoute = new ArrayList<Node>(((Leg)plan.getActsLegs().get(1)).getRoute().getRoute());
			if (isOutLink(link, group.getNode())){
				evacRoute.add(0, group.getNode());
			}
				
			
			Link dest = ((Act)plan.getActsLegs().get(2)).getLink();
				
			
			
			Id color = this.linkColor.get(link);
			ArrayList<Link> links = group.getLinks();
			for (Link l : links) {

				ArrayList<Plan> plans = this.linkPlanMapping.get(l);
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


	


	private boolean isOutLink(final Link l, final Node node) {
		for (Link tmp : node.getOutLinks().values()){
			if (tmp == l) {
				return true;
			}
		}
		return false;
	}


	private void modifyPlans(final Link dest, final ArrayList<Node> evacRoute, final Link origin, final ArrayList<Plan> plans) {
		
		for (Plan plan : plans) {
			Leg leg = new Leg(Mode.car);
			Route route = new RouteImpl();
			route.setRoute(evacRoute);
			leg.setRoute(route);
			leg.setNum(1);
			Act act = new Act("h",dest);
			try {
				route.getLinkRoute();
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

			Person pers = plan.getPerson();
			pers.removeWorstPlans(0);
			plan = pers.getSelectedPlan();
			plan.removeLeg(1);
			plan.addLeg(leg);
			plan.addAct(act);
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





	private Plan getBestLinkPlan(final Link link) {
		Plan bestPlan = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (Plan plan : this.linkPlanMapping.get(link)) {
			if (plan.getScore() > bestScore) {
				bestScore = plan.getScore();
				bestPlan = plan;
			}
		}
		return bestPlan;
	}


	private void iterateLinks() {
		for (Node node : this.network.getNodes().values()){
			boolean mixedColored = false;
			Iterator it = node.getInLinks().values().iterator();
			Link firstLink = (Link) it.next();
			Id color = this.linkColor.get(firstLink);
			if (color == null) {
				continue;
			}
			while ( it.hasNext()) {
				Link link = (Link) it.next();
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
				Link link = (Link) it.next();
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


	private void generateLinksScoreGroup(final Node node) {
		LinksScoreGroup mapping = new LinksScoreGroup(node);
		Iterator it = node.getInLinks().values().iterator();
		while (it.hasNext()) {
			Link link = (Link) it.next();
			double score = calcScore(link);
			mapping.addLinkScore(link, score);
		}
		
		it = node.getOutLinks().values().iterator();
		while (it.hasNext()) {
			Link link = (Link) it.next();
			double score = calcScore(link);
			mapping.addLinkScore(link, score);
		}
		
		this.linksScoreGroups.add(mapping);
	}


	private double calcScore(final Link link) {
		ArrayList<Plan> plans = this.linkPlanMapping.get(link);
		if (plans == null) {
			return Double.NEGATIVE_INFINITY;
		}
		
		
		double scoreSum = 0;
		for (Plan plan : plans) {
			scoreSum += plan.getScore();
		}
		
		return scoreSum / plans.size();
	}


	private void iteratePlans() {
		Collection<Person> pers = this.population.getPersons().values();
		for (Person p : pers) {
			Plan plan = p.getSelectedPlan();
			Link link = plan.getFirstActivity().getLink();
			ArrayList<Plan> coPlans = this.linkPlanMapping.get(link);
			if (coPlans == null) {
				coPlans = new ArrayList<Plan>();
				this.linkPlanMapping.put(link, coPlans);
			}
			coPlans.add(plan);
			if (this.linkColor.get(link) == null) {
				this.linkColor.put(link, ((Act)plan.getActsLegs().get(2)).getLinkId());
			}
		}
	}


	private static class LinksScoreGroup{ 
		private final ArrayList<Link> links;
		private final ArrayList<Double> score;
		private final Node node;
		
		public LinksScoreGroup(final Node node) {
			this.node = node;
			this.links = new ArrayList<Link>();
			this.score = new ArrayList<Double>();
		}
		
		
		public void addLinkScore(final Link link, final double score) {
			this.links.add(link);
			this.score.add(score);
		}
		
		public ArrayList<Link> getLinks() {
			return this.links;
		}
		
		public Link getBestLink() {
			Link bestLink = null;
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
		
		public Node getNode() {
			return this.node;
		}
	}
}
