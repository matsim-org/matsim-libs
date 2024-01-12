/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.parking.parkingchoice.run;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingchoice.PC2.scoring.AbstractParkingBetas;

/**
 * @author jbischoff
 *	example class for setting parking betas: we simply return 1 for Beta values. Typically those values should be person- and/or income depending
 */
class ParkingBetaExample extends AbstractParkingBetas {

	
	@Override
	public double getParkingWalkBeta(Person person,
			double activityDurationInSeconds) {
		return 1;
	}

	@Override
	public double getParkingCostBeta(Person person) {
		return 1;
	}
	
	
	

}
