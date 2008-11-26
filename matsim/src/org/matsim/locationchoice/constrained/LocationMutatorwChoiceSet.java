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
import org.matsim.gbl.MatsimRandom;
import org.matsim.locationchoice.LocationMutator;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.router.PlansCalcRoute;
import org.matsim.utils.geometry.Coord;



public abstract class LocationMutatorwChoiceSet extends LocationMutator {
	
//	private static final Logger log = Logger.getLogger(LocationMutatorwChoiceSet.class);
	protected int unsuccessfullLC = 0;
	
	public LocationMutatorwChoiceSet(final NetworkLayer network, Controler controler) {
		super(network, controler);
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
			double speed = 30.36/3.6;
			
			if (sc.getTtBudget() < 1.0) {
				continue;
			}
					
			int nrOfTrials = 0;
			boolean successful = false;
			while (!successful) {
				
				if (nrOfTrials % 10 == 0 && nrOfTrials > 0) {
					speed *= 0.9;
				}
				
				successful = this.handleSubChain(sc, speed, nrOfTrials);
				nrOfTrials++;
			}
		}
	}
	
	
	protected abstract boolean handleSubChain(SubChain subChain, double speed, int trialNr);
	
	protected boolean modifyLocation(Act act, Coord startCoord, Coord endCoord, double radius, int trialNr) {
		
		ArrayList<Facility> choiceSet = this.computeChoiceSet
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
		
	protected List<SubChain> calcActChains(final Plan plan) {
		
		ManageSubchains manager = new ManageSubchains();	
		List<Act> movablePrimaryActivities = defineMovablePrimaryActivities(plan);
		
		final ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Act act = (Act)actslegs.get(j);
			
			boolean isPrimary = plan.getPerson().getKnowledge().isPrimary(act.getType(), act.getFacilityId());
			boolean movable = movablePrimaryActivities.contains(act);
			
			// found secondary activity
			if (!isPrimary || movable) {			
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
	
	
	private ArrayList<Facility>  computeChoiceSet(Coord coordStart, Coord coordEnd, 
			double radius, String type) {
		double midPointX = (coordStart.getX()+coordEnd.getX())/2.0;
		double midPointY = (coordStart.getY()+coordEnd.getY())/2.0;
		return (ArrayList<Facility>) this.quad_trees.get(type).get(midPointX, midPointY, radius);
	}
}
