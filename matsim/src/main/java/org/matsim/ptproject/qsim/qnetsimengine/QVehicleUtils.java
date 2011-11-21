/* *********************************************************************** *
 * project: matsim
 * QVehicleUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim.qnetsimengine;

import org.matsim.ptproject.qsim.interfaces.MobsimVehicle;
import org.matsim.vehicles.Vehicle;

/**
 * @author nagel
 *
 */
public class QVehicleUtils {

	/**
	 * Mobsim factory that returns vehicles of type QVehicle.  Cannot fully hide the implementation since it is needed
	 * both by QSim and by QueueSimulation. kai, nov'11
	 * <p/>
	 * Design thoughts:<ul>
	 * <li> Should return MobsimVehicle instead of QVehicle.  kai, nov'11
	 * </ul>
	 */
	public static MobsimVehicle createMobsimVehicle(Vehicle basicVehicle, double sizeInEquivalents) {
		return new QVehicle(basicVehicle, sizeInEquivalents);
	}

	/**
	 * Mobsim factory that returns vehicles of type QVehicle.  Cannot fully hide the implementation since it is needed
	 * both by QSim and by QueueSimulation. kai, nov'11
	 * <p/>
	 * Design thoughts:<ul>
	 * <li> See above.
	 * </ul>
	 */
	public static MobsimVehicle createMobsimVehicle(Vehicle basicVehicle) {
		return new QVehicle(basicVehicle);
	}

}
