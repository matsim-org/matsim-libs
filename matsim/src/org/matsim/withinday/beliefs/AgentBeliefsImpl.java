/* *********************************************************************** *
 * project: org.matsim.*
 * AgentBeliefsImpl.java
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

package org.matsim.withinday.beliefs;

import java.util.LinkedList;
import java.util.List;

import org.matsim.network.Link;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;


/**
 * @author dgrether
 *
 */
public class AgentBeliefsImpl implements AgentBeliefs {

	private List<TravelTimeI> travelTimePerceptions;
	
	private List<TravelCostI> travelCostPerceptions;
	
	private FreespeedTravelTimeCost freespeedTimeCost;
	
	
	public AgentBeliefsImpl() {
		this.travelTimePerceptions = new LinkedList<TravelTimeI>();
		this.freespeedTimeCost = new FreespeedTravelTimeCost();
	  this.travelCostPerceptions = new LinkedList<TravelCostI>();	
	}
		
	/**
	 * 
	 * @see org.matsim.router.util.TravelTimeI#getLinkTravelTime(org.matsim.network.Link, double)
	 */
	public double getLinkTravelTime(final Link link, final double time) {
		double ttime = 0.0;
		for (TravelTimeI tt : this.travelTimePerceptions) {
			ttime = tt.getLinkTravelTime(link, time);
			if (ttime > 0) {
				return ttime;
			}
		}
		return this.freespeedTimeCost.getLinkTravelTime(link, time);
	}

	
	/**
	 * @see org.matsim.router.util.TravelCostI#getLinkTravelCost(org.matsim.network.Link, double)
	 */
	public double getLinkTravelCost(final Link link, final double time) {
		double tcost = 0.0;
		for (TravelCostI tc : this.travelCostPerceptions) {
			tcost = tc.getLinkTravelCost(link, time);
			if (tcost > 0) {
				return tcost;
			}
		}
		return this.freespeedTimeCost.getLinkTravelCost(link, time);
	}

	public void addTravelTimePerception(final TravelTimeI travelTimePerception) {
		this.travelTimePerceptions.add(travelTimePerception);
	}


}
