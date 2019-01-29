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
	private double unparkDuration = 60;
	
	private static final String PARKDURATION = "parkDuration";
	private double parkDuration = 60;
	
	private static final String AVGPARKINGSLOTLENGTH = "avgParkingSlotLength"; 
	private double avgParkingSlotLength = 10.00;

	private static final String PARKINGSEARCH_STRATEGY = "parkingSearchStrategy";
	private ParkingSearchStrategy parkingSearchStrategy = ParkingSearchStrategy.Random;
	
	public ParkingSearchConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter(UNPARKDURATION)
	public double getUnparkduration() {
		return unparkDuration;
	}

	@StringGetter(PARKDURATION)
	public double getParkduration() {
		return parkDuration;
	}

	@StringGetter(AVGPARKINGSLOTLENGTH)
	public double getAvgparkingslotlength() {
		return avgParkingSlotLength;
	}

	@StringGetter(PARKINGSEARCH_STRATEGY)
	public ParkingSearchStrategy getParkingSearchStrategy() {
		return parkingSearchStrategy;
	}
	
	@StringSetter(UNPARKDURATION)
	public void setUnparkDuration(double unparkDuration) {
		this.unparkDuration = unparkDuration;
	}

	@StringSetter(PARKDURATION)
	public void setParkDuration(double parkDuration) {
		this.parkDuration = parkDuration;
	}

	@StringSetter(AVGPARKINGSLOTLENGTH)
	public void setAvgParkingSlotLength(double avgParkingSlotLength) {
		this.avgParkingSlotLength = avgParkingSlotLength;
	}

	@StringSetter(PARKINGSEARCH_STRATEGY)
	public void setParkingSearchStrategy(ParkingSearchStrategy parkingSearchStrategy) {
		this.parkingSearchStrategy = parkingSearchStrategy;
	}
	
	

}
