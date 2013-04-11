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
package playground.wrashid.parkingSearch.withinDay_v_STRC.strategies;

import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;

import playground.christoph.parking.withinday.replanner.strategy.ParkingSearchStrategy;

public class GeneralParkingSearchStrategy implements ParkingSearchStrategy {

	private FullParkingSearchStrategy fullParkingSearchStrategy;

	public GeneralParkingSearchStrategy(FullParkingSearchStrategy fullParkingSearchStrategy){
		this.fullParkingSearchStrategy = fullParkingSearchStrategy;
	}
	
	@Override
	public void applySearchStrategy(PlanBasedWithinDayAgent agent, double time) {
		fullParkingSearchStrategy.applySearchStrategy(agent, time);
	}

}

