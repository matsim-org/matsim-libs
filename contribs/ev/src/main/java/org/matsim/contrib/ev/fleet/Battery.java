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

import java.util.function.DoubleConsumer;

public interface Battery {
	/**
	 * @return Battery Capacity [J]
	 */
	double getCapacity();

	/**
	 * @return charge [J]
	 */
	double getCharge();

	/**
	 * @param charge charge [J]
	 */
	void setCharge(double charge);

	default double getSoc() {
		return getCharge() / getCapacity();
	}

	// energy [J], positive value reduces the battery charge
	// missingEnergyNotifier -- meant for emitting a MissingEnergyEvent, logging a warning or throwing an exception
	default void dischargeEnergy(double energy, DoubleConsumer missingEnergyNotifier) {
		double oldCharge = getCharge();
		if (oldCharge < energy) {
			missingEnergyNotifier.accept(energy - oldCharge);
			setCharge(0);
		} else {
			setCharge(oldCharge - energy);
		}
	}
}
