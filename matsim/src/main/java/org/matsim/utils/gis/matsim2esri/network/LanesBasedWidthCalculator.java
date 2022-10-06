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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class LanesBasedWidthCalculator implements WidthCalculator {

	private static final Logger log = LogManager.getLogger(LanesBasedWidthCalculator.class);

	private final double effectiveLaneWidth;
	private final double widthCoefficient;

	/**
	 * This constructor is used by reflection. It's signature mustn't be changed or it won't work anymore. :-(
	 */
	public LanesBasedWidthCalculator(final Network network, final Double coef) {
		double w = network.getEffectiveLaneWidth();
		if (Double.isNaN(w)) {
			log.warn("Effective lane width in network is set to Double.NaN. Set a real value in your network.xml to make this tool work with this value. Using 3.75 as effective lane width...");
			this.effectiveLaneWidth = 3.75;
		}
		else {
			this.effectiveLaneWidth = w;
		}
		this.widthCoefficient = coef;
	}

	@Override
	public double getWidth(final Link link) {
		return link.getNumberOfLanes() * this.effectiveLaneWidth * this.widthCoefficient;
	}

}
