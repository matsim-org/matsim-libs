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

package org.matsim.contrib.minibus.fare;

/**
 * Calculates the fare for a given {@link StageContainer}.
 * 
 * @author aneumann
 *
 */
public final class TicketMachine {
	
	private final double earningsPerBoardingPassenger;
	private final double earningsPerMeterAndPassenger;

	public TicketMachine(double earningsPerBoardingPassenger, double earningsPerMeterAndPassenger){
		this.earningsPerBoardingPassenger = earningsPerBoardingPassenger;
		this.earningsPerMeterAndPassenger = earningsPerMeterAndPassenger;
	}
	
	public double getFare(StageContainer stageContainer) {
		return this.earningsPerBoardingPassenger + this.earningsPerMeterAndPassenger * stageContainer.getDistanceTravelledInMeter();
	}
}
