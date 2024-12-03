/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.charging;

import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

/**
 * @author michalm
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface ChargingStrategy {
	boolean isChargingCompleted();

	double calcRemainingEnergyToCharge();

	//XXX should include potentially longer charging if AUX remains turned on
	double calcRemainingTimeToCharge();

	static public interface Factory {
		ChargingStrategy createStrategy(ChargerSpecification charger, ElectricVehicle ev);
	}
}
