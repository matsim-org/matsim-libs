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

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

final class BatteryDefaultImpl implements Battery {
	private final double capacity;
	private double charge;

	BatteryDefaultImpl( double capacity, double charge ) {
		this.capacity = capacity;
		this.charge = charge;
	}

	@Override
	public double getCapacity() {
		return capacity;
	}

	@Override
	public double getCharge() {
		return charge;
	}

	@Override
	public void setCharge(double charge) {
		Preconditions.checkArgument(charge >= 0 && charge <= capacity, "Charge outside allowed range (SOC=%s)", charge / capacity);
		this.charge = charge;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("capacity", capacity).add("charge", charge).toString();
	}
}
