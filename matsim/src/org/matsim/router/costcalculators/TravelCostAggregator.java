/* *********************************************************************** *
 * project: org.matsim.*
 * TravelCostAggregator.java
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

package org.matsim.router.costcalculators;

import java.util.ArrayList;

import org.matsim.network.Link;
import org.matsim.router.util.TravelCostI;

/**
 * This class can aggregate the traveltime and travelcost from different
 * TravelCost-Calculators and build an average value.
 * 
 */
public class TravelCostAggregator implements TravelCostI {

	private ArrayList<TravelCostI> components = new ArrayList<TravelCostI>();
	private ArrayList<Double> costWeights = new ArrayList<Double>();

	public void addCostComponent(TravelCostI component, double costWeight) {
		components.add(component);
		costWeights.add(costWeight);
	}
	
	public double getLinkTravelCost(Link link, double time) {
		double totalCost = 0;
		for (int i = 0; i < components.size(); i++) {
			TravelCostI component = components.get(i);
			double weight = costWeights.get(i);
			totalCost += weight * component.getLinkTravelCost(link, time);
		}
		return totalCost;
	}
}
