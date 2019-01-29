/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.util.stats;

import java.util.List;

import org.matsim.contrib.util.CSVReaders;

public class TaxiStatsReader {
	public enum Section {
		PassengerWaitTime, VehicleEmptyDriveRatio, VehicleWaitRatio, TaskTypeTotalDuration;
	}

	private final List<String[]> content;
	private final int hours;

	public TaxiStatsReader(String file) {
		content = CSVReaders.readTSV(file);
		hours = content.size() / Section.values().length - 4;
	}

	public int getHours() {
		return hours;
	}

	public double getMeanWaitTime(int hour) {
		return getValue(Section.PassengerWaitTime, hour, 2);
	}

	public double getP95WaitTime(int hour) {
		return getValue(Section.PassengerWaitTime, hour, 11);
	}

	public double getFleetEmptyDriveRatio(int hour) {
		return getValue(Section.VehicleEmptyDriveRatio, hour, 1);
	}

	public double getFleetWaitRatio(int hour) {
		return getValue(Section.VehicleWaitRatio, hour, 1);
	}

	// hour == 0...hours-1 ==> hourly stats
	// hour == hours ==> daily stats
	public double getValue(Section section, int hour, int col) {
		if (hour < 0 || hour > hours) {
			throw new IllegalArgumentException();
		}

		int row = section.ordinal() * (hours + 4) + hour + 2;

		return Double.valueOf(content.get(row)[col]);
	}
}
