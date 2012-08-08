/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.eMob.vehicles.impl;

import org.matsim.contrib.eMob.vehicles.api.BatteryElectricVehicle;
import org.matsim.contrib.eMob.vehicles.api.InductivlyChargable;
import org.matsim.contrib.eMob.vehicles.energyConsumption.EnergyConsumptionModel;

/**
 * Inductively chargeable, battery electric vehicle
 * @author wrashid
 *
 */
public class IC_BEV extends BatteryElectricVehicle implements InductivlyChargable {

	public IC_BEV(EnergyConsumptionModel ecm){
		this.electricDriveEnergyConsumptionModel=ecm;
	}
	
}
