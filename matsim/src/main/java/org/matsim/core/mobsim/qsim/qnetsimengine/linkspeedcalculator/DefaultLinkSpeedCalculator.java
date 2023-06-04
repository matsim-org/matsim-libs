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

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;
import org.matsim.vehicles.Vehicle;

import java.util.Collection;

/**
 * A simple link speed calculator taking the vehicle's max speed and the link's
 * free speed into account;
 * 
 * @author mrieser / Senozon AG
 */
public final class DefaultLinkSpeedCalculator implements LinkSpeedCalculator {
	@Inject private Collection<VehicleSpeedCalculator> calculators;

	@Override
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
		double speed = Double.NaN;
		for ( VehicleSpeedCalculator calculator : calculators ) {
			double tmp = calculator.getMaximumVelocity( vehicle, link, time ) ;
			if ( !Double.isNaN( tmp ) ) {
				if ( Double.isNaN( speed ) ){
					speed = tmp;
				} else {
					throw new RuntimeException( "two vehicle speed calculators feel responsible for vehicle; don't know what to do." );
					// would be possible to have the calculators sorted, i.e. as List, but don't see how this could be configured in an easy way.  kai, jun'23
				}
			}
		}
		if ( !Double.isNaN( speed ) ) {
			return speed;
		} else{
			return Math.min( vehicle.getMaximumVelocity(), link.getFreespeed( time ) );
		}
	}

	interface VehicleSpeedCalculator extends LinkSpeedCalculator {

	}
	
}
