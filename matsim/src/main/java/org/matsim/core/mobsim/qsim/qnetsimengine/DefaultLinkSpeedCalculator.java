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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A simple link speed calculator taking the vehicle's max speed and the link's
 * free speed into account;
 *
 * @author mrieser / Senozon AG
 */
public final class DefaultLinkSpeedCalculator implements LinkSpeedCalculator{
	// I have moved this into the qnetsimengine package so that it can be used without injection inside that package.  kai, jun'23
	private static final Logger log = LogManager.getLogger(DefaultLinkSpeedCalculator.class );
	private final Collection<LinkSpeedCalculator> calculators = new ArrayList<>();

	@Inject DefaultLinkSpeedCalculator(){} // so it has to be instantiated by injection from outside package.  kai, jun'23

	@Override public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
		double speed = Double.NaN;
		for ( LinkSpeedCalculator calculator : calculators ) {
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

	/**
	 * This is not meant to be used directly.  But rather through {@link AbstractQSimModule} addLinkSpeedCalculator().  The idea there is that different
	 * link speed calculators can be added orthogonally.  However, if someone still insists on replacing the full link speed calculator, then this
	 * functionality can clearly be used.  kai, jun'23
	 */
	@SuppressWarnings("UnusedReturnValue")
	public final DefaultLinkSpeedCalculator addLinkSpeedCalculator( LinkSpeedCalculator linkSpeedCalculator ){
		this.calculators.add( linkSpeedCalculator );
		return this;
	}

}
