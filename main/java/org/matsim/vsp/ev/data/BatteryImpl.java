/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.vsp.ev.data;

import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

public class BatteryImpl implements Battery {
	private static final Logger LOGGER = Logging.getLogger(BatteryImpl.class);

	private final double capacity;
	private final double initialSoc;
	private double soc;

	public BatteryImpl(double capacity, double initialSoc) {
		this.capacity = capacity;
		this.initialSoc = initialSoc;
		this.soc = initialSoc;
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
			LOGGER.warning("Battery SoC is 0");
		}
	}

	@Override
	public void resetSoc() {
		soc = initialSoc;
	}
}
