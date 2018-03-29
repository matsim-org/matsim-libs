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

public interface Battery {
	double getCapacity();

	double getSoc();

	void setSoc(double soc);

	void resetSoc();// to the initial/start SOC

	default void charge(double energy) {
		setSoc(Math.min(getSoc() + energy, getCapacity()));
	}

	default void discharge(double energy) {
		setSoc(Math.max(getSoc() - energy, 0));
	}
}
