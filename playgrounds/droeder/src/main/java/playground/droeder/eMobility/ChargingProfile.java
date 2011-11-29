/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.eMobility;

import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class ChargingProfile extends AbstractEnergyProfile{

	/**
	 * @param id
	 */
	public ChargingProfile(Id id) {
		super(id);
	}

	@Override
	public double getCharge(double duration, double chargeState) {
		// TODO implement
		return 1;
	}

}
