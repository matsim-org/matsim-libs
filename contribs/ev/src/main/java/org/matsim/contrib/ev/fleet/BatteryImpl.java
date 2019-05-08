/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.ev.fleet;

import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

public class BatteryImpl implements Battery {
	private static final Logger LOGGER = Logging.getLogger(BatteryImpl.class);

	private final double capacity;
	private double soc;

	public BatteryImpl(double capacity, double soc) {
		this.capacity = capacity;
		this.soc = soc;
	}

	@Override
	public double getCapacity() {
		return capacity;
	}

	@Override
	public double getSoc() {
		return soc;
	}

	@Override
	public void setSoc(double soc) {
		if (soc < 0 || soc > capacity) {
			throw new IllegalArgumentException("SoC=" + soc);
		}
		this.soc = soc;
		if (soc == 0) {
			//			LOGGER.warning("Battery SoC is 0");
		}
	}
}
