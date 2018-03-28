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

package org.matsim.vsp.ev.charging;

import org.matsim.vsp.ev.data.ElectricVehicle;

/**
 * @author michalm
 */
public interface ChargingStrategy {
	void chargeVehicle(ElectricVehicle ev, double chargePeriod);

	boolean isChargingCompleted(ElectricVehicle ev);

	double calcRemainingEnergyToCharge(ElectricVehicle ev);

	double calcRemainingTimeToCharge(ElectricVehicle ev);
}
