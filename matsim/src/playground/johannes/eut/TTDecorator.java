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

import org.matsim.interfaces.basic.v01.BasicLink;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.router.util.TravelTime;

/**
 * @author illenberger
 *
 */
public class TTDecorator implements TravelTime {

	private TravelTime meantts;

	private List<BasicLink> accidantLinks = new LinkedList<BasicLink>();

//	public TTDecorator(TravelTime traveltimes) {
//		this.meantts = traveltimes;
//	}

	public void setMeanTravelTimes(TravelTime meantts) {
		this.meantts = meantts;
	}

	public void addAccidantLink(BasicLink link) {
		this.accidantLinks.add(link);
	}

	public void removeAccidantLink(BasicLink link) {
		this.accidantLinks.remove(link);
	}

	public double getLinkTravelTime(Link link, double time) {
		if(this.accidantLinks.contains(link)) {
			return Double.MAX_VALUE;
		} else
			return this.meantts.getLinkTravelTime(link, time);
	}

}
