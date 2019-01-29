/* *********************************************************************** *
 * project: org.matsim.*
 * FreespeedBasedWidthCalculator.java
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

package org.matsim.utils.gis.matsim2esri.network;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class FreespeedBasedWidthCalculator implements WidthCalculator {

	private final Double widthCoefficient;

	public FreespeedBasedWidthCalculator(final Network network, final Double coef) {
		this.widthCoefficient = coef;
	}

	@Override
	public double getWidth(final Link link) {
		return link.getFreespeed() * this.widthCoefficient;
	}


}
