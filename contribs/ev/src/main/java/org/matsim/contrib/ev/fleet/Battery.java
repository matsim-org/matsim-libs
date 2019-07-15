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

public interface Battery {
	/**
	 * @return Battery Capacity [J]
	 */
	double getCapacity();

	/**
	 * @return Vehicle State of Charge [J]
	 */
	double getSoc();

	/**
	 * @param soc Vehicle State of Charge [J]
	 */
	void setSoc(double soc);

	/**
	 * @param energy Energy to charge[J]
	 */
	default void charge(double energy) {
		setSoc(Math.min(getSoc() + energy, getCapacity()));
	}

	/**
	 * @param energy Energy to discharge [J]
	 */
	default void discharge(double energy) {
		setSoc(Math.max(getSoc() - energy, 0));
	}
}
