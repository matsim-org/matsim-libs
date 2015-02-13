/* *********************************************************************** *
 * project: org.matsim.*
 * MultipleFareSystems.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mrieser.pt.fares;

import java.util.ArrayList;
import java.util.List;

import org.matsim.facilities.ActivityFacilityImpl;

import playground.mrieser.pt.fares.api.TransitFares;

public class MultipleFareSystems implements TransitFares {

	final List<TransitFares> allFares = new ArrayList<TransitFares>();

	public void addFares(final TransitFares fares) {
		this.allFares.add(fares);
	}

	public double getSingleTripCost(final ActivityFacilityImpl fromStop, final ActivityFacilityImpl toStop) {
		double minFare = Double.NaN;
		for (TransitFares fareSystem : this.allFares) {
			double fare = fareSystem.getSingleTripCost(fromStop, toStop);
			if (!Double.isNaN(fare)) {
				if (Double.isNaN(minFare) || (fare < minFare)) {
					minFare = fare;
				}
			}
		}
		return minFare;
	}

}
