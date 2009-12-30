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

import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;

public class FreespeedBasedWidthCalculator implements WidthCalculator {

	private final Double widthCoefficient;

	public FreespeedBasedWidthCalculator(final NetworkLayer network, final Double coef) {
		this.widthCoefficient = coef;
	}

	public double getWidth(final LinkImpl link) {
		return link.getFreespeed(Time.UNDEFINED_TIME) * this.widthCoefficient;
	}



}
