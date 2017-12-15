/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingsearch.sim;

import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;
import org.matsim.core.config.ReflectiveConfigGroup;

public class ParkingSearchConfigGroup extends ReflectiveConfigGroup {
	
	public static final String GROUP_NAME = "parkingSearch";
	
	public static final String UNPARKDURATION = "unparkDuration";
	private static double unparkDuration = 60;
	
	private static final String PARKDURATION = "parkDuration";
	private static double parkDuration = 60;
	
	private static final String AVGPARKINGSLOTLENGTH = "avgParkingSlotLength"; 
	private static double avgParkingSlotLength = 10.00;

	private static final String PARKINGSEARCH_STRATEGY = "parkingSearchStrategy";
	private static ParkingSearchStrategy parkingSearchStrategy = ParkingSearchStrategy.Random;
	
	public ParkingSearchConfigGroup(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	public ParkingSearchConfigGroup(String name, boolean storeUnknownParametersAsStrings) {
		super(name, storeUnknownParametersAsStrings);
		// TODO Auto-generated constructor stub
	}

	@StringGetter(UNPARKDURATION)
	public static double getUnparkduration() {
		return unparkDuration;
	}

	@StringGetter(PARKDURATION)
	public static double getParkduration() {
		return parkDuration;
	}

	@StringGetter(AVGPARKINGSLOTLENGTH)
	public static double getAvgparkingslotlength() {
		return avgParkingSlotLength;
	}

	@StringGetter(PARKINGSEARCH_STRATEGY)
	public static ParkingSearchStrategy getParkingSearchStrategy() {
		return parkingSearchStrategy;
	}
	
	@StringSetter(UNPARKDURATION)
	public static void setUnparkDuration(double unparkDuration) {
		ParkingSearchConfigGroup.unparkDuration = unparkDuration;
	}

	@StringSetter(PARKDURATION)
	public static void setParkDuration(double parkDuration) {
		ParkingSearchConfigGroup.parkDuration = parkDuration;
	}

	@StringSetter(AVGPARKINGSLOTLENGTH)
	public static void setAvgParkingSlotLength(double avgParkingSlotLength) {
		ParkingSearchConfigGroup.avgParkingSlotLength = avgParkingSlotLength;
	}

	@StringSetter(PARKINGSEARCH_STRATEGY)
	public static void setParkingSearchStrategy(ParkingSearchStrategy parkingSearchStrategy) {
		ParkingSearchConfigGroup.parkingSearchStrategy = parkingSearchStrategy;
	}
	
	

}
