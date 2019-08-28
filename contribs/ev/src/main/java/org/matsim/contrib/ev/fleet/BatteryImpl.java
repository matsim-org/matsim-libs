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

import com.google.common.base.Preconditions;

public class BatteryImpl implements Battery {
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
		Preconditions.checkArgument(soc >= 0 && soc <= capacity, "SoC outside allowed range: %f", soc);
		this.soc = soc;
	}
}
