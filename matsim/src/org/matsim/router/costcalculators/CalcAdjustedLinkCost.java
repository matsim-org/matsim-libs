/* *********************************************************************** *
 * project: org.matsim.*
 * CalcAdjustedLinkCost.java
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

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.router.util.TravelCost;

/**
 * This travelcost-calculator takes another travelcost-calculator as base
 * but modifies its travel cost depending on the attractivity of a link.<br/>
 * Attractivity of a link is currently depending on the capacity of the link:
 * a link with a high capacity (thus having a large number of lanes) is
 * more attractive as a link with a lower capacity.<br/>
 * This takes the freespeed-velocity implicitly in account too, as high-cap
 * links are more likely to have a higher velocity than low-cap links.
 */
public class CalcAdjustedLinkCost implements TravelCost {

	private final TravelCost baseCost;
	private final double flowCapFactor;

	public CalcAdjustedLinkCost(final TravelCost baseCost) {
		this.baseCost = baseCost;
		this.flowCapFactor = Gbl.getConfig().simulation().getFlowCapFactor();
	}

	public double getLinkTravelCost(final Link link, final double time) {
		double factor = (4000.0/3600.0*this.flowCapFactor - link.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME))*0.1 + 1.0;
		return this.baseCost.getLinkTravelCost(link, time) * factor;
	}

}
