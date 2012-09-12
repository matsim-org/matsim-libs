/* *********************************************************************** *
 * project: org.matsim.*
 * RideTravelTimeFactory.java
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

package org.matsim.core.mobsim.qsim.multimodalsimengine.router.util;

import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TravelTimeFactory;

public class RideTravelTimeFactory implements TravelTimeFactory {

	private final TravelTime carTravelTimeFactory;	// PT speed does not depend on a passenger, therefore not personalizable
	private final TravelTime walkTravelTimeFactory;
	
	public RideTravelTimeFactory(TravelTime carTravelTimes, TravelTime walkTravelTimeFactory2) {
		this.carTravelTimeFactory = carTravelTimes;
		this.walkTravelTimeFactory = walkTravelTimeFactory2;
	}
	
	@Override
	public TravelTime createTravelTime() {
		return new RideTravelTime(carTravelTimeFactory, walkTravelTimeFactory);
	}
	
}
