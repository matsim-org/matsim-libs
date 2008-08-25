/* *********************************************************************** *
 * project: org.matsim.*
 * RandomLocationMutator.java
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

package org.matsim.locationchoice;

import java.util.ArrayList;

import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;


public class RandomLocationMutator extends LocationMutator {

	boolean ch = false;

	public RandomLocationMutator(final NetworkLayer network) {
		super(network);
		
		if (Gbl.getConfig().locationchoice().getArea().equals("ch")) {
			this.ch = true;
		}
	}


	// plan == selected plan
	@Override
	public void handlePlan(final Plan plan){

		if (this.chShopFacilitiesTreeMap.size() > 0 && this.zhShopFacilities.size() > 0) {
			
			if (this.ch) {
				exchangeFacilities("s",this.chShopFacilities, plan);
			}
			else {
				exchangeFacilities("s",this.zhShopFacilities, plan);				
			}
			
		}

		if (this.chLeisureFacilitiesTreeMap.size() > 0 && this.zhLeisureFacilities.size() > 0) {
			if (this.ch) {
				exchangeFacilities("l",this.chLeisureFacilities, plan);
			}
			else {
				exchangeFacilities("l",this.zhLeisureFacilities, plan);
			}
		}
	}


	public void exchangeFacilities(final String type, ArrayList<Facility>  exchange_facilities, final Plan plan) {
		
		final ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Act act = (Act)actslegs.get(j);
			if (act.getType().startsWith(type)) {
				
				final Facility facility=(Facility)exchange_facilities.toArray()[
				           MatsimRandom.random.nextInt(exchange_facilities.size()-1)];
				
				act.setFacility(facility);
				act.setLink(this.network.getNearestLink(facility.getCenter()));
				act.setCoord(facility.getCenter());
			}
		}

		// loop over all <leg>s, remove route-information
		// routing is done after location choice
		for (int j = 1; j < actslegs.size(); j=j+2) {
			final Leg leg = (Leg)actslegs.get(j);
			leg.setRoute(null);
		}
	}
}
