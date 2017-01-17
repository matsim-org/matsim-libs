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

import javax.inject.Inject;

import org.matsim.contrib.minibus.PConfigGroup;

/**
 * Calculates the fare for a given {@link StageContainer}.
 * 
 * @author aneumann
 *
 */
public final class TicketMachineDefaultImpl implements TicketMachineI {
	
	private final double earningsPerBoardingPassenger;
	private final double earningsPerMeterAndPassenger;

	@Inject public TicketMachineDefaultImpl(PConfigGroup pConfig ) {
		this.earningsPerBoardingPassenger = pConfig.getEarningsPerBoardingPassenger() ;
		this.earningsPerMeterAndPassenger = pConfig.getEarningsPerKilometerAndPassenger()/1000. ;
	}
	
	@Override
	public double getFare(StageContainer stageContainer) {
		return this.earningsPerBoardingPassenger + this.earningsPerMeterAndPassenger * stageContainer.getDistanceTravelledInMeter();
	}
}
