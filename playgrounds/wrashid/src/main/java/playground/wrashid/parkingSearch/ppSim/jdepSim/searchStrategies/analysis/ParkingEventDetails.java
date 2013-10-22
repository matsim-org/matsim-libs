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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.analysis;

import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.ParkingSearchStrategy;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingActivityAttributes;

public class ParkingEventDetails {

	public ParkingEventDetails(int legIndex, double score, ParkingSearchStrategy parkingStrategy,
			ParkingActivityAttributes parkingActivityAttributes) {
		super();
		this.legIndex = legIndex;
		this.score = score;
		this.parkingStrategy = parkingStrategy;
		this.parkingActivityAttributes = parkingActivityAttributes;
	}
	public int legIndex;
	public double score;
	public ParkingSearchStrategy parkingStrategy;
	public ParkingActivityAttributes parkingActivityAttributes;
}

