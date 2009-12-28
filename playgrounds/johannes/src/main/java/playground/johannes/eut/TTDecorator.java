/* *********************************************************************** *
 * project: org.matsim.*
 * TTDecorator.java
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

/**
 *
 */
package playground.johannes.eut;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelTime;

/**
 * @author illenberger
 *
 */
public class TTDecorator implements TravelTime {

	private TravelTime meantts;

	private List<Link> accidantLinks = new LinkedList<Link>();

//	public TTDecorator(TravelTime traveltimes) {
//		this.meantts = traveltimes;
//	}

	public void setMeanTravelTimes(TravelTime meantts) {
		this.meantts = meantts;
	}

	public void addAccidantLink(Link link) {
		this.accidantLinks.add(link);
	}

	public void removeAccidantLink(Link link) {
		this.accidantLinks.remove(link);
	}

	public double getLinkTravelTime(Link link, double time) {
		if(this.accidantLinks.contains(link)) {
			return Double.MAX_VALUE;
		} else
			return this.meantts.getLinkTravelTime(link, time);
	}

}
