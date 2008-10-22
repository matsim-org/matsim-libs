/* *********************************************************************** *
 * project: org.matsim.*
 * LocationMutatorwChoiceSetSimultan.java
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


import java.util.Iterator;
//import org.apache.log4j.Logger;
import org.matsim.controler.Controler;

import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.utils.geometry.Coord;

public class LocationMutatorwChoiceSetSimultan extends LocationMutatorwChoiceSet {
	
	//private static final Logger log = Logger.getLogger(LocationMutatorwChoiceSetSimultan.class);
	
	public LocationMutatorwChoiceSetSimultan(final NetworkLayer network, Controler controler) {
		super(network, controler);
	}
	
	@Override
	protected boolean handleSubChain(SubChain subChain, double speed, int trialNr) {
				
		if (trialNr > 50) {		
			super.unsuccessfullLC += 1;
					
			Iterator<Act> act_it = subChain.getSlActs().iterator();
			while (act_it.hasNext()) {
				Act act = act_it.next();
				this.modifyLocation(act, subChain.getStartCoord(), subChain.getEndCoord(), Double.MAX_VALUE, 0);
			}
			return true;
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
				return false;
			}
					
			startCoord = act.getCoord();				
			ttBudget -= this.computeTravelTime(prevAct, act);
			
			if (!act_it.hasNext()) {
				double tt2Anchor = this.computeTravelTime(act, subChain.getLastPrimAct());
				ttBudget -= tt2Anchor;
			}
			
			if (ttBudget < 0.0) {
				return false;
			}
			prevAct = act;
		}
		return true;
	}
}
