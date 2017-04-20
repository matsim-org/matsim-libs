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

package playground.michalm.ev.data;

public class BatteryImpl implements Battery {
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
	public void charge(double energy) {
		if (energy < 0 || energy > capacity - soc) {
			throw new IllegalArgumentException();
		}

		soc += energy;
	}

	@Override
	public void discharge(double energy) {
		if (energy < 0 || energy > soc) {
			throw new IllegalStateException();
		}

		soc -= energy;
	}

	@Override
	public void resetSoc() {
		soc = initialSoc;
	}
}
