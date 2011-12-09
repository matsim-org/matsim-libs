/* *********************************************************************** *
 * project: org.matsim.*
 * RetailerRelocation.java
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

package playground.anhorni.agglo.run;

import java.util.Collection;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.LinkNetworkRoute;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.locationchoice.facilityload.FacilityPenalty;


public class RetailerRelocation implements IterationStartsListener {	

	private TreeMap<Id, LinkCandidate> linkCandidates;
	private static final Logger log = Logger.getLogger(RetailerRelocation.class);
	private QuadTree<ActivityFacility> shoppingFacilities;
	private Random rnd = new Random();
	private static double replanningShare = 0.1;
	TreeMap<Id, FacilityPenalty> facilityPenalties;
	TreeMap<Double, LinkCandidate> moveProbabilities;
	
	public RetailerRelocation(TreeMap<Id, FacilityPenalty> facilityPenalties) {
		this.facilityPenalties = facilityPenalties;
	}
	
	public void notifyIterationStarts(IterationStartsEvent event) {	
		if (event.getIteration() % 2 == 0 && event.getIteration() > 0) {
			this.initialize(event);
			this.evaluatePotentialCustomers(event);
			this.evaluateCompetitorsPower(event);
			this.generateProbabilities(event);
			this.relocateSomeRetailers(event, RetailerRelocation.replanningShare);
			this.adaptAgents(event);
		}
	}
	
	private void initialize(IterationStartsEvent event) {
		rnd.setSeed(1036230);
		TreeMap<Id,ActivityFacility> facilities_of_type = 
			event.getControler().getScenario().getActivityFacilities().getFacilitiesForActivityType("s");
		this.shoppingFacilities = this.builFacQuadTree(facilities_of_type);
	}
	
	private void evaluatePotentialCustomers(IterationStartsEvent event) {
		
		linkCandidates = new TreeMap<Id, LinkCandidate>();
		// count shopping trips or back home from shopping trips
		for (Person person : event.getControler().getPopulation().getPersons().values()) {
			PlanImpl selectedPlan = (PlanImpl)person.getSelectedPlan();
			for (PlanElement pe : selectedPlan.getPlanElements()) {				
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					if (act.getType().startsWith("s")) {						
						// get trip to and trip from
						LegImpl tripTo = (LegImpl)selectedPlan.getNextLeg(act);
						LinkNetworkRoute routeTo = (LinkNetworkRoute) tripTo.getRoute();
						this.iterateRoute(routeTo);	
						LegImpl tripFrom = (LegImpl)selectedPlan.getPreviousLeg(act);
						LinkNetworkRoute routeFrom = (LinkNetworkRoute) tripFrom.getRoute();
						this.iterateRoute(routeFrom);	
					}
				}
			}
		}
	}
	
	private void evaluateCompetitorsPower(IterationStartsEvent event) {
		NetworkImpl network = event.getControler().getNetwork();
		for (LinkCandidate candidate : this.linkCandidates.values()) {
			Link link = network.getLinks().get(candidate.getLinkId());
			double power = this.getCompetitorsPowerInArea(link.getCoord());
			candidate.increaseCompetitorsPower(power);
		}
	}
	
	private double getCompetitorsPowerInArea(Coord coord) {
		double distance = 1000.0; // TODO: make this dependent on location and business sectors
		Collection<ActivityFacility> competitors = this.shoppingFacilities.get(coord.getX(), coord.getY(), distance);
		return competitors.size() + 1; 	//TODO: weight with distance and size of competitors
	}
	
	private void iterateRoute(LinkNetworkRoute route) {
		for (Id linkId : route.getLinkIds()) {
			this.updateLinkCandidate(linkId, 1);
		}
	}
		
	private void updateLinkCandidate(Id id, int increasePotentialCustomersCount) {
		if (!linkCandidates.containsKey(id)) {
			linkCandidates.put(id, new LinkCandidate(id));
		}
		linkCandidates.get(id).increasePotentialCustomersCount(increasePotentialCustomersCount);
	}
	
	private void relocateSomeRetailers(IterationStartsEvent event, double share) {
		NetworkImpl network = event.getControler().getNetwork();
		
		TreeMap<Id,ActivityFacility> shoppingfacs = 
			event.getControler().getScenario().getActivityFacilities().getFacilitiesForActivityType("s");
		
		for (ActivityFacility facility : shoppingfacs.values()) {
			if (rnd.nextDouble() < share) {
				Id linkId = this.evaluateAndMaybeMoveFacility(facility);
				if (linkId != null) {
					this.move((ActivityFacilityImpl)facility, network.getLinks().get(linkId));
				}
			}
		}
	}
	
	private void generateProbabilities(IterationStartsEvent event) {		
		this.moveProbabilities = new TreeMap<Double, LinkCandidate>();
		
		NetworkImpl network = event.getControler().getNetwork();
		double totalPower = 0.0;
		for (LinkCandidate candidate : this.linkCandidates.values()) {
			Link link = network.getLinks().get(candidate.getLinkId());
			totalPower += this.getCompetitorsPowerInArea(link.getCoord());
		}
		double currentPower = 0.0;
		for (LinkCandidate candidate : this.linkCandidates.values()) {
			Link link = network.getLinks().get(candidate.getLinkId());
			currentPower += this.getCompetitorsPowerInArea(link.getCoord());
			this.moveProbabilities.put(currentPower, candidate);
		}	
	}
	
	private Id evaluateAndMaybeMoveFacility(ActivityFacility facility) {
		// if load/competitors is better at new location -> move
		// evaluate potential at old location
		double currentLoad = this.facilityPenalties.get(facility.getId()).getFacilityLoad().getNumberOfVisitorsPerDay();
		double competitorsPower = this.getCompetitorsPowerInArea(facility.getCoord());
		double currentPotential = currentLoad / (competitorsPower + 1); //  + 1 is ego
		
		// evaluate potential at other locations and maybe chose one with certain probability
		Id choice = null;
		double r = 0.0;
		if (this.moveProbabilities.size() > 0) {
			r = this.rnd.nextDouble() * this.moveProbabilities.lastEntry().getKey();
		}
		for (double key : this.moveProbabilities.keySet()) {
			if (key > r) {
				LinkCandidate candidate = this.moveProbabilities.get(key);
				double potential = candidate.getPotential();
				if (potential > currentPotential) {
					choice = candidate.getLinkId();
				}
			}
		}
		return choice;
	}
	
	private void adaptAgents(IterationStartsEvent event) {
		NetworkImpl network = event.getControler().getNetwork();
		ActivityFacilities facilities = event.getControler().getFacilities();
		
		LeastCostPathCalculator calculator = event.getControler().getLeastCostPathCalculatorFactory().createPathCalculator(network, 
				(TravelCost)event.getControler().createTravelCostCalculator(),
				(TravelTime)event.getControler().getTravelTimeCalculator());
		
		NetworkLegRouter router = new NetworkLegRouter(network, calculator, new ModeRouteFactory());
		
		for (Person person : event.getControler().getPopulation().getPersons().values()) {
			PlanImpl selectedPlan = (PlanImpl)person.getSelectedPlan();
			for (PlanElement pe : selectedPlan.getPlanElements()) {				
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					if (act.getType().startsWith("s")) {
						ActivityFacilityImpl facility = (ActivityFacilityImpl)facilities.getFacilities().get(act.getFacilityId());
						if (facility.getLinkId() == null) {
							facility.setLinkId(network.getNearestLink(facility.getCoord()).getId());
						}
						act.setLinkId(facility.getLinkId());
						act.setCoord(facility.getCoord());
						
						LegImpl tripTo = (LegImpl)selectedPlan.getNextLeg(act);
						LegImpl tripFrom = (LegImpl)selectedPlan.getPreviousLeg(act);
						
						router.routeLeg(selectedPlan.getPerson(), tripTo, selectedPlan.getPreviousActivity(tripTo), act, 
								selectedPlan.getPreviousActivity(tripTo).getEndTime());
						
						router.routeLeg(selectedPlan.getPerson(), tripFrom, act, 
								selectedPlan.getNextActivity(tripFrom), act.getEndTime());	
					}
				}
			}
		}
	}
	
	private void move(ActivityFacilityImpl facility, Link link) {
		facility.setLinkId(link.getId());
		facility.setCoord(link.getCoord());
	}
	
	private QuadTree<ActivityFacility> builFacQuadTree(TreeMap<Id,ActivityFacility> facilities_of_type) {
		Gbl.startMeasurement();
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;

		for (final ActivityFacility f : facilities_of_type.values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		log.info("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<ActivityFacility> quadtree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (final ActivityFacility f : facilities_of_type.values()) {
			quadtree.put(f.getCoord().getX(),f.getCoord().getY(),f);
		}
		log.info("    done");
		Gbl.printRoundTime();
		Gbl.printMemoryUsage();
		return quadtree;
	}
}
