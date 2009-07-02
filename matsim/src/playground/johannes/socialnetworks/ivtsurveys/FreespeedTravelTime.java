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

package playground.johannes.socialnetworks.ivtsurveys;

import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

public class FreespeedTravelTime implements TravelMinCost, TravelTime {

	public double getLinkTravelCost(LinkImpl link, double time) {
		return (link.getLength() / link.getFreespeed(time));
	}

	public double getLinkTravelTime(LinkImpl link, double time) {
		return link.getLength() / link.getFreespeed(time);
	}

	public double getLinkMinimumTravelCost(LinkImpl link) {
		return (link.getLength() / link.getFreespeed(Time.UNDEFINED_TIME));
	}

}
