/* *********************************************************************** *
 * project: org.matsim.*
 * LocationMutatorwChoiceSet.java
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

package org.matsim.locationchoice.constrained;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.controler.Controler;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.locationchoice.LocationMutator;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.router.PlansCalcRoute;
import org.matsim.utils.geometry.Coord;



public class LocationMutatorwChoiceSet extends LocationMutator {
	
//	private static final Logger log = Logger.getLogger(LocationMutatorwChoiceSet.class);
	protected int unsuccessfullLC = 0;
	private double recursionTravelSpeedChange = 0.1;
	private double recursionTravelSpeed = 30.0;
	protected int maxRecursions = 10;
	
	public LocationMutatorwChoiceSet(final NetworkLayer network, Controler controler) {
		super(network, controler);
		this.recursionTravelSpeedChange = Double.parseDouble(Gbl.getConfig().locationchoice().getRecursionTravelSpeedChange());
		this.maxRecursions = Integer.parseInt(Gbl.getConfig().locationchoice().getMaxRecursions());
		this.recursionTravelSpeed = Double.parseDouble(Gbl.getConfig().locationchoice().getRecursionTravelSpeed());
	}
	
	@Override
	public void handlePlan(final Plan plan){
		List<SubChain> subChains = this.calcActChains(plan);
		this.handleSubChains(plan, subChains);
			
		final ArrayList<?> actslegs = plan.getActsLegs();
		// loop over all <leg>s, remove route-information
		// routing is done after location choice
		for (int j = 1; j < actslegs.size(); j=j+2) {
			final Leg leg = (Leg)actslegs.get(j);
			leg.setRoute(null);
		}	
	}
	
	public int getNumberOfUnsuccessfull() {
		return this.unsuccessfullLC;		
	}
	
	public void resetUnsuccsessfull() {
		this.unsuccessfullLC = 0;
	}

	public void handleSubChains(final Plan plan, List<SubChain> subChains) {		
		Iterator<SubChain> sc_it = subChains.iterator();
		while (sc_it.hasNext()) {
			SubChain sc = sc_it.next();
			
			//initially using 25.3 km/h + 20%
			// micro census 2005
			//double speed = 30.36/3.6;
			double speed = this.recursionTravelSpeed;
			
			if (sc.getTtBudget() < 1.0) {
				continue;
			}
					
			int nrOfTrials = 0;
			int change = -2;
			boolean shrinked = false;
			while (change != 0) {				
				// shrinking only every second time
				if (change == -1 && shrinked) {
					speed *= (1.0 - this.recursionTravelSpeedChange);
					shrinked = true;
				}
				else if (change == 1) {
					speed *= (1.0 + this.recursionTravelSpeedChange);
					shrinked = false;
				}				
				change = this.handleSubChain(sc, speed, nrOfTrials);
				nrOfTrials++;
			}
		}
	}
	
	
	protected int handleSubChain(SubChain subChain, double speed, int trialNr){
		if (trialNr > this.maxRecursions) {		
			this.unsuccessfullLC += 1;
					
			Iterator<Act> act_it = subChain.getSlActs().iterator();
			while (act_it.hasNext()) {
				Act act = act_it.next();
				this.modifyLocation(act, subChain.getStartCoord(), subChain.getEndCoord(), Double.MAX_VALUE, 0);
			}
			return 0;
		}
		
		Coord startCoord = subChain.getStartCoord();
		Coord endCoord = subChain.getEndCoord();
		double ttBudget = subChain.getTtBudget();		
		
		Act prevAct = subChain.getFirstPrimAct();
		
		Iterator<Act> act_it = subChain.getSlActs().iterator();
		while (act_it.hasNext()) {
			Act act = act_it.next();
			double radius = (ttBudget * speed) / 2.0;	
			if (!this.modifyLocation(act, startCoord, endCoord, radius, 0)) {
				return 1;
			}
					
			startCoord = act.getCoord();				
			ttBudget -= this.computeTravelTime(prevAct, act);
			
			if (!act_it.hasNext()) {
				double tt2Anchor = this.computeTravelTime(act, subChain.getLastPrimAct());
				ttBudget -= tt2Anchor;
			}
			
			if (ttBudget < 0.0) {
				return -1;
			}
			prevAct = act;
		}
		return 0;
	}

	
	protected boolean modifyLocation(Act act, Coord startCoord, Coord endCoord, double radius, int trialNr) {
		
		ArrayList<Facility> choiceSet = this.computeChoiceSetCircle
		(startCoord, endCoord, radius, act.getType());
		
		if (choiceSet.size()>1) {
			//final Facility facility=(Facility)choiceSet.toArray()[
           	//		           MatsimRandom.random.nextInt(choiceSet.size()-1)];
			final Facility facility=(Facility)choiceSet.get(MatsimRandom.random.nextInt(choiceSet.size()-1));
			
			act.setFacility(facility);
       		act.setLink(this.network.getNearestLink(facility.getCenter()));
       		act.setCoord(facility.getCenter());
       		return true;
		}
		else {
			return false; 			
		}	
	}
	
	protected double computeTravelTime(Act fromAct, Act toAct) {	
		Leg leg = new Leg(BasicLeg.Mode.car);
		leg.setNum(0);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(0.0);
		leg.setArrivalTime(0.0);
		
		PlansCalcRoute router = (PlansCalcRoute)this.controler.getRoutingAlgorithm();
		router.handleLeg(leg, fromAct, toAct, fromAct.getEndTime());
		return leg.getTravelTime();
	}
		
	public List<SubChain> calcActChains(final Plan plan) {
		
		ManageSubchains manager = new ManageSubchains();	
		List<Act> movablePrimaryActivities = null; 
		if (Gbl.getConfig().locationchoice().getFixByActType().equals("false")) {
			movablePrimaryActivities = defineMovablePrimaryActivities(plan);
		}
				
		final ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Act act = (Act)actslegs.get(j);
			
			boolean isPrimary = false;
			boolean movable = false;
			if (Gbl.getConfig().locationchoice().getFixByActType().equals("false")) {	
				isPrimary = plan.getPerson().getKnowledge().isPrimary(act.getType(), act.getFacilityId());
				movable = movablePrimaryActivities.contains(act);
			}
			else {
				isPrimary = plan.getPerson().getKnowledge().isSomewherePrimary(act.getType());
			}
			
			// found secondary activity
			// test for home if by accident home is not declared as primary
			if ((!isPrimary || movable) && !act.getType().startsWith("h")) {			
				manager.secondaryActivityFound(act, (Leg)actslegs.get(j+1));
			}		
			// found primary activity
			else {			
				if (j == (actslegs.size()-1)) {
					manager.primaryActivityFound(act, null);
				}
				else {
					manager.primaryActivityFound(act, (Leg)actslegs.get(j+1));
				}
			}
		}
		return manager.getSubChains();
	}
	
	/* 
	 * All but one activity type are fixed. No primary activities are moved.
	 * Needed for the computation of shopping location choice sets.
	 */
	public List<SubChain> calcActChainsHavingOneFlexibleActivityType(final Plan plan, String flexibleActivityType) {
		ManageSubchains manager = new ManageSubchains();	
		
		final ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Act act = (Act)actslegs.get(j);
						
			// found secondary activity
			if (act.getType().equals(flexibleActivityType)) {			
				manager.secondaryActivityFound(act, (Leg)actslegs.get(j+1));
			}		
			// found primary activity
			else {			
				if (j == (actslegs.size()-1)) {
					manager.primaryActivityFound(act, null);
				}
				else {
					manager.primaryActivityFound(act, (Leg)actslegs.get(j+1));
				}
			}
		}
		return manager.getSubChains();
	}
	
	
	public ArrayList<Facility>  computeChoiceSetCircle(Coord coordStart, Coord coordEnd, 
			double radius, String type) {
		double midPointX = (coordStart.getX()+coordEnd.getX())/2.0;
		double midPointY = (coordStart.getY()+coordEnd.getY())/2.0;
		return (ArrayList<Facility>) this.quad_trees.get(type).get(midPointX, midPointY, radius);
	}
	
	// for test cases:
	public double getRecursionTravelSpeedChange() {
		return recursionTravelSpeedChange;
	}

	public void setRecursionTravelSpeedChange(double recursionTravelSpeedChange) {
		this.recursionTravelSpeedChange = recursionTravelSpeedChange;
	}

	public int getMaxRecursions() {
		return maxRecursions;
	}

	public void setMaxRecursions(int maxRecursions) {
		this.maxRecursions = maxRecursions;
	}
}
