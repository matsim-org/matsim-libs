/* *********************************************************************** *
 * project: org.matsim.*
 * CapacityBasedWidthCalculator.java
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

package playground.gregor.matsim2GIS.network;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

public class CapacityBasedWidthCalculator implements WidthCalculator {

	private final double widthCoefficient;

	public CapacityBasedWidthCalculator(final NetworkLayer network, final Double coef) {
		this.widthCoefficient = coef;
	}

	public double getWidth(final Link link) {
		return link.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME) * this.widthCoefficient;
	}
}
