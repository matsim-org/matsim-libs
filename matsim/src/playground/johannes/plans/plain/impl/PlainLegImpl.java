/* *********************************************************************** *
 * project: org.matsim.*
 * RawLegImpl.java
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
package playground.johannes.plans.plain.impl;

import playground.johannes.plans.ModCount;
import playground.johannes.plans.plain.PlainLeg;
import playground.johannes.plans.plain.PlainRoute;

/**
 * @author illenberger
 *
 */
public class PlainLegImpl implements PlainLeg, ModCount {

	private PlainRoute route;
	
	public PlainRoute getRoute() {
		return route;
	}

	public void setRoute(PlainRoute route) {
		this.route = route;
	}

	/* (non-Javadoc)
	 * @see playground.johannes.plans.ModCount#getModCount()
	 */
	public long getModCount() {
		// TODO Auto-generated method stub
		return 0;
	}

}
