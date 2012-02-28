/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeDecorator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.pysical;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelTime;

/**
 * @author illenberger
 *
 */
public class TravelTimeDecorator implements TravelTime {

	private final TravelTime delegate;
	
	public TravelTimeDecorator(TravelTime delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time) {
		if(link.getFreespeed() < 10) {
			return delegate.getLinkTravelTime(link, time) * 100.0;
		} else {
			return delegate.getLinkTravelTime(link, time);
		}
	}

}
