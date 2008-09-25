/* *********************************************************************** *
 * project: org.matsim.*
 * FreespeedTravelTimeCost.java
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

package playground.johannes.socialnets;

import org.matsim.network.Link;
import org.matsim.router.util.TravelMinCost;
import org.matsim.router.util.TravelTime;
import org.matsim.utils.misc.Time;

public class FreespeedTravelTime implements TravelMinCost, TravelTime {

	public double getLinkTravelCost(Link link, double time) {
		return (link.getLength() / link.getFreespeed(time));
	}

	public double getLinkTravelTime(Link link, double time) {
		return link.getLength() / link.getFreespeed(time);
	}

	public double getLinkMinimumTravelCost(Link link) {
		return (link.getLength() / link.getFreespeed(Time.UNDEFINED_TIME));
	}

}
