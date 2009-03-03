/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneBasedFares.java
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

package playground.marcel.pt.fares;

import java.util.Map;

import org.matsim.interfaces.core.v01.Facility;
import org.matsim.utils.collections.Tuple;

import playground.marcel.pt.interfaces.TransitFares;

public class TableLookupFares implements TransitFares {

	private final Map<Tuple<Facility, Facility>, Double> costs;

	public TableLookupFares(final Map<Tuple<Facility, Facility>, Double> costs) {
		this.costs = costs;
	}

	public double getSingleTripCost(final Facility fromStop, final Facility toStop) {
		Double cost = this.costs.get(new Tuple<Facility, Facility>(fromStop, toStop));
		if (cost == null) {
			// not clear if this is a feature or a bug...
			cost = this.costs.get(new Tuple<Facility, Facility>(toStop, fromStop));
		}
		if (cost == null) {
			if (fromStop == toStop) {
				return 0.0;
			}
			return Double.NaN;
		}
		return cost.doubleValue();
	}

}
