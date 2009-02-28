/* *********************************************************************** *
 * project: org.matsim.*
 * LanesBasedWidthCalculator.java
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

import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Network;
import org.matsim.utils.misc.Time;

public class LanesBasedWidthCalculator implements WidthCalculator{

	private final double effectiveLaneWidth;
	private final double widthCoefficient;

	public LanesBasedWidthCalculator(final Network network, final Double coef) {
		this.effectiveLaneWidth = network.getEffectiveLaneWidth();
		this.widthCoefficient = coef;
	}

	public double getWidth(final Link link) {
		return link.getLanes(Time.UNDEFINED_TIME) * this.effectiveLaneWidth * this.widthCoefficient;
	}



}
