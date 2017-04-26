/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.RunConfigurableQNetworkFactoryExample;

/**
 * Calculates the maximum speed a vehicle can travel with on a specific link 
 * at a specific time in a specific vehicle. If the speed should be depending
 * on the person driving it, use vehicle.getDriver(). But remember that not
 * every vehicle must have a Person as a driver.
 * <br/><br/>
 * Tutorial examples:<ul>
 * <li> {@link RunConfigurableQNetworkFactoryExample}
 * </ul>
 * 
 * @author mrieser / Senozon AG
 */
public interface LinkSpeedCalculator {

	/**
	 * @param vehicle
	 * @param link
	 * @param time
	 * @return the maximum speed the vehicle can travel on the given link.
	 */
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time);

}
